package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val email: String,
    val role: String, // ADMIN, MANAGER, CASHIER
    val pin: String,  // 4-6 digit quick PIN or passcode
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val address: String = "",
    val active: Boolean = true
)

@Entity(
    tableName = "denominations",
    indices = [Index(value = ["currencyCode", "value"], unique = true)]
)
data class DenominationConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val currencyCode: String,
    val value: Double,
    val type: String, // NOTE, COIN
    val active: Boolean = true,
    val displayOrder: Int = 0
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transactionNumber"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["userId"]),
        Index(value = ["locationId"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionNumber: String,
    val userId: Int,
    val userName: String,
    val locationId: Int,
    val locationName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val openingCash: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val difference: Double = 0.0,
    val notes: String = "",
    val status: String // BALANCED, SHORTAGE, EXCESS
)

@Entity(
    tableName = "transaction_items",
    indices = [Index(value = ["transactionId"])]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val denominationValue: Double,
    val denominationType: String,
    val quantity: Int,
    val amount: Double
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val businessName: String = "My Retail Store",
    val logoText: String = "CCP",
    val address: String = "Shop No. 4, Market Complex",
    val phone: String = "+91 98765 43210",
    val email: String = "accounts@myretail.com",
    val gstin: String = "27AAAAA0000A1Z5",
    val currencyCode: String = "INR",
    val receiptFooter: String = "Thank you for tallying your register accurately.",
    val reportFooter: String = "Generated securely by CashCount Pro",
    val isDarkMode: Boolean? = null // null means system default
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["timestamp"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Int,
    val userName: String,
    val action: String, // LOGIN, CREATE_COUNT, EDIT_COUNT, DELETE_COUNT, BACKUP_EXPORT, RESTORE, etc.
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
