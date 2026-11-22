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

enum class NotificationOutcome { IGNORED, SOURCE_DISABLED, UNKNOWN_FORMAT, ALREADY_RECORDED, NO_ACCOUNT, ACCOUNT_UNAVAILABLE, WRITE_FAILED, RECORDED }

class NotificationProcessor(private val database: EasyCashDatabase) {
    suspend fun cleanup() {
        database.notificationDao().deleteExpired(System.currentTimeMillis())
    }
    suspend fun process(packageName: String, postedAt: Long, zone: ZoneId, readText: () -> String): NotificationOutcome {
        if (!SupportedNotificationSources.supports(packageName)) return NotificationOutcome.IGNORED
        return database.withTransaction {
            val dao = database.notificationDao()
            val source = dao.enabledSource(packageName) ?: return@withTransaction NotificationOutcome.SOURCE_DISABLED
            val text = readText()
            val result = if (packageName == TimoNotificationParser.PACKAGE_NAME) TimoNotificationParser.parse(text) else BidvNotificationParser.parse(text, zone)
            val parserVersion = if (packageName == TimoNotificationParser.PACKAGE_NAME) TimoNotificationParser.VERSION else BidvNotificationParser.VERSION
            if (result == BidvParseResult.Ignored) return@withTransaction NotificationOutcome.IGNORED
            val now = System.currentTimeMillis()
            dao.deleteExpired(now)
            val parsed = result as? BidvParseResult.Parsed
            val fingerprint = parsed?.dedupKey ?: MessageDigest.getInstance("SHA-256")
                .digest("$packageName:$postedAt:$text".toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            if (parsed != null && (dao.transactionCount(parsed.dedupKey) > 0 || dao.receiptCount(parsed.dedupKey) > 0)) return@withTransaction NotificationOutcome.ALREADY_RECORDED
            dao.insertEvent(NotificationEventEntity(sourceId = source.id, postedAt = postedAt,
                fingerprint = fingerprint, classification = if (parsed == null) "UNKNOWN" else "TRANSACTION_SUCCESS",
                parseStatus = if (parsed == null) "PARSE_FAILED" else "PARSED", parserVersion = parserVersion,
                expiresAt = now + 30L * 24 * 60 * 60 * 1000, createdAt = now))
            // An existing event may have arrived before account linking; only receipts block replay.
            if (parsed == null) return@withTransaction NotificationOutcome.UNKNOWN_FORMAT
            val accountId = source.linkedAccountId ?: return@withTransaction NotificationOutcome.NO_ACCOUNT
            val account = database.financeDao().account(accountId) ?: return@withTransaction NotificationOutcome.ACCOUNT_UNAVAILABLE
            if (account.isArchived) return@withTransaction NotificationOutcome.ACCOUNT_UNAVAILABLE
            val written = RoomFinanceRepository(database).add(Transaction(0, accountId, null, null, parsed.type,
                parsed.amount, "", parsed.occurredAtMillis, TransactionStatus.NEEDS_REVIEW, TransactionOrigin.NOTIFICATION))
            if (written is WriteResult.Success) {
                dao.setDedupKey(written.value, parsed.dedupKey)
                dao.insertReceipt(NotificationReceiptEntity(parsed.dedupKey))
                NotificationOutcome.RECORDED
            } else {
                NotificationOutcome.WRITE_FAILED
            }
        }
    }
}
