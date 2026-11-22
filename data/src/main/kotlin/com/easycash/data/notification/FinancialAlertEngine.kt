package com.easycash.data.notification

import androidx.room.withTransaction
import com.easycash.data.RoomBudgetRepository
import com.easycash.data.local.AlertReceiptEntity
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.local.ReminderEntity
import kotlinx.coroutines.flow.first
import java.math.BigInteger
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class FinancialAlertEngine(private val database: EasyCashDatabase) {
    suspend fun check(now: Long, zone: ZoneId, publish: (String, String, String) -> Boolean) {
        val date = Instant.ofEpochMilli(now).atZone(zone)
        val month = date.year * 100 + date.monthValue
        val usage = RoomBudgetRepository(database, zone).observeUsage(month).first()
        val dao = database.reminderDao()
        for (item in usage) {
            val spent = BigInteger.valueOf(item.spent)
            val limit = BigInteger.valueOf(item.budget.limitAmount)
            val level = when {
                spent >= limit -> "exceeded"
                spent * BigInteger.valueOf(100) >= limit * BigInteger.valueOf(item.budget.warnThresholdPercent.toLong()) -> "warning"
                else -> null
            } ?: continue
            database.withTransaction {
                val key = "budget:$month:${item.budget.id}:$level"
                if (dao.receiptCount(key) == 0 && publish(key,
                        if (level == "exceeded") "Ngân sách đã chạm hạn mức" else "Ngân sách gần chạm hạn mức",
                        "Mở EasyCash để xem chi tiết ngân sách tháng này.")) dao.record(AlertReceiptEntity(key))
            }
        }
        for (reminder in dao.active()) {
            val due = Instant.ofEpochMilli(reminder.nextDueAt).atZone(zone)
            if (date.isBefore(due.minusDays(reminder.remindBeforeDays.toLong()))) continue
            database.withTransaction {
                val current = dao.find(reminder.id) ?: return@withTransaction
                if (!current.isActive || current.nextDueAt != reminder.nextDueAt) return@withTransaction
                if (database.financeDao().account(current.accountId)?.isArchived != false) return@withTransaction
                val key = "reminder:${current.id}:${current.nextDueAt}"
                if (dao.receiptCount(key) == 0 && !publish(key, "Bạn có khoản định kỳ sắp đến hạn", "Mở EasyCash để xem và tự kiểm tra khoản cần thanh toán.")) return@withTransaction
                dao.record(AlertReceiptEntity(key))
                dao.update(current.copy(nextDueAt = nextOccurrence(current, now, zone), updatedAt = now))
            }
        }
        database.notificationDao().deleteExpired(now)
    }

    internal fun nextOccurrence(reminder: ReminderEntity, now: Long, zone: ZoneId): Long {
        val previous = Instant.ofEpochMilli(reminder.nextDueAt).atZone(zone)
        val current = Instant.ofEpochMilli(now).atZone(zone)
        val steps = when (reminder.frequency) {
            "WEEKLY" -> ChronoUnit.WEEKS.between(previous, current).coerceAtLeast(0) + 1
            "MONTHLY" -> ChronoUnit.MONTHS.between(YearMonth.from(previous), YearMonth.from(current)).coerceAtLeast(1)
            "YEARLY" -> (current.year - previous.year).toLong().coerceAtLeast(1)
            else -> throw IllegalArgumentException("Unknown reminder frequency")
        }
        fun occurrence(count: Long): java.time.ZonedDateTime {
            val next = when (reminder.frequency) {
                "WEEKLY" -> previous.plusWeeks(count)
                "MONTHLY" -> previous.plusMonths(count)
                else -> previous.plusYears(count)
            }
            return if (reminder.frequency == "WEEKLY") next else next.withDayOfMonth(minOf(reminder.dayRule, next.toLocalDate().lengthOfMonth()))
        }
        var next = occurrence(steps)
        if (!next.isAfter(current)) next = occurrence(steps + 1)
        return next.toInstant().toEpochMilli()
    }
}
