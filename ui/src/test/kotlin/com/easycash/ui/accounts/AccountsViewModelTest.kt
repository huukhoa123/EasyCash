package com.easycash.ui.accounts

import com.easycash.domain.Account
import com.easycash.domain.AccountType
import com.easycash.ui.fake.FakeAccountRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

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
    fun `viewModel loads accounts and calculates total estimated balance`() = runTest {
        val initialAccounts = listOf(
            Account(1L, "Tiền mặt", AccountType.CASH, 500_000L, 500_000L, false),
            Account(2L, "BIDV", AccountType.BANK, 2_000_000L, 2_000_000L, false),
        )
        val repo = FakeAccountRepository(initialAccounts)
        val viewModel = AccountsViewModel(repo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.filterIsInstance<AccountsUiState.Success>().first()
        assertEquals(2, state.accounts.size)
        assertEquals(2_500_000L, state.totalEstimatedBalance)
    }

    @Test
    fun `viewModel adds, archives and updates account correctly`() = runTest {
        val repo = FakeAccountRepository(emptyList())
        val viewModel = AccountsViewModel(repo)

        testDispatcher.scheduler.advanceUntilIdle()

        // Add
        viewModel.addAccount("Ví MoMo", AccountType.EWALLET, 300_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state1 = viewModel.uiState.filterIsInstance<AccountsUiState.Success>().first()
        assertEquals(1, state1.accounts.size)
        val acc = state1.accounts[0]
        assertEquals("Ví MoMo", acc.name)

        // Update
        viewModel.updateAccount(acc.copy(name = "MoMo Pay"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state2 = viewModel.uiState.filterIsInstance<AccountsUiState.Success>().first { it.accounts[0].name == "MoMo Pay" }
        assertEquals("MoMo Pay", state2.accounts[0].name)

        // Archive
        viewModel.archiveAccount(acc.id)
        testDispatcher.scheduler.advanceUntilIdle()

        val state3 = viewModel.uiState.first()
        assertTrue(state3 is AccountsUiState.Empty || (state3 is AccountsUiState.Success && state3.accounts.isEmpty()))
    }
}
