package com.easycash.data

import androidx.room.Room
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.notification.*
import com.easycash.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class NotificationProcessorTest {
    @Test
    fun `Timo whitelist credit dedup and source isolation`() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val sources = RoomNotificationSourceRepository(db, flowOf(true))
            val source = sources.observeAll().first().single { it.packageName == TimoNotificationParser.PACKAGE_NAME }
            assertFalse(source.isEnabled)
            val processor = NotificationProcessor(db)
            val zone = ZoneId.of("UTC")
            assertEquals(NotificationOutcome.SOURCE_DISABLED, processor.process(source.packageName, 1, zone) { error("Disabled source must not read text") })
            val finance = RoomFinanceRepository(db)
            val accountId = (finance.add(Account(0, "Timo", AccountType.BANK, 0, 0, false)) as WriteResult.Success).value
            assertEquals(WriteResult.Success(Unit), sources.setEnabled(source.id, true))
            sources.linkAccount(source.id, accountId)
            val fixture = javaClass.getResource("/notifications/timo/credit.txt")!!.readText()
            assertEquals(NotificationOutcome.RECORDED, processor.process(source.packageName, 1, zone) { fixture })
            assertEquals(NotificationOutcome.ALREADY_RECORDED, processor.process(source.packageName, 2, zone) { fixture })
            assertEquals(NotificationOutcome.UNKNOWN_FORMAT, processor.process(source.packageName, 3, zone) { javaClass.getResource("/notifications/bidv/debit.txt")!!.readText() })
            val transaction = finance.observe(TransactionFilter()).first().single()
            assertEquals(TransactionType.INCOME, transaction.type)
            assertEquals(10000L, transaction.amount)
            assertEquals(0L, finance.observeAll().first().single().estimatedBalance)
            finance.confirm(transaction.id)
            assertEquals(10000L, finance.observeAll().first().single().estimatedBalance)
            val backup = RoomDataManagementRepository(db)
            val password = "test-password-only".toCharArray()
            try {
                val exported = (backup.exportBackup(password) as WriteResult.Success).value
                assertEquals(WriteResult.Success(Unit), backup.restoreBackup(exported, password))
                val restored = sources.observeAll().first().single { it.packageName == TimoNotificationParser.PACKAGE_NAME }
                assertFalse(restored.isEnabled)
                assertEquals(accountId, restored.linkedAccountId)
            } finally { password.fill('\u0000') }
            backup.clearAll()
            val reset = sources.observeAll().first()
            assertEquals(2, reset.size)
            assertTrue(reset.none { it.isEnabled || it.linkedAccountId != null })
        } finally { db.close() }
    }

    @Test
    fun `notification seen before account linking can be replayed after linking`() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val sources = RoomNotificationSourceRepository(db, flowOf(true))
            val source = sources.observeAll().first().single { it.packageName == BidvNotificationParser.PACKAGE_NAME }
            sources.setEnabled(source.id, true)
            val processor = NotificationProcessor(db)
            val push = javaClass.getResource("/notifications/bidv/push-debit.txt")!!.readText()
            val zone = ZoneId.of("Asia/Ho_Chi_Minh")
            assertEquals(NotificationOutcome.NO_ACCOUNT, processor.process(BidvNotificationParser.PACKAGE_NAME, 1, zone) { push })
            val finance = RoomFinanceRepository(db)
            assertTrue(finance.observe(TransactionFilter()).first().isEmpty())
            val accountId = (finance.add(Account(0, "BIDV", AccountType.BANK, 100000, 100000, false)) as WriteResult.Success).value
            sources.linkAccount(source.id, accountId)
            assertEquals(NotificationOutcome.RECORDED, processor.process(BidvNotificationParser.PACKAGE_NAME, 1, zone) { push })
            assertEquals(NotificationOutcome.ALREADY_RECORDED, processor.process(BidvNotificationParser.PACKAGE_NAME, 1, zone) { push })
            assertEquals(1, finance.observe(TransactionFilter()).first().size)
            assertEquals(1, db.notificationDao().events().size)
        } finally {
            db.close()
        }
    }

    @Test
    fun `push without reference is recorded once and replay stays deduplicated after cleanup`() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val sources = RoomNotificationSourceRepository(db, flowOf(true))
            val source = sources.observeAll().first().single { it.packageName == BidvNotificationParser.PACKAGE_NAME }
            val finance = RoomFinanceRepository(db)
            val accountId = (finance.add(Account(0, "BIDV", AccountType.BANK, 100000, 100000, false)) as WriteResult.Success).value
            sources.linkAccount(source.id, accountId)
            sources.setEnabled(source.id, true)
            val processor = NotificationProcessor(db)
            val push = javaClass.getResource("/notifications/bidv/push-debit.txt")!!.readText()
            val zone = ZoneId.of("Asia/Ho_Chi_Minh")
            repeat(2) { processor.process(BidvNotificationParser.PACKAGE_NAME, it.toLong(), zone) { push } }
            val transaction = finance.observe(TransactionFilter()).first().single()
            assertEquals(10000L, transaction.amount)
            assertEquals(TransactionStatus.NEEDS_REVIEW, transaction.status)
            assertEquals(100000L, finance.observeAll().first().single().estimatedBalance)
            db.notificationDao().deleteExpired(Long.MAX_VALUE)
            finance.delete(transaction.id)
            processor.process(BidvNotificationParser.PACKAGE_NAME, 3, zone) { push }
            assertTrue(finance.observe(TransactionFilter()).first().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `whitelist otp concurrent dedup and review preserve privacy and balances`() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val processor = NotificationProcessor(db)
            val zone = ZoneId.of("Asia/Ho_Chi_Minh")
            val packageName = BidvNotificationParser.PACKAGE_NAME
            processor.process("untrusted", 1, zone) { error("Must not read untrusted content") }
            val sources = RoomNotificationSourceRepository(db, flowOf(true))
            val source = sources.observeAll().first().single { it.packageName == BidvNotificationParser.PACKAGE_NAME }
            processor.process(packageName, 1, zone) { error("Must not read disabled content") }
            sources.setEnabled(source.id, true)
            processor.process(packageName, 1, zone) { "Mã xác thực giả 123456" }
            assertTrue(db.notificationDao().events().isEmpty())
            val finance = RoomFinanceRepository(db)
            val accountId = (finance.add(Account(0, "BIDV", AccountType.BANK, 100000, 100000, false)) as WriteResult.Success).value
            sources.linkAccount(source.id, accountId)
            val fixture = javaClass.getResource("/notifications/bidv/debit.txt")!!.readText()
            coroutineScope {
                repeat(2) { launch(Dispatchers.IO) { processor.process(packageName, 1, zone) { fixture } } }
            }
            val transaction = finance.observe(TransactionFilter()).first().single()
            assertEquals(TransactionStatus.NEEDS_REVIEW, transaction.status)
            assertEquals(100000L, finance.observeAll().first().single().estimatedBalance)
            assertEquals(1, db.notificationDao().events().size)
            finance.confirm(transaction.id)
            assertEquals(90000L, finance.observeAll().first().single().estimatedBalance)
            finance.update(transaction.copy(status = TransactionStatus.USER_CONFIRMED, note = "Đã kiểm tra"))
            db.notificationDao().deleteExpired(Long.MAX_VALUE)
            processor.process(packageName, 2, zone) { fixture }
            assertEquals(1, finance.observe(TransactionFilter()).first().size)
            assertTrue(db.notificationDao().events().isEmpty())
            finance.delete(transaction.id)
            processor.process(packageName, 3, zone) { fixture }
            assertTrue(finance.observe(TransactionFilter()).first().isEmpty())
            sources.setEnabled(source.id, false)
            processor.process(packageName, 3, zone) { error("Must not read after disabling") }
        } finally {
            db.close()
        }
    }
}
