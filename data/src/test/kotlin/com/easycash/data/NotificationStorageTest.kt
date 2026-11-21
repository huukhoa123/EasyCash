package com.easycash.data

import androidx.room.Room
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.local.NotificationEventEntity
import com.easycash.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class NotificationStorageTest {
    @Test
    fun `source settings persist event fingerprint is unique and expiry removes metadata`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val repository = RoomNotificationSourceRepository(db, flowOf(false))
            val source = repository.observeAll().first().single()
            assertFalse(source.isEnabled)
            assertFalse(repository.observeAccessGranted().first())
            assertEquals(WriteResult.Success(Unit), repository.setEnabled(source.id, true))
            assertEquals(WriteResult.Failure.INVALID_INPUT, repository.linkAccount(source.id, 999))
            val finance = RoomFinanceRepository(db)
            val account = (finance.add(Account(0, "Ngân hàng", AccountType.BANK, 0, 0, false)) as WriteResult.Success).value
            assertEquals(WriteResult.Success(Unit), repository.linkAccount(source.id, account))
            assertEquals(account, repository.observeAll().first().single().linkedAccountId)
            assertTrue(repository.observeAll().first().single().isEnabled)
            finance.archive(account)
            assertEquals(WriteResult.Failure.INVALID_INPUT, repository.linkAccount(source.id, account))
            assertEquals(WriteResult.Success(Unit), repository.linkAccount(source.id, null))
            assertEquals(WriteResult.Failure.NOT_FOUND, repository.setEnabled(999, true))
            val event = NotificationEventEntity(sourceId = source.id, postedAt = 1, fingerprint = "a".repeat(64),
                classification = "UNKNOWN", parseStatus = "PARSE_FAILED", parserVersion = 1, expiresAt = 10, createdAt = 1)
            assertTrue(db.notificationDao().insertEvent(event) > 0)
            assertEquals(-1L, db.notificationDao().insertEvent(event))
            assertEquals(1, db.notificationDao().events().size)
            assertEquals(0, db.notificationDao().deleteExpired(9))
            assertEquals(1, db.notificationDao().deleteExpired(10))
            assertTrue(db.notificationDao().events().isEmpty())
        } finally {
            db.close()
        }
    }
}
