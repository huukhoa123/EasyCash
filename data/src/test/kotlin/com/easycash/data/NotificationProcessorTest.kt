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
    fun `push without reference is recorded once and replay stays deduplicated after cleanup`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val sources = RoomNotificationSourceRepository(db, flowOf(true))
            val source = sources.observeAll().first().single()
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
    fun `whitelist otp concurrent dedup and review preserve privacy and balances`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val processor = NotificationProcessor(db)
            val zone = ZoneId.of("Asia/Ho_Chi_Minh")
            val packageName = BidvNotificationParser.PACKAGE_NAME
            processor.process("untrusted", 1, zone) { error("Must not read untrusted content") }
            val sources = RoomNotificationSourceRepository(db, flowOf(true))
            val source = sources.observeAll().first().single()
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
