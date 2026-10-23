package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.AuditLogEntity
import com.example.data.OrderEntity
import com.example.data.OrderItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportService {
    fun generateOrdersCSV(orders: List<OrderEntity>): String {
        val headers = listOf(
            "Order ID",
            "Order Date",
            "Customer Name",
            "Phone",
            "Address",
            "District",
            "Thana",
            "Order Type",
            "Priority",
            "Payment Status",
            "Fulfillment Status",
            "Schedule State",
            "Earliest Dispatch",
            "Requested Delivery",
            "Total Bill",
            "Net Paid",
            "COD Outstanding",
            "Courier"
        )

        fun escapeCsv(v: Any?): String {
            val str = v?.toString() ?: ""
            return "\"${str.replace("\"", "\"\"")}\""
        }

        val rows = orders.map { o ->
            val schedule = CoreUtils.getDeliveryScheduleState(o.earliestDispatchDate, o.requestedDeliveryDate)
            val dateStr = if (o.orderDate.length >= 10) o.orderDate.substring(0, 10) else o.orderDate
            listOf(
                o.orderNumber,
                dateStr,
                o.customerName,
                o.primaryPhone,
                o.fullAddress,
                o.district,
                o.thanaUpazila,
                o.orderType,
                o.priority,
                o.paymentStatus,
                o.fulfillmentStatus,
                schedule,
                o.earliestDispatchDate ?: "",
                o.requestedDeliveryDate ?: "",
                o.totalBill,
                o.netPaid,
                maxOf(0, o.outstanding),
                o.courier
            ).joinToString(",") { escapeCsv(it) }
        }

        // UTF-8 BOM (\uFEFF)
        return "\uFEFF" + (listOf(headers.joinToString(",")) + rows).joinToString("\n")
    }

    fun generateFullBackupJSON(
        orders: List<OrderEntity>,
        items: List<OrderItemEntity>,
        logs: List<AuditLogEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "Fashion Aura OMS")
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))

        val data = JSONObject()
        val ordersArray = JSONArray()
        for (o in orders) {
            val obj = JSONObject()
            obj.put("id", o.id)
            obj.put("orderNumber", o.orderNumber)
            obj.put("customerName", o.customerName)
            obj.put("primaryPhone", o.primaryPhone)
            obj.put("fullAddress", o.fullAddress)
            obj.put("district", o.district)
            obj.put("thanaUpazila", o.thanaUpazila)
            obj.put("orderType", o.orderType)
            obj.put("priority", o.priority)
            obj.put("paymentStatus", o.paymentStatus)
            obj.put("fulfillmentStatus", o.fulfillmentStatus)
            obj.put("totalBill", o.totalBill)
            obj.put("netPaid", o.netPaid)
            obj.put("outstanding", o.outstanding)
            obj.put("deliveryCharge", o.deliveryCharge)
            obj.put("discount", o.discount)
            obj.put("courier", o.courier)
            obj.put("isDeleted", o.isDeleted)
            obj.put("createdAt", o.createdAt)
            ordersArray.put(obj)
        }
        data.put("orders", ordersArray)

        val itemsArray = JSONArray()
        for (it in items) {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("orderId", it.orderId)
            obj.put("productName", it.productName)
            obj.put("sku", it.sku ?: "")
            obj.put("size", it.size)
            obj.put("quantity", it.quantity)
            obj.put("unitPrice", it.unitPrice)
            itemsArray.put(obj)
        }
        data.put("orderItems", itemsArray)

        val logsArray = JSONArray()
        for (l in logs) {
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("entityType", l.entityType)
            obj.put("entityId", l.entityId)
            obj.put("action", l.action)
            obj.put("details", l.details)
            obj.put("performedAt", l.performedAt)
            logsArray.put(obj)
        }
        data.put("auditLogs", logsArray)

        root.put("data", data)
        return root.toString(2)
    }

    fun shareTextFile(context: Context, filename: String, content: String, mimeType: String) {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, filename)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share $filename"))
    }
}
