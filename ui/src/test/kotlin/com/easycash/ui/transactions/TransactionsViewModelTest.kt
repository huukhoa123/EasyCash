package com.easycash.ui.transactions

import com.easycash.domain.Account
import com.easycash.domain.AccountType
import com.easycash.domain.Transaction
import com.easycash.domain.TransactionOrigin
import com.easycash.domain.TransactionStatus
import com.easycash.domain.TransactionType
import com.easycash.ui.fake.FakeAccountRepository
import com.easycash.ui.fake.FakeCategoryRepository
import com.easycash.ui.fake.FakeTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel filters by type and adds transaction correctly`() = runTest {
        val txRepo = FakeTransactionRepository(emptyList())
        val accRepo = FakeAccountRepository(
            listOf(Account(1L, "BIDV", AccountType.BANK, 1_000_000L, 1_000_000L, false))
        )
        val catRepo = FakeCategoryRepository(emptyList())

        val viewModel = TransactionsViewModel(txRepo, accRepo, catRepo)

        viewModel.addTransaction(
            accountId = 1L,
            toAccountId = null,
            categoryId = null,
            type = TransactionType.EXPENSE,
            amount = 50_000L,
            note = "Ăn tối",
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.filterIsInstance<TransactionsUiState.Success>().first()
        assertEquals(1, state.transactions.size)
        assertEquals("Ăn tối", state.transactions[0].note)
        assertEquals(50_000L, state.transactions[0].amount)

        // Filter by INCOME should return 0 items
        viewModel.setSelectedType(TransactionType.INCOME)
        testDispatcher.scheduler.advanceUntilIdle()

        val filteredState = viewModel.uiState.filterIsInstance<TransactionsUiState.Success>().first { it.selectedType == TransactionType.INCOME }
        assertEquals(0, filteredState.transactions.size)
    }

    @Test
    fun `viewModel adds transfer transaction correctly`() = runTest {
        val txRepo = FakeTransactionRepository(emptyList())
        val accRepo = FakeAccountRepository(
            listOf(
                Account(1L, "BIDV", AccountType.BANK, 2_000_000L, 2_000_000L, false),
                Account(2L, "Ví MoMo", AccountType.EWALLET, 500_000L, 500_000L, false),
            )
        )
        val catRepo = FakeCategoryRepository(emptyList())
        val viewModel = TransactionsViewModel(txRepo, accRepo, catRepo)

        viewModel.addTransaction(
            accountId = 1L,
            toAccountId = 2L,
            categoryId = null,
            type = TransactionType.TRANSFER,
            amount = 100_000L,
            note = "Nạp ví MoMo từ BIDV",
        )

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.filterIsInstance<TransactionsUiState.Success>().first()
        assertEquals(1, state.transactions.size)
        val tx = state.transactions[0]
        assertEquals(TransactionType.TRANSFER, tx.type)
        assertEquals(1L, tx.accountId)
        assertEquals(2L, tx.toAccountId)
        assertEquals(100_000L, tx.amount)
    }

    @Test
    fun `viewModel deletes and restores transaction correctly`() = runTest {
        val txRepo = FakeTransactionRepository(emptyList())
        val accRepo = FakeAccountRepository(
            listOf(Account(1L, "Tiền mặt", AccountType.CASH, 1_000_000L, 1_000_000L, false))
        )
        val catRepo = FakeCategoryRepository(emptyList())
        val viewModel = TransactionsViewModel(txRepo, accRepo, catRepo)

        viewModel.addTransaction(1L, null, null, TransactionType.EXPENSE, 25_000L, "Bánh mì")
        testDispatcher.scheduler.advanceUntilIdle()

        val state1 = viewModel.uiState.filterIsInstance<TransactionsUiState.Success>().first()
        val tx = state1.transactions[0]

        // Delete
        viewModel.deleteTransaction(tx.id)
        testDispatcher.scheduler.advanceUntilIdle()
        val state2 = viewModel.uiState.filterIsInstance<TransactionsUiState.Success>().first { it.transactions.isEmpty() }
        assertEquals(0, state2.transactions.size)

        // Restore
        viewModel.restoreTransaction(tx)
        testDispatcher.scheduler.advanceUntilIdle()
        val state3 = viewModel.uiState.filterIsInstance<TransactionsUiState.Success>().first { it.transactions.isNotEmpty() }
        assertEquals(1, state3.transactions.size)
        assertEquals("Bánh mì", state3.transactions[0].note)
    }

    @Test
    fun `viewModel confirms and rejects transaction correctly`() = runTest {
        val initialTx = Transaction(
            id = 1L,
            accountId = 1L,
            toAccountId = null,
            categoryId = null,
            type = TransactionType.EXPENSE,
            amount = 10_000L,
            note = "Trà đá",
            occurredAtMillis = 1000L,
            status = TransactionStatus.AUTO_RECORDED,
            origin = TransactionOrigin.NOTIFICATION,
        )
        val txRepo = FakeTransactionRepository(listOf(initialTx))
        val accRepo = FakeAccountRepository(emptyList())
        val catRepo = FakeCategoryRepository(emptyList())
        val viewModel = TransactionsViewModel(txRepo, accRepo, catRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        // Confirm
        viewModel.confirmTransaction(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TransactionStatus.USER_CONFIRMED, txRepo.observeById(1L).first()?.status)

        // Reject
        viewModel.rejectTransaction(1L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(TransactionStatus.REJECTED, txRepo.observeById(1L).first()?.status)
    }

    @Test
    fun `viewModel filters by status correctly`() = runTest {
        val tx1 = Transaction(1L, 1L, null, null, TransactionType.EXPENSE, 10_000L, "Auto", 1000L, TransactionStatus.NEEDS_REVIEW, TransactionOrigin.NOTIFICATION)
        val tx2 = Transaction(2L, 1L, null, null, TransactionType.EXPENSE, 20_000L, "Manual", 1000L, TransactionStatus.USER_CONFIRMED, TransactionOrigin.MANUAL)
        val txRepo = FakeTransactionRepository(listOf(tx1, tx2))
        val accRepo = FakeAccountRepository(emptyList())
        val catRepo = FakeCategoryRepository(emptyList())
        val viewModel = TransactionsViewModel(txRepo, accRepo, catRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setSelectedStatus(TransactionStatus.NEEDS_REVIEW)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.filterIsInstance<TransactionsUiState.Success>().first { it.selectedStatus == TransactionStatus.NEEDS_REVIEW }
        assertEquals(1, state.transactions.size)
        assertEquals(1L, state.transactions[0].id)
    }

    @Test
    fun `viewModel updates transaction correctly`() = runTest {
        val initialTx = Transaction(
            id = 1L,
            accountId = 1L,
            toAccountId = null,
            categoryId = null,
            type = TransactionType.EXPENSE,
            amount = 10_000L,
            note = "Ăn vặt",
            occurredAtMillis = 1000L,
            status = TransactionStatus.NEEDS_REVIEW,
            origin = TransactionOrigin.NOTIFICATION,
        )
        val txRepo = FakeTransactionRepository(listOf(initialTx))
        val accRepo = FakeAccountRepository(emptyList())
        val catRepo = FakeCategoryRepository(emptyList())
        val viewModel = TransactionsViewModel(txRepo, accRepo, catRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateTransaction(
            initialTx.copy(
                amount = 20_000L,
                note = "Ăn vặt cùng bạn",
                status = TransactionStatus.USER_CONFIRMED,
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = txRepo.observeById(1L).first()
        assertEquals(20_000L, updated?.amount)
        assertEquals("Ăn vặt cùng bạn", updated?.note)
        assertEquals(TransactionStatus.USER_CONFIRMED, updated?.status)
    }
}
