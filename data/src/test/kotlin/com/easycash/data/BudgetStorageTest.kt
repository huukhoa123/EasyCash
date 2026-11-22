package com.easycash.data

import androidx.room.Room
import com.easycash.data.local.EasyCashDatabase
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
class BudgetStorageTest {
    @Test
    fun `budget remains after database reopen`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "budget-persistence-test.db"
        context.deleteDatabase(name)
        try {
            val first = Room.databaseBuilder(context, EasyCashDatabase::class.java, name).build()
            try {
                val category = (RoomCategoryRepository(first).add(Category(0, "Ăn uống", CategoryType.EXPENSE, "food", "orange", false)) as WriteResult.Success).value
                assertTrue(RoomBudgetRepository(first).upsert(Budget(0, category, 202611, 500000, 80)) is WriteResult.Success)
            } finally {
                first.close()
            }
            val reopened = Room.databaseBuilder(context, EasyCashDatabase::class.java, name).build()
            try {
                assertEquals(500000L, RoomBudgetRepository(reopened).observeUsage(202611).first().single().budget.limitAmount)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun `budget usage respects month boundary and confirmed status and upserts once`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val categories = RoomCategoryRepository(db)
            val categoryId = (categories.add(Category(0, "Ăn uống", CategoryType.EXPENSE, "food", "orange", false)) as WriteResult.Success).value
            val finance = RoomFinanceRepository(db)
            val account = (finance.add(Account(0, "Tiền mặt", AccountType.CASH, 1000, 1000, false)) as WriteResult.Success).value
            val budgets = RoomBudgetRepository(db, ZoneId.of("Asia/Ho_Chi_Minh"))
            val budget = Budget(0, categoryId, 202611, 500, 80)
            val id = (budgets.upsert(budget) as WriteResult.Success).value
            assertEquals(WriteResult.Failure.CONFLICT, categories.update(Category(categoryId, "Ăn uống", CategoryType.INCOME, "food", "orange", false)))
            assertEquals(WriteResult.Success(id), budgets.upsert(budget.copy(limitAmount = 600)))
            val transaction = Transaction(0, account, null, categoryId, TransactionType.EXPENSE, 100, "",
                Instant.parse("2026-10-31T17:00:00Z").toEpochMilli(), TransactionStatus.USER_CONFIRMED, TransactionOrigin.MANUAL)
            assertTrue(finance.add(transaction) is WriteResult.Success)
            finance.add(transaction.copy(occurredAtMillis = transaction.occurredAtMillis - 1))
            finance.add(transaction.copy(occurredAtMillis = Instant.parse("2026-11-30T17:00:00Z").toEpochMilli()))
            finance.add(transaction.copy(status = TransactionStatus.NEEDS_REVIEW, origin = TransactionOrigin.NOTIFICATION))
            val usage = budgets.observeUsage(202611).first().single()
            assertEquals(100L, usage.spent)
            assertEquals(600L, usage.budget.limitAmount)
            assertEquals(WriteResult.Failure.INVALID_INPUT, budgets.upsert(budget.copy(month = 202613)))
            assertEquals(WriteResult.Failure.INVALID_INPUT, budgets.upsert(budget.copy(limitAmount = 0)))
            assertEquals(WriteResult.Success(Unit), budgets.delete(id))
            assertTrue(budgets.observeUsage(202611).first().isEmpty())
        } finally {
            db.close()
        }
    }
}
