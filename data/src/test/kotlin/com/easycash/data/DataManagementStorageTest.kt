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
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class DataManagementStorageTest {
    @Test
    fun `encrypted backup restores on clean install and wrong password never changes data`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val source = Room.inMemoryDatabaseBuilder(context, EasyCashDatabase::class.java).build()
        val target = Room.inMemoryDatabaseBuilder(context, EasyCashDatabase::class.java).build()
        val password = "correct horse battery".toCharArray()
        try {
            val finance = RoomFinanceRepository(source)
            val account = (finance.add(Account(0, "Tiền mặt", AccountType.CASH, 1000, 1000, false)) as WriteResult.Success).value
            finance.add(Transaction(0, account, null, null, TransactionType.EXPENSE, 100,
                "=SUM(A1)\nGhi chú, có dấu \"Việt\"", 1, TransactionStatus.USER_CONFIRMED, TransactionOrigin.MANUAL))
            val data = RoomDataManagementRepository(source)
            val backup = (data.exportBackup(password) as WriteResult.Success).value
            assertFalse(backup.toString(Charsets.UTF_8).contains("Tiền mặt"))
            val restored = RoomDataManagementRepository(target)
            assertEquals(WriteResult.Failure.INVALID_INPUT, restored.restoreBackup(backup, "wrong password".toCharArray()))
            assertTrue(RoomFinanceRepository(target).observeAll().first().isEmpty())
            assertEquals(WriteResult.Success(Unit), restored.restoreBackup(backup, password))
            assertEquals(900L, RoomFinanceRepository(target).observeAll().first().single().estimatedBalance)
            val oldPlain = BackupCrypto.decrypt(backup, password)
            val oldSnapshot = try { JSONObject(oldPlain.toString(Charsets.UTF_8)) } finally { oldPlain.fill(0) }
            oldSnapshot.put("schema", 5)
            oldSnapshot.remove("recurring_transactions")
            oldSnapshot.remove("alert_receipts")
            val oldBackup = BackupCrypto.encrypt(oldSnapshot.toString().toByteArray(Charsets.UTF_8), password)
            assertEquals(WriteResult.Success(Unit), restored.restoreBackup(oldBackup, password))
            assertEquals(900L, RoomFinanceRepository(target).observeAll().first().single().estimatedBalance)
            for (invalidTransaction in listOf("zero-amount", "missing-account")) {
                val clear = BackupCrypto.decrypt(backup, password)
                val snapshot = try { JSONObject(clear.toString(Charsets.UTF_8)) } finally { clear.fill(0) }
                val row = snapshot.getJSONArray("transactions").getJSONObject(0)
                if (invalidTransaction == "zero-amount") row.put("amount", 0) else row.put("account_id", 999)
                val invalidBackup = BackupCrypto.encrypt(snapshot.toString().toByteArray(Charsets.UTF_8), password)
                assertEquals(WriteResult.Failure.INVALID_INPUT, restored.restoreBackup(invalidBackup, password))
                assertEquals(900L, RoomFinanceRepository(target).observeAll().first().single().estimatedBalance)
            }
            val tampered = backup.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            assertEquals(WriteResult.Failure.INVALID_INPUT, restored.restoreBackup(tampered, password))
            assertEquals(900L, RoomFinanceRepository(target).observeAll().first().single().estimatedBalance)
            val csv = (restored.exportCsv() as WriteResult.Success).value
            assertTrue(csv.contains("'=SUM(A1)"))
            assertTrue(csv.contains("\"\"Việt\"\""))
            assertEquals(WriteResult.Success(Unit), restored.clearAll())
            assertTrue(RoomFinanceRepository(target).observeAll().first().isEmpty())
            assertTrue(RoomFinanceRepository(target).observe(TransactionFilter()).first().isEmpty())
            assertEquals(4, RoomCategoryRepository(target).observeAll(CategoryType.EXPENSE).first().size)
        } finally {
            password.fill('\u0000')
            source.close()
            target.close()
        }
    }
}
