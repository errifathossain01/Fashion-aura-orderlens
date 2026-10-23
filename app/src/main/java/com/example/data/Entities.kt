package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["orderNumber"], unique = true),
        Index(value = ["primaryPhone"]),
        Index(value = ["isDeleted"])
    ]
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val customerId: String,
    val customerName: String,
    val primaryPhone: String,
    val alternatePhone: String? = null,
    val fullAddress: String,
    val areaVillageStreet: String? = null,
    val district: String,
    val thanaUpazila: String,
    val landmark: String? = null,
    val orderType: String = "Regular", // Regular, Pre-order, Free, Exchange
    val priority: String = "Normal", // Normal, Urgent
    val paymentStatus: String = "Unpaid", // Unpaid, Partially Paid, Paid, etc.
    val fulfillmentStatus: String = "New", // New, Confirmed, Preparing, Ready to Ship, Shipped, Delivered, etc.
    val orderDate: String,
    val requestedDeliveryDate: String? = null,
    val requestedDeliveryTimeWindow: String? = null,
    val earliestDispatchDate: String? = null,
    val preorderAvailabilityDate: String? = null,
    val actualShipDate: String? = null,
    val actualDeliveryDate: String? = null,
    val assignedEmployeeName: String? = null,
    val source: String = "WhatsApp",
    val courier: String = "Steadfast",
    val trackingNumber: String? = null,
    val courierSyncStatus: String = "not_sent", // not_sent, queued_offline, sending, sent, failed
    val courierConsignmentId: String? = null,
    val courierLastError: String? = null,
    val courierSentAt: String? = null,
    val subtotal: Int = 0,
    val deliveryCharge: Int = 80,
    val additionalCharge: Int = 0,
    val discount: Int = 0,
    val totalBill: Int = 0,
    val netPaid: Int = 0,
    val outstanding: Int = 0,
    val customerDeliveryInstructions: String? = null,
    val internalStaffNotes: String? = null,
    val exchangeReturnNotes: String? = null,
    val freeOrderReason: String? = null,
    val dispatchOverrideReason: String? = null,
    val isDraft: Boolean = false,
    val isDeleted: Boolean = false,
    val version: Int = 1,
    val createdAt: String,
    val createdBy: String = "Admin",
    val updatedAt: String,
    val updatedBy: String = "Admin",
    val syncState: String = "pending"
)

@Entity(
    tableName = "order_items",
    indices = [Index(value = ["orderId"])]
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productName: String,
    val sku: String? = null,
    val size: String = "40",
    val customSizeText: String? = null,
    val quantity: Int = 1,
    val unitPrice: Int = 1250,
    val color: String? = null,
    val productNotes: String? = null,
    val productImageBase64: String? = null
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val details: String,
    val performedBy: String = "Admin",
    val performedAt: String
)

@Entity(tableName = "courier_configs")
data class CourierConfigEntity(
    @PrimaryKey val courier: String, // Steadfast, Pathao
    val enabled: Boolean = false,
    val baseUrl: String,
    val apiKey: String = "",
    val secretKeyOrToken: String = "",
    val storeId: String? = null,
    val updatedAt: String
)
