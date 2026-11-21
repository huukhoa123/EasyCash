package com.easycash.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT COUNT(*) FROM notification_receipts WHERE fingerprint = :key")
    suspend fun receiptCount(key: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReceipt(receipt: NotificationReceiptEntity): Long

    @Query("SELECT * FROM notification_sources WHERE package_name = :packageName AND is_enabled = 1")
    suspend fun enabledSource(packageName: String): NotificationSourceEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE dedup_key = :key")
    suspend fun transactionCount(key: String): Int

    @Query("UPDATE transactions SET dedup_key = :key WHERE id = :id")
    suspend fun setDedupKey(id: Long, key: String)

    @Query("SELECT * FROM notification_sources ORDER BY id")
    fun observeSources(): Flow<List<NotificationSourceEntity>>

    @Query("SELECT * FROM notification_sources WHERE id = :id")
    suspend fun source(id: Long): NotificationSourceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSource(source: NotificationSourceEntity): Long

    @Update
    suspend fun updateSource(source: NotificationSourceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: NotificationEventEntity): Long

    @Query("SELECT * FROM notification_events ORDER BY id")
    suspend fun events(): List<NotificationEventEntity>

    @Query("DELETE FROM notification_events WHERE expires_at <= :now")
    suspend fun deleteExpired(now: Long): Int
}
