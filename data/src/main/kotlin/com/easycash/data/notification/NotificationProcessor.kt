package com.easycash.data.notification

import androidx.room.withTransaction
import com.easycash.data.RoomFinanceRepository
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.local.NotificationEventEntity
import com.easycash.data.local.NotificationReceiptEntity
import com.easycash.domain.Transaction
import com.easycash.domain.TransactionOrigin
import com.easycash.domain.TransactionStatus
import com.easycash.domain.WriteResult
import java.security.MessageDigest
import java.time.ZoneId

class NotificationProcessor(private val database: EasyCashDatabase) {
    suspend fun cleanup() {
        database.notificationDao().deleteExpired(System.currentTimeMillis())
    }
    suspend fun process(packageName: String, postedAt: Long, zone: ZoneId, readText: () -> String) {
        if (packageName != BidvNotificationParser.PACKAGE_NAME) return
        database.withTransaction {
            val dao = database.notificationDao()
            val source = dao.enabledSource(packageName) ?: return@withTransaction
            val text = readText()
            val result = BidvNotificationParser.parse(text, zone)
            if (result == BidvParseResult.Ignored) return@withTransaction
            val now = System.currentTimeMillis()
            dao.deleteExpired(now)
            val parsed = result as? BidvParseResult.Parsed
            val fingerprint = parsed?.dedupKey ?: MessageDigest.getInstance("SHA-256")
                .digest("$packageName:$postedAt:$text".toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            if (parsed != null && (dao.transactionCount(parsed.dedupKey) > 0 || dao.receiptCount(parsed.dedupKey) > 0)) return@withTransaction
            val event = dao.insertEvent(NotificationEventEntity(sourceId = source.id, postedAt = postedAt,
                fingerprint = fingerprint, classification = if (parsed == null) "UNKNOWN" else "TRANSACTION_SUCCESS",
                parseStatus = if (parsed == null) "PARSE_FAILED" else "PARSED", parserVersion = BidvNotificationParser.VERSION,
                expiresAt = now + 30L * 24 * 60 * 60 * 1000, createdAt = now))
            if (event == -1L || parsed == null) return@withTransaction
            val accountId = source.linkedAccountId ?: return@withTransaction
            val account = database.financeDao().account(accountId) ?: return@withTransaction
            if (account.isArchived) return@withTransaction
            val written = RoomFinanceRepository(database).add(Transaction(0, accountId, null, null, parsed.type,
                parsed.amount, "", parsed.occurredAtMillis, TransactionStatus.NEEDS_REVIEW, TransactionOrigin.NOTIFICATION))
            if (written is WriteResult.Success) {
                dao.setDedupKey(written.value, parsed.dedupKey)
                dao.insertReceipt(NotificationReceiptEntity(parsed.dedupKey))
            }
        }
    }
}
