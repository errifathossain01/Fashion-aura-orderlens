package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.AppDatabase
import com.example.data.AuditLogEntity
import com.example.data.OrderEntity
import com.example.data.OrderItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CourierPushResult(
    val orderId: String,
    val orderNumber: String,
    val status: String, // sent, queued_offline, failed
    val trackingCode: String? = null,
    val consignmentId: String? = null,
    val message: String
)

class CourierApiClient(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val db = AppDatabase.getDatabase(context)
    private val orderDao = db.orderDao()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun sendOrderToCourier(
        orderId: String,
        allowEarlyDispatchOverride: Boolean = false
    ): CourierPushResult = withContext(Dispatchers.IO) {
        val order = orderDao.getOrderById(orderId) ?: return@withContext CourierPushResult(
            orderId = orderId,
            orderNumber = "",
            status = "failed",
            message = "Order not found"
        )

        if (order.courierSyncStatus == "sent" && !order.trackingNumber.isNullOrBlank()) {
            return@withContext CourierPushResult(
                orderId = order.id,
                orderNumber = order.orderNumber,
                status = "sent",
                trackingCode = order.trackingNumber,
                consignmentId = order.courierConsignmentId,
                message = "Already sent (Tracking: ${order.trackingNumber})"
            )
        }

        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (!order.earliestDispatchDate.isNullOrBlank() &&
            todayIso < order.earliestDispatchDate &&
            !allowEarlyDispatchOverride
        ) {
            return@withContext CourierPushResult(
                orderId = order.id,
                orderNumber = order.orderNumber,
                status = "failed",
                message = "Restricted: Do Not Dispatch Before ${order.earliestDispatchDate}"
            )
        }

        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        if (!isOnline()) {
            orderDao.updateCourierFailure(
                orderId = order.id,
                status = "queued_offline",
                errorMsg = "Queued offline — waiting for internet",
                updatedAt = now
            )
            return@withContext CourierPushResult(
                orderId = order.id,
                orderNumber = order.orderNumber,
                status = "queued_offline",
                message = "Phone is offline. Added to Pending Courier Sync queue."
            )
        }

        val config = orderDao.getCourierConfig(order.courier)
        if (config == null || !config.enabled) {
            val msg = "${order.courier} API is not enabled in Settings."
            orderDao.updateCourierFailure(
                orderId = order.id,
                status = "failed",
                errorMsg = msg,
                updatedAt = now
            )
            return@withContext CourierPushResult(
                orderId = order.id,
                orderNumber = order.orderNumber,
                status = "failed",
                message = msg
            )
        }

        orderDao.updateCourierFailure(
            orderId = order.id,
            status = "sending",
            errorMsg = "",
            updatedAt = now
        )

        val items = orderDao.getItemsForOrder(order.id)

        try {
            val result = when (order.courier) {
                "Steadfast" -> pushToSteadfast(order, items, config.baseUrl, config.apiKey, config.secretKeyOrToken)
                "Pathao" -> pushToPathao(order, items, config.baseUrl, config.secretKeyOrToken, config.storeId ?: "")
                else -> throw IllegalStateException("Direct API for ${order.courier} not supported.")
            }

            orderDao.updateCourierSuccess(
                orderId = order.id,
                status = "sent",
                trackingNumber = result.first,
                consignmentId = result.second,
                sentAt = now
            )

            orderDao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    entityType = "courier_api",
                    entityId = order.id,
                    action = "SENT_${order.courier.uppercase()}",
                    details = "Tracking: ${result.first}",
                    performedBy = "Admin",
                    performedAt = now
                )
            )

            CourierPushResult(
                orderId = order.id,
                orderNumber = order.orderNumber,
                status = "sent",
                trackingCode = result.first,
                consignmentId = result.second,
                message = "Sent to ${order.courier}! Tracking: ${result.first}"
            )
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Courier API Error"
            orderDao.updateCourierFailure(
                orderId = order.id,
                status = "failed",
                errorMsg = errorMsg,
                updatedAt = now
            )
            CourierPushResult(
                orderId = order.id,
                orderNumber = order.orderNumber,
                status = "failed",
                message = errorMsg
            )
        }
    }

    suspend fun bulkSyncOrders(orderIds: List<String>? = null): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val allOrders = orderDao.getAllOrdersSync()
        val targetOrders = if (!orderIds.isNullOrEmpty()) {
            allOrders.filter { orderIds.contains(it.id) && !it.isDeleted }
        } else {
            allOrders.filter { !it.isDeleted && (it.courierSyncStatus == "queued_offline" || it.courierSyncStatus == "failed") }
        }

        var sent = 0
        var failed = 0
        var queued = 0

        for (order in targetOrders) {
            val res = sendOrderToCourier(order.id)
            when (res.status) {
                "sent" -> sent++
                "queued_offline" -> queued++
                else -> failed++
            }
        }

        Triple(sent, failed, queued)
    }

    private fun pushToSteadfast(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        baseUrl: String,
        apiKey: String,
        secretKey: String
    ): Pair<String, String> {
        if (apiKey.isBlank() || secretKey.isBlank()) {
            throw IllegalArgumentException("Steadfast API Key and Secret Key required.")
        }

        val itemSummary = items.joinToString(", ") { "${it.productName} (Size ${it.size} x${it.quantity})" }
        val returnNote = "রিটার্ন করলে, ডেলিভারি চার্জ ${order.deliveryCharge} টাকা নিয়ে নিবেন।"
        val finalNote = "$itemSummary | $returnNote ${order.customerDeliveryInstructions ?: ""}".trim()

        val json = JSONObject().apply {
            put("invoice", order.orderNumber)
            put("recipient_name", order.customerName)
            put("recipient_phone", order.primaryPhone)
            put("recipient_address", "${order.fullAddress}, ${order.thanaUpazila}, ${order.district}")
            put("cod_amount", maxOf(0, order.outstanding))
            put("note", finalNote)
        }

        val url = baseUrl.trimEnd('/') + "/create_order"
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Api-Key", apiKey.trim())
            .addHeader("Secret-Key", secretKey.trim())
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errObj = runCatching { JSONObject(respBody) }.getOrNull()
                val errMsg = errObj?.optString("message") ?: "HTTP ${response.code}"
                throw RuntimeException(errMsg)
            }
            val data = JSONObject(respBody)
            val consignment = data.optJSONObject("consignment")
            val trackingCode = consignment?.optString("tracking_code")?.ifBlank { null }
                ?: consignment?.optString("consignment_id")?.ifBlank { null }
                ?: throw RuntimeException("Steadfast response missing tracking_code")
            val consignmentId = consignment?.optString("consignment_id") ?: trackingCode
            return Pair(trackingCode, consignmentId)
        }
    }

    private fun pushToPathao(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        baseUrl: String,
        bearerToken: String,
        storeId: String
    ): Pair<String, String> {
        if (bearerToken.isBlank() || storeId.isBlank()) {
            throw IllegalArgumentException("Pathao Token and Store ID required.")
        }

        val totalQty = items.sumOf { maxOf(1, it.quantity) }
        val itemDesc = items.joinToString(", ") { "${it.productName} (Size ${it.size})" }
        val returnNote = "রিটার্ন করলে, ডেলিভারি চার্জ ${order.deliveryCharge} টাকা নিয়ে নিবেন।"
        val finalNote = "$itemDesc | $returnNote ${order.customerDeliveryInstructions ?: ""}".trim()

        val json = JSONObject().apply {
            put("store_id", storeId.trim().toIntOrNull() ?: 0)
            put("merchant_order_id", order.orderNumber)
            put("recipient_name", order.customerName)
            put("recipient_phone", order.primaryPhone)
            put("recipient_address", "${order.fullAddress}, ${order.thanaUpazila}, ${order.district}")
            put("delivery_type", 48)
            put("item_type", 2)
            put("item_quantity", if (totalQty > 0) totalQty else 1)
            put("item_weight", 0.5)
            put("amount_to_collect", maxOf(0, order.outstanding))
            put("special_instruction", finalNote)
            put("item_description", itemDesc)
        }

        val url = baseUrl.trimEnd('/') + "/aladdin/api/v1/orders"
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${bearerToken.trim()}")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val respBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errObj = runCatching { JSONObject(respBody) }.getOrNull()
                val errMsg = errObj?.optString("message") ?: "HTTP ${response.code}"
                throw RuntimeException(errMsg)
            }
            val data = JSONObject(respBody)
            val dataObj = data.optJSONObject("data")
            val consignmentId = dataObj?.optString("consignment_id")?.ifBlank { null }
                ?: throw RuntimeException("Pathao response missing consignment_id")
            return Pair(consignmentId, consignmentId)
        }
    }
}
