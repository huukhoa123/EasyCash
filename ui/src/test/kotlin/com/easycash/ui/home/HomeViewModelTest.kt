package com.easycash.ui.home

import com.easycash.domain.Account
import com.easycash.domain.AccountType
import com.easycash.domain.CategorySpending
import com.easycash.domain.StatsRepository
import com.easycash.domain.Summary
import com.easycash.domain.TimeRange
import com.easycash.domain.Transaction
import com.easycash.domain.TransactionOrigin
import com.easycash.domain.TransactionStatus
import com.easycash.domain.TransactionType
import com.easycash.ui.fake.FakeAccountRepository
import com.easycash.ui.fake.FakeNotificationSourceRepository
import com.easycash.ui.fake.FakeTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeStatsRepo = object : StatsRepository {
        override fun observeSummary(range: TimeRange): Flow<Summary> =
            flowOf(Summary(income = 0L, expense = 0L))

        override fun observeSpendingByCategory(range: TimeRange): Flow<List<CategorySpending>> =
            flowOf(emptyList())
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState calculates balances and caps recent transactions at 5`() = runTest {
        val accounts = listOf(
            Account(1L, "Tiền mặt", AccountType.CASH, 2_000_000L, 2_000_000L, false),
            Account(2L, "BIDV", AccountType.BANK, 6_000_000L, 6_000_000L, false),
        )
        val txs = (1..6).map { i ->
            Transaction(
                id = i.toLong(),
                accountId = 1L,
                toAccountId = null,
                categoryId = null,
                type = if (i == 1) TransactionType.INCOME else TransactionType.EXPENSE,
                amount = if (i == 1) 1_000_000L else 100_000L,
                occurredAtMillis = 1000L * i,
                note = "Tx $i",
                origin = TransactionOrigin.MANUAL,
                status = if (i == 2) TransactionStatus.NEEDS_REVIEW else TransactionStatus.USER_CONFIRMED,
            )
        }

        val accRepo = FakeAccountRepository(accounts)
        val txRepo = FakeTransactionRepository(txs)
        val notifRepo = FakeNotificationSourceRepository(emptyList())
        val viewModel = HomeViewModel(accRepo, txRepo, fakeStatsRepo, notifRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.filterIsInstance<HomeUiState.Success>().first()
        assertEquals(8_000_000L, state.totalEstimatedBalance)
        assertEquals(1_000_000L, state.totalIncome)
        assertEquals(500_000L, state.totalExpense)
        assertEquals(5, state.recentTransactions.size)
        assertEquals(1, state.needsReviewCount)
        assertTrue(state.isAccessGranted)
    }

    @Test
    fun `toggleBalanceVisibility switches hidden state`() = runTest {
        val accRepo = FakeAccountRepository(emptyList())
        val txRepo = FakeTransactionRepository(emptyList())
        val notifRepo = FakeNotificationSourceRepository(emptyList())
        val viewModel = HomeViewModel(accRepo, txRepo, fakeStatsRepo, notifRepo)

        assertFalse(viewModel.isBalanceHidden.value)
        viewModel.toggleBalanceVisibility()
        assertTrue(viewModel.isBalanceHidden.value)
        viewModel.toggleBalanceVisibility()
        assertFalse(viewModel.isBalanceHidden.value)
    }
}
