package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.DenominationConfigEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.SettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UserEntity
import com.example.data.model.Currency
import com.example.data.model.DenominationType
import com.example.engine.DenominationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CashCountRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val locationDao = database.locationDao()
    private val denominationDao = database.denominationConfigDao()
    private val transactionDao = database.transactionDao()
    private val settingsDao = database.settingsDao()
    private val auditLogDao = database.auditLogDao()

    // Flows
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allLocations: Flow<List<LocationEntity>> = locationDao.getAllLocations()
    val activeLocations: Flow<List<LocationEntity>> = locationDao.getActiveLocations()
    val settingsFlow: Flow<SettingsEntity?> = settingsDao.getSettings()
    val auditLogs: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogs()

    fun getDenominationsForCurrency(currencyCode: String): Flow<List<DenominationConfigEntity>> {
        return denominationDao.getDenominationsForCurrency(currencyCode)
    }

    suspend fun getDenominationsForCurrencySync(currencyCode: String): List<DenominationConfigEntity> {
        return denominationDao.getDenominationsForCurrencySync(currencyCode)
    }

    /**
     * Initializes default database entries if empty.
     */
    suspend fun initializeDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        // 1. Settings
        val currentSettings = settingsDao.getSettingsSync()
        if (currentSettings == null) {
            settingsDao.saveSettings(
                SettingsEntity(
                    id = 1,
                    businessName = "Apex Retail & Supermarket",
                    address = "Plot 42, Commercial Avenue, Sector 5",
                    phone = "+91 98201 12345",
                    email = "finance@apexretail.com",
                    gstin = "27AABCA1234F1Z9",
                    currencyCode = "INR",
                    receiptFooter = "Thank you for reconciling the cash register accurately.",
                    reportFooter = "Certified Cash Statement - CashCount Pro"
                )
            )
        }

        // 2. Users (Clean single account without personal names/roles)
        val existingUsers = userDao.getUserCount()
        if (existingUsers == 0) {
            userDao.insertUser(
                UserEntity(
                    id = 1,
                    name = "Cashier",
                    email = "cashier@cashcount.pro",
                    role = "CASHIER",
                    pin = "0000"
                )
            )
        } else {
            // Clean up legacy multi-user records if present
            userDao.deleteUserById(2)
            userDao.deleteUserById(3)
            val u1 = userDao.getUserById(1)
            if (u1 != null && (u1.name.contains("Rajesh") || u1.name.contains("Admin"))) {
                userDao.updateUser(u1.copy(name = "Cashier", role = "CASHIER"))
            }
        }

        // 3. Locations
        if (locationDao.getLocationCount() == 0) {
            locationDao.insertLocations(
                listOf(
                    LocationEntity(id = 1, name = "Counter 01 - Main Till", address = "Front Entrance"),
                    LocationEntity(id = 2, name = "Counter 02 - Express Checkout", address = "Aisle 3"),
                    LocationEntity(id = 3, name = "Back Office Vault", address = "Accounts Dept")
                )
            )
        }

        // 4. Denominations for INR
        ensureCurrencyDenominationsSeeded(Currency.INR.code)
    }

    suspend fun ensureCurrencyDenominationsSeeded(currencyCode: String) = withContext(Dispatchers.IO) {
        val count = denominationDao.getDenominationCountForCurrency(currencyCode)
        if (count == 0) {
            val currency = Currency.fromCode(currencyCode)
            val list = mutableListOf<DenominationConfigEntity>()
            var order = 1
            for (note in currency.defaultNotes) {
                list.add(
                    DenominationConfigEntity(
                        currencyCode = currencyCode,
                        value = note,
                        type = DenominationType.NOTE.name,
                        active = true,
                        displayOrder = order++
                    )
                )
            }
            for (coin in currency.defaultCoins) {
                list.add(
                    DenominationConfigEntity(
                        currencyCode = currencyCode,
                        value = coin,
                        type = DenominationType.COIN.name,
                        active = true,
                        displayOrder = order++
                    )
                )
            }
            denominationDao.insertDenominations(list)
        }
    }

    // Transaction operations
    suspend fun getTransactionById(id: Long): TransactionEntity? = withContext(Dispatchers.IO) {
        transactionDao.getTransactionById(id)
    }

    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItemEntity> =
        withContext(Dispatchers.IO) {
            transactionDao.getItemsForTransaction(transactionId)
        }

    fun getItemsForTransactionFlow(transactionId: Long): Flow<List<TransactionItemEntity>> {
        return transactionDao.getItemsForTransactionFlow(transactionId)
    }

    suspend fun saveTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long = withContext(Dispatchers.IO) {
        val id = transactionDao.saveFullTransaction(transaction, items)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = transaction.userId,
                userName = transaction.userName,
                action = "TRANSACTION_CREATED",
                details = "Saved ${transaction.transactionNumber} for ${transaction.actualCash} (${transaction.status})"
            )
        )
        id
    }

    suspend fun updateTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ) = withContext(Dispatchers.IO) {
        transactionDao.updateFullTransaction(transaction, items)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = transaction.userId,
                userName = transaction.userName,
                action = "TRANSACTION_EDITED",
                details = "Updated ${transaction.transactionNumber} for ${transaction.actualCash}"
            )
        )
    }

    suspend fun deleteTransaction(
        id: Long,
        txNumber: String,
        userId: Int,
        userName: String
    ) = withContext(Dispatchers.IO) {
        transactionDao.deleteFullTransaction(id)
        auditLogDao.insertLog(
            AuditLogEntity(
                userId = userId,
                userName = userName,
                action = "TRANSACTION_DELETED",
                details = "Deleted transaction $txNumber (ID: $id)"
            )
        )
    }

    // Settings
    suspend fun updateSettings(settings: SettingsEntity, userId: Int, userName: String) =
        withContext(Dispatchers.IO) {
            settingsDao.saveSettings(settings)
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = userId,
                    userName = userName,
                    action = "SETTINGS_UPDATED",
                    details = "Updated business profile & currency: ${settings.currencyCode}"
                )
            )
        }

    // Denominations management
    suspend fun addDenomination(denom: DenominationConfigEntity) = withContext(Dispatchers.IO) {
        denominationDao.insertDenomination(denom)
    }

    suspend fun updateDenomination(denom: DenominationConfigEntity) = withContext(Dispatchers.IO) {
        denominationDao.updateDenomination(denom)
    }

    suspend fun deleteDenomination(id: Int) = withContext(Dispatchers.IO) {
        denominationDao.deleteDenominationById(id)
    }

    // Users
    suspend fun addUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(id: Int) = withContext(Dispatchers.IO) {
        userDao.deleteUserById(id)
    }

    // Locations
    suspend fun addLocation(location: LocationEntity) = withContext(Dispatchers.IO) {
        locationDao.insertLocation(location)
    }

    suspend fun updateLocation(location: LocationEntity) = withContext(Dispatchers.IO) {
        locationDao.updateLocation(location)
    }

    suspend fun deleteLocation(id: Int) = withContext(Dispatchers.IO) {
        locationDao.deleteLocationById(id)
    }

    // Audit logs
    suspend fun logAction(userId: Int, userName: String, action: String, details: String) =
        withContext(Dispatchers.IO) {
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = userId,
                    userName = userName,
                    action = action,
                    details = details
                )
            )
        }

    // Demo Data
    suspend fun loadDemoData(currentUserId: Int, currentUserName: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dayMillis = 24 * 60 * 60 * 1000L

        // Generate realistic transaction samples across recent days
        val sampleTransactions = listOf(
            Triple(
                TransactionEntity(
                    transactionNumber = "TXN-${System.currentTimeMillis() % 100000}-01",
                    userId = 1,
                    userName = "Cashier",
                    locationId = 1,
                    locationName = "Counter 01 - Main Till",
                    timestamp = now - (1 * 3600 * 1000L),
                    openingCash = 2000.0,
                    expectedCash = 28500.0,
                    actualCash = 28500.0,
                    difference = 0.0,
                    notes = "Evening shift closing. Cash perfectly balanced.",
                    status = "BALANCED"
                ),
                listOf(
                    Pair(500.0, 40), // 20000
                    Pair(200.0, 30), // 6000
                    Pair(100.0, 20), // 2000
                    Pair(50.0, 8),   // 400
                    Pair(10.0, 10)   // 100 -> total 28500
                ),
                listOf(Pair(20.0, 0))
            ),
            Triple(
                TransactionEntity(
                    transactionNumber = "TXN-${System.currentTimeMillis() % 100000}-02",
                    userId = 1,
                    userName = "Cashier",
                    locationId = 2,
                    locationName = "Counter 02 - Express Checkout",
                    timestamp = now - (5 * 3600 * 1000L),
                    openingCash = 1500.0,
                    expectedCash = 14200.0,
                    actualCash = 14250.0,
                    difference = 50.0,
                    notes = "Afternoon count. Slight excess due to uncollected coin change.",
                    status = "EXCESS"
                ),
                listOf(
                    Pair(500.0, 20), // 10000
                    Pair(200.0, 15), // 3000
                    Pair(100.0, 10), // 1000
                    Pair(50.0, 4),   // 200
                    Pair(10.0, 5)    // 50 -> 14250
                ),
                listOf()
            ),
            Triple(
                TransactionEntity(
                    transactionNumber = "TXN-${System.currentTimeMillis() % 100000}-03",
                    userId = 1,
                    userName = "Cashier",
                    locationId = 1,
                    locationName = "Counter 01 - Main Till",
                    timestamp = now - dayMillis,
                    openingCash = 2000.0,
                    expectedCash = 31200.0,
                    actualCash = 31100.0,
                    difference = -100.0,
                    notes = "Yesterday end of day. Shortage verified with receipt audit.",
                    status = "SHORTAGE"
                ),
                listOf(
                    Pair(500.0, 50), // 25000
                    Pair(200.0, 25), // 5000
                    Pair(100.0, 10), // 1000
                    Pair(20.0, 5)    // 100 -> 31100
                ),
                listOf()
            ),
            Triple(
                TransactionEntity(
                    transactionNumber = "TXN-${System.currentTimeMillis() % 100000}-04",
                    userId = 1,
                    userName = "Cashier",
                    locationId = 3,
                    locationName = "Back Office Vault",
                    timestamp = now - (2 * dayMillis),
                    openingCash = 50000.0,
                    expectedCash = 125000.0,
                    actualCash = 125000.0,
                    difference = 0.0,
                    notes = "Weekly vault audit and deposit tally.",
                    status = "BALANCED"
                ),
                listOf(
                    Pair(500.0, 200), // 100000
                    Pair(200.0, 100), // 20000
                    Pair(100.0, 50)   // 5000 -> 125000
                ),
                listOf()
            )
        )

        for ((tx, notes, coins) in sampleTransactions) {
            val items = mutableListOf<TransactionItemEntity>()
            for ((value, qty) in notes) {
                items.add(
                    TransactionItemEntity(
                        transactionId = 0,
                        denominationValue = value,
                        denominationType = "NOTE",
                        quantity = qty,
                        amount = value * qty
                    )
                )
            }
            for ((value, qty) in coins) {
                items.add(
                    TransactionItemEntity(
                        transactionId = 0,
                        denominationValue = value,
                        denominationType = "COIN",
                        quantity = qty,
                        amount = value * qty
                    )
                )
            }
            transactionDao.saveFullTransaction(tx, items)
        }

        auditLogDao.insertLog(
            AuditLogEntity(
                userId = currentUserId,
                userName = currentUserName,
                action = "DEMO_DATA_LOADED",
                details = "Loaded realistic sample transactions for testing"
            )
        )
    }

    suspend fun clearAllTransactions(currentUserId: Int, currentUserName: String) =
        withContext(Dispatchers.IO) {
            transactionDao.deleteAllTransactions()
            transactionDao.deleteAllTransactionItems()
            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = currentUserId,
                    userName = currentUserName,
                    action = "DEMO_DATA_CLEARED",
                    details = "Cleared all transaction records"
                )
            )
        }

    // Backup & Restore
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "CashCount Pro")
        root.put("version", "1.0")
        root.put("exportedAt", System.currentTimeMillis())

        // Settings
        val settings = settingsDao.getSettingsSync()
        if (settings != null) {
            val sObj = JSONObject()
            sObj.put("businessName", settings.businessName)
            sObj.put("address", settings.address)
            sObj.put("phone", settings.phone)
            sObj.put("email", settings.email)
            sObj.put("gstin", settings.gstin)
            sObj.put("currencyCode", settings.currencyCode)
            sObj.put("receiptFooter", settings.receiptFooter)
            sObj.put("reportFooter", settings.reportFooter)
            root.put("settings", sObj)
        }

        // Transactions
        val txs = transactionDao.getAllTransactionsSync()
        val txArray = JSONArray()
        for (tx in txs) {
            val txObj = JSONObject()
            txObj.put("id", tx.id)
            txObj.put("transactionNumber", tx.transactionNumber)
            txObj.put("userId", tx.userId)
            txObj.put("userName", tx.userName)
            txObj.put("locationId", tx.locationId)
            txObj.put("locationName", tx.locationName)
            txObj.put("timestamp", tx.timestamp)
            txObj.put("openingCash", tx.openingCash)
            txObj.put("expectedCash", tx.expectedCash)
            txObj.put("actualCash", tx.actualCash)
            txObj.put("difference", tx.difference)
            txObj.put("notes", tx.notes)
            txObj.put("status", tx.status)

            // Items
            val items = transactionDao.getItemsForTransaction(tx.id)
            val itemsArray = JSONArray()
            for (it in items) {
                val itObj = JSONObject()
                itObj.put("denominationValue", it.denominationValue)
                itObj.put("denominationType", it.denominationType)
                itObj.put("quantity", it.quantity)
                itObj.put("amount", it.amount)
                itemsArray.put(itObj)
            }
            txObj.put("items", itemsArray)
            txArray.put(txObj)
        }
        root.put("transactions", txArray)

        root.toString(2)
    }

    suspend fun restoreBackupJson(
        jsonString: String,
        currentUserId: Int,
        currentUserName: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("transactions")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid backup format: missing transactions"))
            }

            // Restore settings if present
            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val current = settingsDao.getSettingsSync() ?: SettingsEntity()
                settingsDao.saveSettings(
                    current.copy(
                        businessName = sObj.optString("businessName", current.businessName),
                        address = sObj.optString("address", current.address),
                        phone = sObj.optString("phone", current.phone),
                        email = sObj.optString("email", current.email),
                        gstin = sObj.optString("gstin", current.gstin),
                        currencyCode = sObj.optString("currencyCode", current.currencyCode),
                        receiptFooter = sObj.optString("receiptFooter", current.receiptFooter),
                        reportFooter = sObj.optString("reportFooter", current.reportFooter)
                    )
                )
            }

            val txArray = root.getJSONArray("transactions")
            var importedCount = 0

            for (i in 0 until txArray.length()) {
                val txObj = txArray.getJSONObject(i)
                val tx = TransactionEntity(
                    transactionNumber = txObj.getString("transactionNumber"),
                    userId = txObj.optInt("userId", currentUserId),
                    userName = txObj.optString("userName", "Restored User"),
                    locationId = txObj.optInt("locationId", 1),
                    locationName = txObj.optString("locationName", "Main Counter"),
                    timestamp = txObj.optLong("timestamp", System.currentTimeMillis()),
                    openingCash = txObj.optDouble("openingCash", 0.0),
                    expectedCash = txObj.optDouble("expectedCash", 0.0),
                    actualCash = txObj.optDouble("actualCash", 0.0),
                    difference = txObj.optDouble("difference", 0.0),
                    notes = txObj.optString("notes", ""),
                    status = txObj.optString("status", "BALANCED")
                )

                val itemsList = mutableListOf<TransactionItemEntity>()
                if (txObj.has("items")) {
                    val itArray = txObj.getJSONArray("items")
                    for (j in 0 until itArray.length()) {
                        val itObj = itArray.getJSONObject(j)
                        itemsList.add(
                            TransactionItemEntity(
                                transactionId = 0,
                                denominationValue = itObj.getDouble("denominationValue"),
                                denominationType = itObj.optString("denominationType", "NOTE"),
                                quantity = itObj.getInt("quantity"),
                                amount = itObj.getDouble("amount")
                            )
                        )
                    }
                }
                transactionDao.saveFullTransaction(tx, itemsList)
                importedCount++
            }

            auditLogDao.insertLog(
                AuditLogEntity(
                    userId = currentUserId,
                    userName = currentUserName,
                    action = "BACKUP_RESTORED",
                    details = "Restored $importedCount transactions from backup JSON"
                )
            )

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
