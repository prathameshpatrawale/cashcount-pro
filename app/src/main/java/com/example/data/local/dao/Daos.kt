package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.DenominationConfigEntity
import com.example.data.local.entity.LocationEntity
import com.example.data.local.entity.SettingsEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Int)
}

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY name ASC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE active = 1 ORDER BY name ASC")
    fun getActiveLocations(): Flow<List<LocationEntity>>

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getLocationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun deleteLocationById(id: Int)
}

@Dao
interface DenominationConfigDao {
    @Query("SELECT * FROM denominations WHERE currencyCode = :currencyCode ORDER BY displayOrder ASC, value DESC")
    fun getDenominationsForCurrency(currencyCode: String): Flow<List<DenominationConfigEntity>>

    @Query("SELECT * FROM denominations WHERE currencyCode = :currencyCode ORDER BY displayOrder ASC, value DESC")
    suspend fun getDenominationsForCurrencySync(currencyCode: String): List<DenominationConfigEntity>

    @Query("SELECT COUNT(*) FROM denominations WHERE currencyCode = :currencyCode")
    suspend fun getDenominationCountForCurrency(currencyCode: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDenominations(denominations: List<DenominationConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDenomination(denomination: DenominationConfigEntity): Long

    @Update
    suspend fun updateDenomination(denomination: DenominationConfigEntity)

    @Query("DELETE FROM denominations WHERE id = :id")
    suspend fun deleteDenominationById(id: Int)

    @Query("DELETE FROM denominations WHERE currencyCode = :currencyCode")
    suspend fun deleteDenominationsForCurrency(currencyCode: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY denominationValue DESC")
    suspend fun getItemsForTransaction(transactionId: Long): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY denominationValue DESC")
    fun getItemsForTransactionFlow(transactionId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items")
    suspend fun getAllTransactionItemsSync(): List<TransactionItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun deleteItemsForTransaction(transactionId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transaction_items")
    suspend fun deleteAllTransactionItems()

    @Transaction
    suspend fun saveFullTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long {
        val txId = insertTransaction(transaction)
        val itemsWithTxId = items.map { it.copy(transactionId = txId) }
        insertTransactionItems(itemsWithTxId)
        return txId
    }

    @Transaction
    suspend fun updateFullTransaction(
        transaction: TransactionEntity,
        items: List<TransactionItemEntity>
    ) {
        updateTransaction(transaction)
        deleteItemsForTransaction(transaction.id)
        val itemsWithTxId = items.map { it.copy(transactionId = transaction.id) }
        insertTransactionItems(itemsWithTxId)
    }

    @Transaction
    suspend fun deleteFullTransaction(id: Long) {
        deleteItemsForTransaction(id)
        deleteTransactionById(id)
    }
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}
