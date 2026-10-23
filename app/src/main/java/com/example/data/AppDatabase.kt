package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        OrderEntity::class,
        OrderItemEntity::class,
        AuditLogEntity::class,
        CourierConfigEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fashion_aura_oms.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate default configs
                            CoroutineScope(Dispatchers.IO).launch {
                                val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                                INSTANCE?.orderDao()?.insertDefaultCourierConfigs(
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
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
