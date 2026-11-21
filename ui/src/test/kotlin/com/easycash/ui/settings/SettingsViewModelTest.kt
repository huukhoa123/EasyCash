package com.easycash.ui.settings

import com.easycash.domain.Account
import com.easycash.domain.AccountType
import com.easycash.domain.CategoryType
import com.easycash.domain.NotificationSource
import com.easycash.ui.fake.FakeAccountRepository
import com.easycash.ui.fake.FakeCategoryRepository
import com.easycash.ui.fake.FakeNotificationSourceRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `viewModel toggles source and links account correctly`() = runTest {
        val sources = listOf(
            NotificationSource(1L, "com.mservice.momopay", "Ví MoMo", true, 1L),
            NotificationSource(2L, "com.vnpay.bidv", "BIDV SmartBanking", true, null),
        )
        val accounts = listOf(
            Account(1L, "MoMo", AccountType.EWALLET, 0L, 0L, false),
            Account(2L, "BIDV", AccountType.BANK, 0L, 0L, false),
        )

        val srcRepo = FakeNotificationSourceRepository(sources)
        val accRepo = FakeAccountRepository(accounts)
        val catRepo = FakeCategoryRepository(emptyList())
        val viewModel = SettingsViewModel(srcRepo, accRepo, catRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.filterIsInstance<SettingsUiState.Success>().first()
        assertEquals(2, state.sources.size)
        assertEquals(2, state.accounts.size)
        assertTrue(state.sources[0].isEnabled)
        assertTrue(state.isAccessGranted)

        // Toggle MoMo to disabled
        viewModel.toggleSource(1L, false)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState1 = viewModel.uiState.filterIsInstance<SettingsUiState.Success>().first { !it.sources[0].isEnabled }
        assertFalse(updatedState1.sources[0].isEnabled)

        // Link BIDV to Account 2
        viewModel.linkAccount(2L, 2L)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState2 = viewModel.uiState.filterIsInstance<SettingsUiState.Success>().first { it.sources[1].linkedAccountId == 2L }
        assertEquals(2L, updatedState2.sources[1].linkedAccountId)

        // Unlink BIDV
        viewModel.linkAccount(2L, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedState3 = viewModel.uiState.filterIsInstance<SettingsUiState.Success>().first { it.sources[1].linkedAccountId == null }
        org.junit.Assert.assertNull(updatedState3.sources[1].linkedAccountId)
    }

    @Test
    fun `viewModel can add category`() = runTest {
        val srcRepo = FakeNotificationSourceRepository(emptyList())
        val accRepo = FakeAccountRepository(emptyList())
        val catRepo = FakeCategoryRepository(emptyList())
        val viewModel = SettingsViewModel(srcRepo, accRepo, catRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.addCategory("Xăng xe", CategoryType.EXPENSE)
        testDispatcher.scheduler.advanceUntilIdle()

        val state1 = viewModel.uiState.filterIsInstance<SettingsUiState.Success>().first { it.categories.isNotEmpty() }
        assertEquals(1, state1.categories.size)
        assertEquals("Xăng xe", state1.categories[0].name)
    }
}
