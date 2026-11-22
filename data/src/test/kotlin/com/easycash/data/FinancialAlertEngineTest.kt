package com.easycash.data

import androidx.room.Room
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.local.ReminderEntity
import com.easycash.data.notification.FinancialAlertEngine
import com.easycash.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class FinancialAlertEngineTest {
    @Test
    fun `denied notification is retried and budget levels do not repeat`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val zone = ZoneId.of("UTC")
            val now = Instant.parse("2026-11-22T10:00:00Z").toEpochMilli()
            val finance = RoomFinanceRepository(db)
            val account = (finance.add(Account(0, "Cash", AccountType.CASH, 1000, 1000, false)) as WriteResult.Success).value
            val category = (RoomCategoryRepository(db).add(Category(0, "Food", CategoryType.EXPENSE, "food", "orange", false)) as WriteResult.Success).value
            RoomBudgetRepository(db, zone).upsert(Budget(0, category, 202611, 100, 80))
            val transaction = Transaction(0, account, null, category, TransactionType.EXPENSE, 80, "", now,
                TransactionStatus.USER_CONFIRMED, TransactionOrigin.MANUAL)
            finance.add(transaction)
            val engine = FinancialAlertEngine(db)
            engine.check(now, zone) { _, _, _ -> false }
            val sent = mutableListOf<String>()
            engine.check(now, zone) { key, _, _ -> sent.add(key); true }
            engine.check(now, zone) { key, _, _ -> sent.add(key); true }
            assertEquals(1, sent.size)
            assertTrue(sent.single().endsWith("warning"))
            finance.add(transaction.copy(amount = 20))
            engine.check(now, zone) { key, _, _ -> sent.add(key); true }
            assertEquals(2, sent.size)
            assertTrue(sent.last().endsWith("exceeded"))
        } finally { db.close() }
    }

    @Test
    fun `reminder keeps month end anchor and never writes a transaction`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val zone = ZoneId.of("UTC")
            val finance = RoomFinanceRepository(db)
            val account = (finance.add(Account(0, "Cash", AccountType.CASH, 1000, 1000, false)) as WriteResult.Success).value
            val due = Instant.parse("2026-01-31T09:00:00Z").toEpochMilli()
            val reminder = ReminderEntity(name = "Monthly bill", type = "EXPENSE", amount = 100, accountId = account,
                categoryId = null, frequency = "MONTHLY", dayRule = 31, nextDueAt = due, remindBeforeDays = 1,
                isActive = true, createdAt = due, updatedAt = due)
            val reminders = RoomReminderRepository(db)
            val id = (reminders.upsert(reminder.toDomain()) as WriteResult.Success).value
            assertEquals("Monthly bill", reminders.observeAll().first().single().name)
            assertThrows(IllegalArgumentException::class.java) { reminder.copy(type = "INVALID").toDomain() }
            assertThrows(IllegalArgumentException::class.java) { reminder.copy(frequency = "INVALID").toDomain() }
            assertEquals(WriteResult.Failure.INVALID_INPUT, reminders.upsert(reminder.copy(accountId = 999)))
            assertEquals(WriteResult.Failure.INVALID_INPUT, reminders.upsert(reminder.copy(amount = -1)))
            assertEquals(WriteResult.Failure.INVALID_INPUT, reminders.upsert(reminder.copy(frequency = "DAILY")))
            val engine = FinancialAlertEngine(db)
            engine.check(due, zone) { _, _, _ -> false }
            assertEquals(due, db.reminderDao().find(id)!!.nextDueAt)
            engine.check(due, zone) { _, _, _ -> true }
            val february = db.reminderDao().find(id)!!
            assertEquals(Instant.parse("2026-02-28T09:00:00Z").toEpochMilli(), february.nextDueAt)
            assertEquals(Instant.parse("2026-03-31T09:00:00Z").toEpochMilli(), engine.nextOccurrence(february, february.nextDueAt, zone))
            assertTrue(finance.observe(TransactionFilter()).first().isEmpty())
            assertEquals(1000L, finance.observeAll().first().single().estimatedBalance)
        } finally { db.close() }
    }
}
