package com.easycash.ui.fake

import com.easycash.domain.Account
import com.easycash.domain.AccountType
import com.easycash.domain.Category
import com.easycash.domain.CategoryType
import com.easycash.domain.Transaction
import com.easycash.domain.TransactionOrigin
import com.easycash.domain.TransactionStatus
import com.easycash.domain.TransactionType
import com.easycash.domain.WriteResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeRepositoriesTest {
    @Test
    fun `FakeAccountRepository performs CRUD correctly`() = runBlocking {
        val repo = FakeAccountRepository(emptyList())
        val addResult = repo.add(
            Account(0L, "Ví tiền", AccountType.CASH, 100_000L, 100_000L, false)
        )
        assertTrue(addResult is WriteResult.Success)
        val id = (addResult as WriteResult.Success).value

        val accounts = repo.observeAll().first()
        assertEquals(1, accounts.size)
        assertEquals("Ví tiền", accounts[0].name)

        repo.archive(id)
        val afterArchive = repo.observeAll().first()
        assertEquals(0, afterArchive.size)
    }

    @Test
    fun `FakeTransactionRepository performs filter and status change correctly`() = runBlocking {
        val repo = FakeTransactionRepository(emptyList())
        val addResult = repo.add(
            Transaction(
                id = 0L,
                accountId = 1L,
                toAccountId = null,
                categoryId = 1L,
                type = TransactionType.EXPENSE,
                amount = 25_000L,
                note = "Cà phê",
                occurredAtMillis = 1000L,
                status = TransactionStatus.AUTO_RECORDED,
                origin = TransactionOrigin.NOTIFICATION,
            )
        )
        val txId = (addResult as WriteResult.Success).value
        assertEquals(1, repo.observeNeedsReviewCount().first())

        repo.confirm(txId)
        val updatedTx = repo.observeById(txId).first()
        assertEquals(TransactionStatus.USER_CONFIRMED, updatedTx?.status)
        assertEquals(0, repo.observeNeedsReviewCount().first())
    }
}
