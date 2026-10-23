package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AuditLogEntity
import com.example.data.CourierConfigEntity
import com.example.data.OrderEntity
import com.example.data.OrderItemEntity
import com.example.util.CoreUtils
import com.example.util.CourierApiClient
import com.example.util.SmartPasteParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FashionAuraViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val orderDao = db.orderDao()
    private val courierApiClient = CourierApiClient(application)

    val activeOrders: StateFlow<List<OrderEntity>> = orderDao.getActiveOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedOrders: StateFlow<List<OrderEntity>> = orderDao.getTrashedOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrderItems: StateFlow<List<OrderItemEntity>> = orderDao.getAllOrderItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courierConfigs: StateFlow<List<CourierConfigEntity>> = orderDao.getCourierConfigsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Language & Digits
    private val _lang = MutableStateFlow("bn")
    val lang: StateFlow<String> = _lang.asStateFlow()

    private val _useBnDigits = MutableStateFlow(true)
    val useBnDigits: StateFlow<Boolean> = _useBnDigits.asStateFlow()

    // Navigation Tab
    private val _activeTab = MutableStateFlow("dashboard") // dashboard, orders, create, smartPaste, settings, backup
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Search & Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow("All")
    val filterType: StateFlow<String> = _filterType.asStateFlow()

    private val _filterPriority = MutableStateFlow("All")
    val filterPriority: StateFlow<String> = _filterPriority.asStateFlow()

    private val _filterPayment = MutableStateFlow("All")
    val filterPayment: StateFlow<String> = _filterPayment.asStateFlow()

    private val _filterFulfillment = MutableStateFlow("All")
    val filterFulfillment: StateFlow<String> = _filterFulfillment.asStateFlow()

    private val _filterSchedule = MutableStateFlow("All")
    val filterSchedule: StateFlow<String> = _filterSchedule.asStateFlow()

    // Order Form States
    val editingOrderId = MutableStateFlow<String?>(null)
    val customerName = MutableStateFlow("")
    val primaryPhone = MutableStateFlow("")
    val fullAddress = MutableStateFlow("")
    val district = MutableStateFlow("Dhaka")
    val thanaUpazila = MutableStateFlow("Dhanmondi")
    val orderType = MutableStateFlow("Regular")
    val priority = MutableStateFlow("Normal")
    val fulfillmentStatus = MutableStateFlow("New")
    val courier = MutableStateFlow("Steadfast")
    val requestedDeliveryDate = MutableStateFlow("")
    val earliestDispatchDate = MutableStateFlow("")
    val deliveryCharge = MutableStateFlow(80)
    val discount = MutableStateFlow(0)
    val advancePaid = MutableStateFlow(0)
    val customerInstructions = MutableStateFlow("")
    val internalNotes = MutableStateFlow("")
    val productName = MutableStateFlow("")
    val sku = MutableStateFlow("")
    val size = MutableStateFlow("40")
    val quantity = MutableStateFlow(1)
    val unitPrice = MutableStateFlow(1250)

    // Smart Paste
    val rawPasteText = MutableStateFlow("")

    // Syncing state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Toast
    private val _toastMsg = MutableStateFlow<String?>(null)
    val toastMsg: StateFlow<String?> = _toastMsg.asStateFlow()

    init {
        // Ensure configs are populated
        viewModelScope.launch {
            if (orderDao.getCourierConfigCount() == 0) {
                val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                orderDao.insertDefaultCourierConfigs(
                    listOf(
                        CourierConfigEntity(
                            courier = "Steadfast",
                            enabled = false,
                            baseUrl = "https://portal.packzy.com/api/v1",
                            apiKey = "",
                            secretKeyOrToken = "",
                            storeId = null,
                            updatedAt = now
                        ),
                        CourierConfigEntity(
                            courier = "Pathao",
                            enabled = false,
                            baseUrl = "https://api-hermes.pathao.com",
                            apiKey = "",
                            secretKeyOrToken = "",
                            storeId = "",
                            updatedAt = now
                        )
                    )
                )
            }
        }
    }

    fun toggleLang() {
        _lang.value = if (_lang.value == "bn") "en" else "bn"
    }

    fun toggleBnDigits() {
        _useBnDigits.value = !_useBnDigits.value
    }

    fun setActiveTab(tab: String) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(type: String) { _filterType.value = type }
    fun setFilterPriority(p: String) { _filterPriority.value = p }
    fun setFilterPayment(p: String) { _filterPayment.value = p }
    fun setFilterFulfillment(f: String) { _filterFulfillment.value = f }
    fun setFilterSchedule(s: String) { _filterSchedule.value = s }

    fun showToast(msg: String) {
        _toastMsg.value = msg
        viewModelScope.launch {
            kotlinx.coroutines.delay(3500)
            if (_toastMsg.value == msg) {
                _toastMsg.value = null
            }
        }
    }

    fun resetForm() {
        editingOrderId.value = null
        customerName.value = ""
        primaryPhone.value = ""
        fullAddress.value = ""
        productName.value = ""
        sku.value = ""
        size.value = "40"
        quantity.value = 1
        unitPrice.value = 1250
        advancePaid.value = 0
        discount.value = 0
        deliveryCharge.value = 80
        requestedDeliveryDate.value = ""
        earliestDispatchDate.value = ""
        customerInstructions.value = ""
        internalNotes.value = ""
        orderType.value = "Regular"
        priority.value = "Normal"
        fulfillmentStatus.value = "New"
        courier.value = "Steadfast"
        district.value = "Dhaka"
        thanaUpazila.value = "Dhanmondi"
    }

    fun populateEditOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        editingOrderId.value = order.id
        customerName.value = order.customerName
        primaryPhone.value = order.primaryPhone
        fullAddress.value = order.fullAddress
        district.value = order.district
        thanaUpazila.value = order.thanaUpazila
        orderType.value = order.orderType
        priority.value = order.priority
        fulfillmentStatus.value = order.fulfillmentStatus
        courier.value = order.courier
        requestedDeliveryDate.value = order.requestedDeliveryDate ?: ""
        earliestDispatchDate.value = order.earliestDispatchDate ?: ""
        deliveryCharge.value = order.deliveryCharge
        discount.value = order.discount
        advancePaid.value = order.netPaid
        customerInstructions.value = order.customerDeliveryInstructions ?: ""
        internalNotes.value = order.internalStaffNotes ?: ""

        val firstItem = items.firstOrNull()
        if (firstItem != null) {
            productName.value = firstItem.productName
            sku.value = firstItem.sku ?: ""
            size.value = firstItem.size
            quantity.value = firstItem.quantity
            unitPrice.value = firstItem.unitPrice
        }
        setActiveTab("create")
    }

    fun handleSmartFill() {
        val text = rawPasteText.value
        if (text.isBlank()) return
        val parsed = SmartPasteParser.parseOrderMessageOffline(text)

        customerName.value = parsed.customerName.value
        primaryPhone.value = parsed.primaryPhone.value
        fullAddress.value = parsed.fullAddress.value
        district.value = parsed.district.value
        productName.value = if (parsed.productName.value.isNotBlank()) parsed.productName.value else "Three Piece"
        size.value = parsed.size.value
        unitPrice.value = if (parsed.unitPrice.value > 0) parsed.unitPrice.value else 1250
        deliveryCharge.value = parsed.deliveryCharge.value
        advancePaid.value = parsed.advancePaid.value
        customerInstructions.value = parsed.deliveryInstructions.value

        showToast(
            if (_lang.value == "bn")
                "স্মার্ট ফিল সম্পন্ন! অনুগ্রহ করে যাচাই করে সেভ করুন।"
            else
                "Smart Fill extracted! Review fields before saving."
        )
        setActiveTab("create")
    }

    fun saveOrder() {
        val name = customerName.value.trim()
        val phone = primaryPhone.value.trim()
        val address = fullAddress.value.trim()
        val prod = productName.value.trim()

        if (name.isEmpty() || !CoreUtils.isValidBDPhone(phone) || address.isEmpty() || prod.isEmpty()) {
            showToast(
                if (_lang.value == "bn")
                    "ক্রেতার নাম, ১১ সংখ্যার মোবাইল নম্বর, ঠিকানা ও পণ্যের নাম আবশ্যক!"
                else
                    "Name, valid 11-digit BD phone, address, and product name are required!"
            )
            return
        }

        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val id = editingOrderId.value ?: UUID.randomUUID().toString()
            val existing = if (editingOrderId.value != null) orderDao.getOrderById(id) else null
            val orderNum = existing?.orderNumber ?: CoreUtils.generateOrderId()

            val financials = CoreUtils.calculateFinancials(
                orderType = orderType.value,
                items = listOf(Pair(unitPrice.value, quantity.value)),
                deliveryCharge = deliveryCharge.value,
                additionalCharge = 0,
                discount = discount.value,
                netPaid = advancePaid.value
            )

            val orderRecord = OrderEntity(
                id = id,
                orderNumber = orderNum,
                customerId = CoreUtils.normalizeBDPhone(phone),
                customerName = name,
                primaryPhone = CoreUtils.normalizeBDPhone(phone),
                fullAddress = address,
                district = district.value,
                thanaUpazila = thanaUpazila.value,
                orderType = orderType.value,
                priority = priority.value,
                paymentStatus = financials.paymentStatus,
                fulfillmentStatus = fulfillmentStatus.value,
                orderDate = existing?.orderDate ?: now,
                requestedDeliveryDate = requestedDeliveryDate.value.ifBlank { null },
                earliestDispatchDate = earliestDispatchDate.value.ifBlank { null },
                source = "WhatsApp",
                courier = courier.value,
                trackingNumber = existing?.trackingNumber,
                courierSyncStatus = existing?.courierSyncStatus ?: "not_sent",
                courierConsignmentId = existing?.courierConsignmentId,
                courierLastError = existing?.courierLastError,
                courierSentAt = existing?.courierSentAt,
                subtotal = financials.subtotal,
                deliveryCharge = deliveryCharge.value,
                additionalCharge = 0,
                discount = discount.value,
                totalBill = financials.totalBill,
                netPaid = advancePaid.value,
                outstanding = financials.outstanding,
                customerDeliveryInstructions = customerInstructions.value.trim().ifBlank { null },
                internalStaffNotes = internalNotes.value.trim().ifBlank { null },
                isDraft = false,
                isDeleted = false,
                version = 1,
                createdAt = existing?.createdAt ?: now,
                createdBy = "Admin",
                updatedAt = now,
                updatedBy = "Admin",
                syncState = "pending"
            )

            val itemRecord = OrderItemEntity(
                id = UUID.randomUUID().toString(),
                orderId = id,
                productName = prod,
                sku = sku.value.trim().ifBlank { null },
                size = size.value,
                quantity = quantity.value,
                unitPrice = unitPrice.value
            )

            val log = AuditLogEntity(
                id = UUID.randomUUID().toString(),
                entityType = "order",
                entityId = id,
                action = if (editingOrderId.value != null) "UPDATED" else "CREATED",
                details = "$orderNum - BDT ${financials.totalBill}",
                performedBy = "Admin",
                performedAt = now
            )

            orderDao.saveOrderWithItem(orderRecord, itemRecord, log)

            resetForm()
            showToast(
                if (_lang.value == "bn") "অর্ডার সফলভাবে সংরক্ষিত হয়েছে!" else "Order saved to local database!"
            )
            setActiveTab("orders")
        }
    }

    fun sendOrderToCourier(orderId: String, allowEarlyOverride: Boolean = false) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val res = courierApiClient.sendOrderToCourier(orderId, allowEarlyOverride)
                showToast(res.message)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun bulkSyncOrders() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val (sent, failed, queued) = courierApiClient.bulkSyncOrders()
                showToast("Courier Sync: $sent sent, $failed failed, $queued queued offline.")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun saveCourierConfig(config: CourierConfigEntity) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            orderDao.saveCourierConfig(config.copy(updatedAt = now))
            showToast("${config.courier} API settings saved locally!")
        }
    }

    fun softDelete(orderId: String) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            orderDao.softDeleteOrder(orderId, now)
            showToast(if (_lang.value == "bn") "অর্ডার ট্র্যাশে সরানো হয়েছে।" else "Moved to Trash.")
        }
    }

    fun restore(orderId: String) {
        viewModelScope.launch {
            val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            orderDao.restoreOrder(orderId, now)
            showToast(if (_lang.value == "bn") "অর্ডার পুনরুদ্ধার হয়েছে!" else "Order restored!")
        }
    }

    suspend fun getAllDataForBackup(): Triple<List<OrderEntity>, List<OrderItemEntity>, List<AuditLogEntity>> {
        val orders = orderDao.getAllOrdersSync()
        val items = orderDao.getAllOrderItemsSync()
        val logs = orderDao.getAllAuditLogsSync()
        return Triple(orders, items, logs)
    }
}
