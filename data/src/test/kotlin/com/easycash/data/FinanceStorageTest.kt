package com.easycash.data

import androidx.room.Room
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.local.MIGRATION_1_2
import com.easycash.data.local.MIGRATION_2_3
import com.easycash.data.local.MIGRATION_3_4
import com.easycash.data.local.MIGRATION_4_5
import com.easycash.data.local.MIGRATION_5_6
import com.easycash.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class FinanceStorageTest {
    @Test
    fun `migration from original trial schema preserves transactions and balances`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "original-trial-migration.db"
        context.deleteDatabase(name)
        try {
            val schema = JSONObject(java.io.File(System.getProperty("easycash.schemaRoot"), "com.easycash.data.local.EasyCashDatabase/4.json").readText())
                .getJSONObject("database").getJSONArray("entities")
            context.openOrCreateDatabase(name, 0, null).use { old ->
                for (index in 0 until schema.length()) {
                    val entity = schema.getJSONObject(index)
                    val table = entity.getString("tableName")
                    old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                    for (position in 0 until indices.length()) old.execSQL(indices.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", table))
                }
                old.execSQL("INSERT INTO accounts (id, name, type, initial_balance, is_archived, created_at, updated_at) VALUES (1, 'Legacy cash', 'CASH', 1000, 0, 1, 1)")
                old.execSQL("INSERT INTO transactions (id, account_id, type, amount, note, occurred_at, status, origin, created_at, updated_at) VALUES (1, 1, 'EXPENSE', 200, 'Legacy purchase', 1, 'USER_CONFIRMED', 'MANUAL', 1, 1)")
                old.version = 4
            }
            val db = Room.databaseBuilder(context, EasyCashDatabase::class.java, name).addMigrations(MIGRATION_4_5, MIGRATION_5_6).build()
            try {
                val repository = RoomFinanceRepository(db)
                assertEquals(800L, repository.observeAll().first().single().estimatedBalance)
                assertEquals("Legacy purchase", repository.observe(TransactionFilter()).first().single().note)
                assertTrue(db.budgetDao().observe(202611).first().isEmpty())
                assertTrue(db.reminderDao().observeAll().first().isEmpty())
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test
    fun `balances respect status transfers refunds and updates`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val repository = RoomFinanceRepository(db)
            val source = (repository.add(Account(0, "Tiền mặt", AccountType.CASH, 1000, 1000, false)) as WriteResult.Success).value
            val target = (repository.add(Account(0, "Ngân hàng", AccountType.BANK, 0, 0, false)) as WriteResult.Success).value
            val transfer = Transaction(0, source, target, null, TransactionType.TRANSFER, 300, "Chuyển", 10,
                TransactionStatus.USER_CONFIRMED, TransactionOrigin.MANUAL)
            val id = (repository.add(transfer) as WriteResult.Success).value
            assertEquals(listOf(700L, 300L), repository.observeAll().first().map { it.estimatedBalance })
            assertEquals(WriteResult.Failure.INVALID_INPUT, repository.add(transfer.copy(toAccountId = source)))
            val review = transfer.copy(type = TransactionType.EXPENSE, toAccountId = null, amount = 50,
                status = TransactionStatus.NEEDS_REVIEW, origin = TransactionOrigin.NOTIFICATION)
            val reviewId = (repository.add(review) as WriteResult.Success).value
            assertEquals(700L, repository.observeAll().first().first().estimatedBalance)
            repository.confirm(reviewId)
            assertEquals(650L, repository.observeAll().first().first().estimatedBalance)
            repository.reject(reviewId)
            repository.add(transfer.copy(type = TransactionType.REFUND, toAccountId = null, amount = 20))
            repository.add(transfer.copy(type = TransactionType.ADJUSTMENT, toAccountId = null, amount = -10))
            assertEquals(710L, repository.observeAll().first().first().estimatedBalance)
            repository.update(transfer.copy(id = id, amount = 200))
            assertEquals(listOf(810L, 200L), repository.observeAll().first().map { it.estimatedBalance })
            assertEquals(1, repository.observe(TransactionFilter(accountId = target)).first().size)
            repository.delete(id)
            assertEquals(listOf(1010L, 0L), repository.observeAll().first().map { it.estimatedBalance })
            repository.archive(source)
            assertEquals(WriteResult.Failure.INVALID_INPUT, repository.add(transfer))
            val huge = (repository.add(Account(0, "Giới hạn", AccountType.CASH, Long.MAX_VALUE, 0, false)) as WriteResult.Success).value
            assertEquals(WriteResult.Failure.INVALID_INPUT, repository.add(transfer.copy(
                accountId = huge, toAccountId = null, type = TransactionType.INCOME, amount = 1,
            )))
            assertEquals(Long.MAX_VALUE, repository.observeAll().first().last().estimatedBalance)
            assertEquals(WriteResult.Failure.INVALID_INPUT, repository.add(Account(0, "Vượt tổng", AccountType.CASH, 1, 1, false)))
            assertEquals(2, repository.observeAll().first().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `migration preserves v1 categories and validates new schema`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "finance-migration-test.db"
        context.deleteDatabase(name)
        try {
            context.openOrCreateDatabase(name, 0, null).use { old ->
                old.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, icon_key TEXT NOT NULL, color_key TEXT NOT NULL, is_system INTEGER NOT NULL, is_archived INTEGER NOT NULL, sort_order INTEGER NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
                old.execSQL("CREATE UNIQUE INDEX index_categories_name_type ON categories(name, type)")
                old.execSQL("INSERT INTO categories VALUES (1, 'Giữ lại', 'EXPENSE', 'food', 'expense', 0, 0, 0, 1, 1)")
                old.version = 1
            }
            val db = Room.databaseBuilder(context, EasyCashDatabase::class.java, name).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
            try {
                assertEquals("Giữ lại", db.categoryDao().findById(1)?.name)
                assertTrue(RoomFinanceRepository(db).observeAll().first().isEmpty())
            } finally {
                db.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
