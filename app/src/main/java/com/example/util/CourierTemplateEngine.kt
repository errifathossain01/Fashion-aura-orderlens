package com.example.util

import com.example.data.OrderEntity
import com.example.data.OrderItemEntity

object CourierTemplateEngine {
    fun generateCourierText(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        lang: String = "bn"
    ): String {
        val itemsText = items.joinToString(", ") { item ->
            val sizeLabel = if (lang == "bn") "সাইজ" else "Size"
            val displaySize = if (item.size == "Other") (item.customSizeText ?: "Other") else item.size
            "${item.productName} ($sizeLabel: $displaySize × ${item.quantity})"
        }

        val cod = maxOf(0, order.outstanding)
        val altPhoneText = if (!order.alternatePhone.isNullOrBlank()) ", ${order.alternatePhone}" else ""

        return if (lang == "bn") {
            val lines = mutableListOf(
                "অর্ডার আইডি: ${order.orderNumber}",
                "নাম: ${order.customerName}",
                "মোবাইল: ${order.primaryPhone}$altPhoneText",
                "ঠিকানা: ${order.fullAddress}, ${order.thanaUpazila}, ${order.district}",
                "পণ্য: $itemsText",
                "মোট বিল: ৳${order.totalBill} (ডেলিভারি চার্জ: ৳${order.deliveryCharge})",
                "ক্যাশ অন ডেলিভারি (COD): " + (if (cod == 0) "প্রিপেইড (৳০)" else "৳$cod")
            )
            if (!order.requestedDeliveryDate.isNullOrBlank()) {
                lines.add("ডেলিভারি তারিখ: ${order.requestedDeliveryDate}")
            }
            if (!order.customerDeliveryInstructions.isNullOrBlank()) {
                lines.add("নির্দেশনা: ${order.customerDeliveryInstructions}")
            }
            lines.joinToString("\n")
        } else {
            val lines = mutableListOf(
                "Order ID: ${order.orderNumber}",
                "Customer: ${order.customerName}",
                "Phone: ${order.primaryPhone}$altPhoneText",
                "Address: ${order.fullAddress}, ${order.thanaUpazila}, ${order.district}",
                "Items: $itemsText",
                "Total Bill: BDT ${order.totalBill} (Delivery: BDT ${order.deliveryCharge})",
                "COD to Collect: " + (if (cod == 0) "Prepaid (BDT 0)" else "BDT $cod")
            )
            if (!order.requestedDeliveryDate.isNullOrBlank()) {
                lines.add("Requested Delivery: ${order.requestedDeliveryDate}")
            }
            if (!order.customerDeliveryInstructions.isNullOrBlank()) {
                lines.add("Note: ${order.customerDeliveryInstructions}")
            }
            lines.joinToString("\n")
        }
    }
}
