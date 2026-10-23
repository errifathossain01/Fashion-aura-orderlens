package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CourierConfigEntity
import com.example.data.OrderEntity
import com.example.data.OrderItemEntity
import com.example.ui.FashionAuraViewModel
import com.example.util.CoreUtils
import com.example.util.CourierTemplateEngine
import com.example.util.ExportService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val DeepPink = Color(0xFF831843)
val DarkPink = Color(0xFF500724)
val LightPinkBg = Color(0xFFFDF2F8)
val AccentPurple = Color(0xFF6B21A8)
val SlateBg = Color(0xFFF8FAFC)
val EmeraldGreen = Color(0xFF065F46)
val SoftRed = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FashionAuraApp(viewModel: FashionAuraViewModel) {
    val lang by viewModel.lang.collectAsState()
    val useBnDigits by viewModel.useBnDigits.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val toastMsg by viewModel.toastMsg.collectAsState()
    val activeOrders by viewModel.activeOrders.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(toastMsg) {
        toastMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("fashion_aura_main_scaffold"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (lang == "bn") "ফ্যাশন আউরা — অর্ডার ম্যানেজমেন্ট" else "Fashion Aura — Order Management",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (lang == "bn") "ডাইরেক্ট কুরিয়ার এপিআই (Steadfast & Pathao)" else "Direct Courier API (Steadfast & Pathao)",
                            fontSize = 11.sp,
                            color = Color(0xFFFBCFE8)
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.toggleBnDigits() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF9D174D),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 4.dp).testTag("btn_toggle_digits")
                    ) {
                        Text(
                            text = if (useBnDigits) "১২৩" else "123",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.toggleLang() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = DeepPink
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("btn_toggle_lang")
                    ) {
                        Text(
                            text = if (lang == "bn") "English" else "বাংলা",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepPink
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "dashboard",
                    onClick = { viewModel.setActiveTab("dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text(if (lang == "bn") "ড্যাশবোর্ড" else "Dashboard", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = activeTab == "orders",
                    onClick = { viewModel.setActiveTab("orders") },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeOrders.isNotEmpty()) {
                                    Badge {
                                        Text(CoreUtils.formatDigits(activeOrders.size, useBnDigits))
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = "Orders")
                        }
                    },
                    label = { Text(if (lang == "bn") "অর্ডার" else "Orders", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_orders")
                )
                NavigationBarItem(
                    selected = activeTab == "create",
                    onClick = {
                        viewModel.resetForm()
                        viewModel.setActiveTab("create")
                    },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Add") },
                    label = { Text(if (lang == "bn") "+ নতুন" else "+ Add", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_create")
                )
                NavigationBarItem(
                    selected = activeTab == "smartPaste",
                    onClick = { viewModel.setActiveTab("smartPaste") },
                    icon = { Icon(Icons.Default.ContentPaste, contentDescription = "Smart Paste") },
                    label = { Text(if (lang == "bn") "স্মার্ট পেস্ট" else "Paste", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_smart_paste")
                )
                NavigationBarItem(
                    selected = activeTab == "settings",
                    onClick = { viewModel.setActiveTab("settings") },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Courier API") },
                    label = { Text(if (lang == "bn") "কুরিয়ার API" else "Courier", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_courier_settings")
                )
                NavigationBarItem(
                    selected = activeTab == "backup",
                    onClick = { viewModel.setActiveTab("backup") },
                    icon = { Icon(Icons.Default.Storage, contentDescription = "Backup") },
                    label = { Text(if (lang == "bn") "ব্যাকআপ" else "Backup", fontSize = 10.sp) },
                    modifier = Modifier.testTag("tab_backup")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateBg)
        ) {
            when (activeTab) {
                "dashboard" -> DashboardContent(viewModel)
                "orders" -> OrdersListContent(viewModel)
                "create" -> CreateOrderContent(viewModel)
                "smartPaste" -> SmartPasteContent(viewModel)
                "settings" -> CourierSettingsContent(viewModel)
                "backup" -> BackupTrashContent(viewModel)
            }
        }
    }
}

@Composable
fun DashboardContent(viewModel: FashionAuraViewModel) {
    val lang by viewModel.lang.collectAsState()
    val useBnDigits by viewModel.useBnDigits.collectAsState()
    val activeOrders by viewModel.activeOrders.collectAsState()

    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val totalCount = activeOrders.size
    val newToday = activeOrders.count { it.orderDate.startsWith(todayIso) }
    val awaitingShip = activeOrders.count {
        listOf("New", "Confirmed", "Preparing", "Ready to Ship").contains(it.fulfillmentStatus)
    }
    val urgentCount = activeOrders.count { it.priority == "Urgent" }
    val scheduledCount = activeOrders.count {
        CoreUtils.getDeliveryScheduleState(it.earliestDispatchDate, it.requestedDeliveryDate) == "Scheduled"
    }
    val preorderCount = activeOrders.count { it.orderType == "Pre-order" }
    val courierPending = activeOrders.count { it.courierSyncStatus == "queued_offline" || it.courierSyncStatus == "failed" }
    val codToCollect = activeOrders.sumOf { maxOf(0, it.outstanding) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (lang == "bn") "অর্ডার পরিসংখ্যান (রিয়েল-টাইম)" else "Order Overview (Real-Time)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DeepPink
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (lang == "bn") "মোট অর্ডার" else "Total Orders",
                        value = CoreUtils.formatDigits(totalCount, useBnDigits),
                        modifier = Modifier.weight(1f).clickable { viewModel.setActiveTab("orders") }
                    )
                    MetricCard(
                        title = if (lang == "bn") "আজকের নতুন" else "New Today",
                        value = CoreUtils.formatDigits(newToday, useBnDigits),
                        modifier = Modifier.weight(1f).clickable { viewModel.setActiveTab("orders") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (lang == "bn") "শিপমেন্টের অপেক্ষায়" else "Awaiting Ship",
                        value = CoreUtils.formatDigits(awaitingShip, useBnDigits),
                        modifier = Modifier.weight(1f).clickable { viewModel.setActiveTab("orders") }
                    )
                    MetricCard(
                        title = if (lang == "bn") "জরুরি (Urgent)" else "Urgent",
                        value = CoreUtils.formatDigits(urgentCount, useBnDigits),
                        valueColor = SoftRed,
                        modifier = Modifier.weight(1f).clickable { viewModel.setActiveTab("orders") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (lang == "bn") "শিডিউলড" else "Scheduled",
                        value = CoreUtils.formatDigits(scheduledCount, useBnDigits),
                        modifier = Modifier.weight(1f).clickable { viewModel.setActiveTab("orders") }
                    )
                    MetricCard(
                        title = if (lang == "bn") "প্রি-অর্ডার" else "Pre-orders",
                        value = CoreUtils.formatDigits(preorderCount, useBnDigits),
                        modifier = Modifier.weight(1f).clickable { viewModel.setActiveTab("orders") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (lang == "bn") "কুরিয়ার পেন্ডিং" else "Courier Pending",
                        value = CoreUtils.formatDigits(courierPending, useBnDigits),
                        valueColor = if (courierPending > 0) Color(0xFFD97706) else DeepPink,
                        modifier = Modifier.weight(1f).clickable { viewModel.setActiveTab("orders") }
                    )
                    MetricCard(
                        title = if (lang == "bn") "মোট COD বাকি" else "COD to Collect",
                        value = CoreUtils.formatBDT(codToCollect, useBnDigits),
                        valueColor = EmeraldGreen,
                        modifier = Modifier.weight(1f).clickable { viewModel.setActiveTab("orders") }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.resetForm()
                        viewModel.setActiveTab("create")
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_new_order_dashboard"),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (lang == "bn") "+ নতুন অর্ডার" else "+ New Order", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.setActiveTab("smartPaste") },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("btn_smart_paste_dashboard"),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (lang == "bn") "স্মার্ট পেস্ট" else "Smart Paste", fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = { viewModel.setActiveTab("settings") },
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .size(48.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = "Courier Settings", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = DeepPink
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListContent(viewModel: FashionAuraViewModel) {
    val lang by viewModel.lang.collectAsState()
    val useBnDigits by viewModel.useBnDigits.collectAsState()
    val activeOrders by viewModel.activeOrders.collectAsState()
    val allItems by viewModel.allOrderItems.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val filterPriority by viewModel.filterPriority.collectAsState()
    val filterPayment by viewModel.filterPayment.collectAsState()
    val filterFulfillment by viewModel.filterFulfillment.collectAsState()
    val filterSchedule by viewModel.filterSchedule.collectAsState()

    val itemsByOrder = remember(allItems) {
        allItems.groupBy { it.orderId }
    }

    val context = LocalContext.current

    val pendingCount = remember(activeOrders) {
        activeOrders.count { it.courierSyncStatus == "queued_offline" || it.courierSyncStatus == "failed" }
    }

    val filteredOrders = remember(
        activeOrders, itemsByOrder, searchQuery,
        filterType, filterPriority, filterPayment, filterFulfillment, filterSchedule
    ) {
        val q = CoreUtils.toEnglishDigits(searchQuery).lowercase().trim()
        activeOrders.filter { o ->
            if (filterType != "All" && o.orderType != filterType) return@filter false
            if (filterPriority != "All" && o.priority != filterPriority) return@filter false
            if (filterPayment != "All" && o.paymentStatus != filterPayment) return@filter false
            if (filterFulfillment != "All" && o.fulfillmentStatus != filterFulfillment) return@filter false
            val schedule = CoreUtils.getDeliveryScheduleState(o.earliestDispatchDate, o.requestedDeliveryDate)
            if (filterSchedule != "All" && schedule != filterSchedule) return@filter false

            if (q.isEmpty()) return@filter true

            val items = itemsByOrder[o.id] ?: emptyList()
            val itemMatch = items.any {
                it.productName.lowercase().contains(q) ||
                (it.sku != null && it.sku.lowercase().contains(q)) ||
                it.size.lowercase().contains(q)
            }

            o.orderNumber.lowercase().contains(q) ||
            o.customerName.lowercase().contains(q) ||
            o.primaryPhone.contains(q) ||
            o.fullAddress.lowercase().contains(q) ||
            o.district.lowercase().contains(q) ||
            (o.trackingNumber != null && o.trackingNumber.lowercase().contains(q)) ||
            itemMatch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            if (lang == "bn") "অর্ডার আইডি, নাম, মোবাইল বা ট্র্যাকিং..."
                            else "Search Order ID, Name, Phone, Tracking...",
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("input_search_orders"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )

                if (pendingCount > 0) {
                    Button(
                        onClick = { viewModel.bulkSyncOrders() },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sync ($pendingCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // 5 Filters
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterDropdown(
                        label = "Type",
                        currentValue = filterType,
                        options = listOf("All", "Regular", "Pre-order", "Free", "Exchange"),
                        onSelect = { viewModel.setFilterType(it) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterDropdown(
                        label = "Priority",
                        currentValue = filterPriority,
                        options = listOf("All", "Normal", "Urgent"),
                        onSelect = { viewModel.setFilterPriority(it) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterDropdown(
                        label = "Payment",
                        currentValue = filterPayment,
                        options = listOf("All", "Unpaid", "Partially Paid", "Paid"),
                        onSelect = { viewModel.setFilterPayment(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterDropdown(
                        label = "Status",
                        currentValue = filterFulfillment,
                        options = listOf("All", "New", "Confirmed", "Ready to Ship", "Shipped", "Delivered"),
                        onSelect = { viewModel.setFilterFulfillment(it) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterDropdown(
                        label = "Schedule",
                        currentValue = filterSchedule,
                        options = listOf("All", "Scheduled", "Unscheduled"),
                        onSelect = { viewModel.setFilterSchedule(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (lang == "bn") "কোনো অর্ডার পাওয়া যায়নি" else "No orders found",
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        items(filteredOrders, key = { it.id }) { order ->
            val orderItems = itemsByOrder[order.id] ?: emptyList()
            val scheduleState = CoreUtils.getDeliveryScheduleState(order.earliestDispatchDate, order.requestedDeliveryDate)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.populateEditOrder(order, orderItems) }
                    .testTag("order_card_${order.orderNumber}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Do Not Dispatch Before Banner
                    if (!order.earliestDispatchDate.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SoftRed, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Do Not Dispatch Before: ${order.earliestDispatchDate}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (order.courierSyncStatus != "sent") {
                                Surface(
                                    onClick = { viewModel.sendOrderToCourier(order.id, allowEarlyOverride = true) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White
                                ) {
                                    Text(
                                        text = "Override",
                                        color = SoftRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Order Number & Financial summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = order.orderNumber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = DeepPink,
                            fontSize = 15.sp
                        )
                        val cod = maxOf(0, order.outstanding)
                        Text(
                            text = "${CoreUtils.formatBDT(order.totalBill, useBnDigits)} (COD: ${CoreUtils.formatBDT(cod, useBnDigits)})",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    // Customer details
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = order.customerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(text = " — ", color = Color.Gray)
                            Text(
                                text = order.primaryPhone,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color.DarkGray
                            )
                        }
                        Text(
                            text = "${order.fullAddress}, ${order.thanaUpazila}, ${order.district}",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    // Items list
                    if (orderItems.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            orderItems.forEach { item ->
                                Text(
                                    text = "• ${item.productName} | Size: ${item.size} × ${item.quantity}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepPink
                                )
                            }
                        }
                    }

                    // Badges row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgeChip(text = order.orderType, bgColor = Color(0xFFF1F5F9), textColor = Color(0xFF334155))
                        BadgeChip(
                            text = order.priority,
                            bgColor = if (order.priority == "Urgent") Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                            textColor = if (order.priority == "Urgent") Color(0xFF991B1B) else Color(0xFF334155)
                        )
                        BadgeChip(text = order.paymentStatus, bgColor = Color(0xFFD1FAE5), textColor = EmeraldGreen)
                        BadgeChip(text = order.fulfillmentStatus, bgColor = Color(0xFFDBEAFE), textColor = Color(0xFF1E40AF))
                        BadgeChip(text = scheduleState, bgColor = Color(0xFFF3E8FF), textColor = AccentPurple)

                        // Courier Sync Status Chip
                        val courierBg = when (order.courierSyncStatus) {
                            "sent" -> Color(0xFFD1FAE5)
                            "queued_offline" -> Color(0xFFFEF3C7)
                            "failed" -> Color(0xFFFEE2E2)
                            "sending" -> Color(0xFFE0F2FE)
                            else -> Color(0xFFF1F5F9)
                        }
                        val courierFg = when (order.courierSyncStatus) {
                            "sent" -> EmeraldGreen
                            "queued_offline" -> Color(0xFF92400E)
                            "failed" -> Color(0xFF991B1B)
                            "sending" -> Color(0xFF075985)
                            else -> Color(0xFF475569)
                        }
                        BadgeChip(
                            text = "${order.courier}: ${order.courierSyncStatus}",
                            bgColor = courierBg,
                            textColor = courierFg
                        )
                    }

                    // Error note if any
                    if (!order.courierLastError.isNullOrBlank()) {
                        Text(
                            text = "Courier: ${order.courierLastError}",
                            fontSize = 10.sp,
                            color = SoftRed,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Direct Send to Courier Button
                        if (order.courierSyncStatus != "sent") {
                            Button(
                                onClick = { viewModel.sendOrderToCourier(order.id, allowEarlyOverride = false) },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_send_courier_${order.orderNumber}")
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Send to ${order.courier}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFD1FAE5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "#${order.trackingNumber}",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val text = CourierTemplateEngine.generateCourierText(order, orderItems, lang)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Courier Info", text)
                                cm.setPrimaryClip(clip)
                                viewModel.showToast(if (lang == "bn") "কুরিয়ার টেক্সট কপি হয়েছে!" else "Copied for Courier!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepPink),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_copy_courier_${order.orderNumber}")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(if (lang == "bn") "স্লিপ কপি" else "Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val text = CourierTemplateEngine.generateCourierText(order, orderItems, lang)
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Order ${order.orderNumber}")
                                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Order Slip"))
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                        }

                        Spacer(Modifier.weight(1f))

                        IconButton(
                            onClick = { viewModel.softDelete(order.id) },
                            modifier = Modifier.size(32.dp).testTag("btn_trash_${order.orderNumber}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Trash", tint = SoftRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CourierSettingsContent(viewModel: FashionAuraViewModel) {
    val lang by viewModel.lang.collectAsState()
    val configs by viewModel.courierConfigs.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF92400E), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (lang == "bn")
                            "এপিআই কি আপনার ফোনের Room DB-তে সুরক্ষিত থাকে।"
                        else
                            "API keys stored securely in local database.",
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        items(configs, key = { it.courier }) { cfg ->
            var enabled by remember(cfg.enabled) { mutableStateOf(cfg.enabled) }
            var baseUrl by remember(cfg.baseUrl) { mutableStateOf(cfg.baseUrl) }
            var apiKey by remember(cfg.apiKey) { mutableStateOf(cfg.apiKey) }
            var secretKey by remember(cfg.secretKeyOrToken) { mutableStateOf(cfg.secretKeyOrToken) }
            var storeId by remember(cfg.storeId) { mutableStateOf(cfg.storeId ?: "") }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = DeepPink)
                            Spacer(Modifier.width(6.dp))
                            Text("${cfg.courier} API", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DeepPink)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (enabled) "Enabled" else "Disabled", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Switch(checked = enabled, onCheckedChange = { enabled = it })
                        }
                    }

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("Base URL", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (cfg.courier == "Pathao") {
                        OutlinedTextField(
                            value = storeId,
                            onValueChange = { storeId = it },
                            label = { Text("Store ID", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = secretKey,
                            onValueChange = { secretKey = it },
                            label = { Text("Bearer Token", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("API Key", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = secretKey,
                            onValueChange = { secretKey = it },
                            label = { Text("Secret Key", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.saveCourierConfig(
                                cfg.copy(
                                    enabled = enabled,
                                    baseUrl = baseUrl.trim(),
                                    apiKey = apiKey.trim(),
                                    secretKeyOrToken = secretKey.trim(),
                                    storeId = if (cfg.courier == "Pathao") storeId.trim() else null
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPink),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save ${cfg.courier}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentValue == "All") "$label: All" else currentValue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontSize = 12.sp) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun BadgeChip(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmartPasteContent(viewModel: FashionAuraViewModel) {
    val lang by viewModel.lang.collectAsState()
    val rawText by viewModel.rawPasteText.collectAsState()

    val placeholderText = if (lang == "bn")
        "নাম: তানিয়া আক্তার\nমোবাইল: ০১৭১২৩৪৫৬৭৮\nঠিকানা: বাড়ি ১২, রোড ৫, ধানমন্ডি, ঢাকা\nপণ্য: Organza Three Piece\nসাইজ: 42\nদাম: 1650\nঅগ্রিম: 200\n২৮ তারিখে ডেলিভারি, ২৬ তারিখের আগে পাঠাবেন না"
    else
        "Name: Tania Akter\nPhone: 01712345678\nAddress: House 12, Road 5, Dhanmondi, Dhaka\nProduct: Organza Three Piece\nSize: 42\nPrice: 1650\nAdvance: 200\nDelivery Date: 28th, Do not dispatch before 26th"

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (lang == "bn") "স্মার্ট পেস্ট (WhatsApp / Facebook মেসেজ)" else "Smart Paste (Offline Order Message Parser)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPink
                    )

                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { viewModel.rawPasteText.value = it },
                        placeholder = { Text(placeholderText, fontSize = 12.sp, color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().height(180.dp).testTag("input_smart_paste"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { viewModel.handleSmartFill() },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_smart_fill"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (lang == "bn") "স্মার্ট ফিল করুন (রিভিউ ফর্মে যান)" else "Smart Fill -> Review Form",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderContent(viewModel: FashionAuraViewModel) {
    val lang by viewModel.lang.collectAsState()
    val useBnDigits by viewModel.useBnDigits.collectAsState()

    val custName by viewModel.customerName.collectAsState()
    val phone by viewModel.primaryPhone.collectAsState()
    val address by viewModel.fullAddress.collectAsState()
    val district by viewModel.district.collectAsState()
    val thana by viewModel.thanaUpazila.collectAsState()
    val orderType by viewModel.orderType.collectAsState()
    val priority by viewModel.priority.collectAsState()
    val courier by viewModel.courier.collectAsState()
    val prodName by viewModel.productName.collectAsState()
    val sku by viewModel.sku.collectAsState()
    val size by viewModel.size.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val unitPrice by viewModel.unitPrice.collectAsState()
    val earliestDispatch by viewModel.earliestDispatchDate.collectAsState()
    val requestedDelivery by viewModel.requestedDeliveryDate.collectAsState()
    val deliveryCharge by viewModel.deliveryCharge.collectAsState()
    val discount by viewModel.discount.collectAsState()
    val advancePaid by viewModel.advancePaid.collectAsState()
    val instructions by viewModel.customerInstructions.collectAsState()

    val liveCalc = remember(orderType, unitPrice, quantity, deliveryCharge, discount, advancePaid) {
        CoreUtils.calculateFinancials(
            orderType = orderType,
            items = listOf(Pair(unitPrice, quantity)),
            deliveryCharge = deliveryCharge,
            additionalCharge = 0,
            discount = discount,
            netPaid = advancePaid
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (lang == "bn") "অর্ডার ফর্ম (সব ফিল্ড যাচাই করুন)" else "Create / Review Order",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPink
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = custName,
                            onValueChange = { viewModel.customerName.value = it },
                            label = { Text("Customer Name *") },
                            modifier = Modifier.weight(1f).testTag("input_cust_name"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { viewModel.primaryPhone.value = it },
                            label = { Text("Phone 01XXXXXXXXX *") },
                            modifier = Modifier.weight(1f).testTag("input_cust_phone"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { viewModel.fullAddress.value = it },
                        label = { Text("Full Delivery Address *") },
                        modifier = Modifier.fillMaxWidth().testTag("input_cust_address"),
                        maxLines = 3
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterDropdown(
                            label = "District",
                            currentValue = district,
                            options = CoreUtils.BD_DISTRICTS,
                            onSelect = { viewModel.district.value = it },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = thana,
                            onValueChange = { viewModel.thanaUpazila.value = it },
                            label = { Text("Thana / Upazila") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterDropdown(
                            label = "Type",
                            currentValue = orderType,
                            options = listOf("Regular", "Pre-order", "Free", "Exchange"),
                            onSelect = { viewModel.orderType.value = it },
                            modifier = Modifier.weight(1f)
                        )
                        FilterDropdown(
                            label = "Priority",
                            currentValue = priority,
                            options = listOf("Normal", "Urgent"),
                            onSelect = { viewModel.priority.value = it },
                            modifier = Modifier.weight(1f)
                        )
                        FilterDropdown(
                            label = "Courier",
                            currentValue = courier,
                            options = listOf("Steadfast", "Pathao", "RedX", "Paperfly", "Sundarban"),
                            onSelect = { viewModel.courier.value = it },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = prodName,
                            onValueChange = { viewModel.productName.value = it },
                            label = { Text("Product Name *") },
                            modifier = Modifier.weight(2f).testTag("input_prod_name"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { viewModel.sku.value = it },
                            label = { Text("SKU") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterDropdown(
                            label = "Size",
                            currentValue = "Size $size",
                            options = listOf("36", "38", "40", "42", "44", "46", "48", "Other"),
                            onSelect = { viewModel.size.value = it },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = quantity.toString(),
                            onValueChange = { viewModel.quantity.value = CoreUtils.parseIntSafe(it).coerceAtLeast(1) },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(0.7f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unitPrice.toString(),
                            onValueChange = { viewModel.unitPrice.value = CoreUtils.parseIntSafe(it) },
                            label = { Text("Unit Price ৳") },
                            modifier = Modifier.weight(1.3f).testTag("input_unit_price"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = earliestDispatch,
                            onValueChange = { viewModel.earliestDispatchDate.value = it },
                            label = { Text("Do Not Dispatch (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = requestedDelivery,
                            onValueChange = { viewModel.requestedDeliveryDate.value = it },
                            label = { Text("Delivery Date (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = deliveryCharge.toString(),
                            onValueChange = { viewModel.deliveryCharge.value = CoreUtils.parseIntSafe(it) },
                            label = { Text("Delivery ৳") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = discount.toString(),
                            onValueChange = { viewModel.discount.value = CoreUtils.parseIntSafe(it) },
                            label = { Text("Discount ৳") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = advancePaid.toString(),
                            onValueChange = { viewModel.advancePaid.value = CoreUtils.parseIntSafe(it) },
                            label = { Text("Advance ৳") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { viewModel.customerInstructions.value = it },
                        label = { Text("Customer Delivery Instructions (Printed on Slip)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total: ${CoreUtils.formatBDT(liveCalc.totalBill, useBnDigits)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "COD: ${CoreUtils.formatBDT(liveCalc.codAmount, useBnDigits)}",
                                color = Color(0xFFFBCFE8),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.saveOrder() },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_save_order"),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPink),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (lang == "bn") "অর্ডার সেভ করুন" else "Save Order", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BackupTrashContent(viewModel: FashionAuraViewModel) {
    val lang by viewModel.lang.collectAsState()
    val trashedOrders by viewModel.trashedOrders.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (lang == "bn") "ব্যাকআপ ও এক্সপোর্ট" else "Backup, Export & Trash",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepPink
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val (orders, _, _) = viewModel.getAllDataForBackup()
                                    val csv = ExportService.generateOrdersCSV(orders.filter { !it.isDeleted })
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                    ExportService.shareTextFile(
                                        context,
                                        "FashionAura_Orders_$dateStr.csv",
                                        csv,
                                        "text/csv"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_export_csv"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val (orders, items, logs) = viewModel.getAllDataForBackup()
                                    val json = ExportService.generateFullBackupJSON(orders, items, logs)
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                    ExportService.shareTextFile(
                                        context,
                                        "FashionAura_Backup_$dateStr.json",
                                        json,
                                        "application/json"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("btn_export_json"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Full JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${if (lang == "bn") "ট্র্যাশ (মুছে ফেলা অর্ডার)" else "Trash"} (${trashedOrders.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )

                    if (trashedOrders.isEmpty()) {
                        Text(
                            text = if (lang == "bn") "ট্র্যাশে কোনো অর্ডার নেই" else "No orders in trash",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    } else {
                        trashedOrders.forEach { o ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(o.orderNumber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(o.customerName, fontSize = 11.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = { viewModel.restore(o.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD1FAE5), contentColor = EmeraldGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_restore_${o.orderNumber}")
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (lang == "bn") "পুনরুদ্ধার" else "Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
