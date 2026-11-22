package com.easycash.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY next_due_at, id")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun find(id: Long): ReminderEntity?

    @Query("SELECT r.* FROM recurring_transactions r JOIN accounts a ON a.id = r.account_id WHERE r.is_active = 1 AND a.is_archived = 0")
    suspend fun active(): List<ReminderEntity>

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("UPDATE recurring_transactions SET is_active = 0, updated_at = :now WHERE account_id = :accountId")
    suspend fun deactivateForAccount(accountId: Long, now: Long)

    @Query("SELECT COUNT(*) FROM alert_receipts WHERE \u0060key\u0060 = :key")
    suspend fun receiptCount(key: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun record(receipt: AlertReceiptEntity): Long
}
