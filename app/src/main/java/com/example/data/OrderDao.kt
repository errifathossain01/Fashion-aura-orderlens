package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getActiveOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getTrashedOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersSync(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: String): OrderEntity?

    @Query("SELECT * FROM order_items")
    fun getAllOrderItems(): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items")
    suspend fun getAllOrderItemsSync(): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: String): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItemsForOrder(orderId: String)

    @Query("UPDATE orders SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun softDeleteOrder(orderId: String, updatedAt: String)

    @Query("UPDATE orders SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun restoreOrder(orderId: String, updatedAt: String)

    @Query("""
        UPDATE orders 
        SET courierSyncStatus = :status, 
            trackingNumber = :trackingNumber, 
            courierConsignmentId = :consignmentId, 
            courierLastError = :errorMsg, 
            courierSentAt = :sentAt,
            fulfillmentStatus = CASE WHEN fulfillmentStatus IN ('New', 'Confirmed') THEN 'Ready to Ship' ELSE fulfillmentStatus END,
            updatedAt = :sentAt 
        WHERE id = :orderId
    """)
    suspend fun updateCourierSuccess(
        orderId: String,
        status: String,
        trackingNumber: String,
        consignmentId: String,
        sentAt: String,
        errorMsg: String? = null
    )

    @Query("UPDATE orders SET courierSyncStatus = :status, courierLastError = :errorMsg, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateCourierFailure(orderId: String, status: String, errorMsg: String, updatedAt: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY performedAt DESC")
    suspend fun getAllAuditLogsSync(): List<AuditLogEntity>

    // Courier Configs
    @Query("SELECT * FROM courier_configs")
    fun getCourierConfigsFlow(): Flow<List<CourierConfigEntity>>

    @Query("SELECT * FROM courier_configs WHERE courier = :courier")
    suspend fun getCourierConfig(courier: String): CourierConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCourierConfig(config: CourierConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultCourierConfigs(configs: List<CourierConfigEntity>)

    @Query("SELECT COUNT(*) FROM courier_configs")
    suspend fun getCourierConfigCount(): Int

    @Transaction
    suspend fun saveOrderWithItem(
        order: OrderEntity,
        item: OrderItemEntity,
        auditLog: AuditLogEntity
    ) {
        insertOrder(order)
        deleteItemsForOrder(order.id)
        insertOrderItem(item)
        insertAuditLog(auditLog)
    }
}
