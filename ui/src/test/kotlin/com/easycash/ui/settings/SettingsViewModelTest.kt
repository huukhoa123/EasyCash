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

    @Test
    fun `exportBackup validates password length and wipes char array`() = runTest {
        val srcRepo = FakeNotificationSourceRepository(emptyList())
        val accRepo = FakeAccountRepository(emptyList())
        val catRepo = FakeCategoryRepository(emptyList())
        val dataRepo = com.easycash.ui.fake.FakeDataManagementRepository()
        val viewModel = SettingsViewModel(srcRepo, accRepo, catRepo, dataRepo)

        var exportedBytes: ByteArray? = null
        val shortPass = "short".toCharArray()
        viewModel.exportBackup(shortPass) { exportedBytes = it }
        testDispatcher.scheduler.advanceUntilIdle()

        org.junit.Assert.assertNull(exportedBytes)
        assertEquals("Mật khẩu sao lưu phải có từ 8 đến 1024 ký tự.", viewModel.writeError.value)
        // Password should be wiped
        assertTrue(shortPass.all { it == '\u0000' })

        viewModel.dismissWriteError()
        org.junit.Assert.assertNull(viewModel.writeError.value)

        val longPass = CharArray(1025) { 'a' }
        viewModel.exportBackup(longPass) { exportedBytes = it }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Mật khẩu sao lưu phải có từ 8 đến 1024 ký tự.", viewModel.writeError.value)
        assertTrue(longPass.all { it == '\u0000' })

        viewModel.dismissWriteError()

        val validPass = "strongPassword123".toCharArray()
        viewModel.exportBackup(validPass) { exportedBytes = it }
        testDispatcher.scheduler.advanceUntilIdle()

        org.junit.Assert.assertNotNull(exportedBytes)
        assertEquals(3, exportedBytes?.size)
        // Valid password should also be wiped in finally
        assertTrue(validPass.all { it == '\u0000' })
    }

    @Test
    fun `restoreBackup validates exact boundary size and wipes password`() = runTest {
        val srcRepo = FakeNotificationSourceRepository(emptyList())
        val accRepo = FakeAccountRepository(emptyList())
        val catRepo = FakeCategoryRepository(emptyList())
        val dataRepo = com.easycash.ui.fake.FakeDataManagementRepository()
        val viewModel = SettingsViewModel(srcRepo, accRepo, catRepo, dataRepo)

        var restored = false
        // Boundary check: 32MB + 49 bytes (> MAX_BACKUP_SIZE)
        val oversized = ByteArray(SettingsViewModel.MAX_BACKUP_SIZE + 1)
        val pass1 = "password".toCharArray()
        viewModel.restoreBackup(oversized, pass1) { restored = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(restored)
        assertEquals("File sao lưu vượt quá dung lượng tối đa 32MB.", viewModel.writeError.value)
        assertTrue(pass1.all { it == '\u0000' })

        viewModel.dismissWriteError()

        // Boundary check: exactly MAX_BACKUP_SIZE (32MB + 48 bytes) should be ACCEPTED
        val boundaryBytes = ByteArray(SettingsViewModel.MAX_BACKUP_SIZE)
        val pass2 = "boundaryPass".toCharArray()
        viewModel.restoreBackup(boundaryBytes, pass2) { restored = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(restored)
        assertEquals("Khôi phục dữ liệu thành công!", viewModel.infoMessage.value)
        assertTrue(pass2.all { it == '\u0000' })

        // Failure restore check
        viewModel.dismissInfoMessage()
        dataRepo.restoreResult = com.easycash.domain.WriteResult.Failure.INVALID_INPUT
        var failedRestored = false
        val pass3 = "wrongPassword".toCharArray()
        val validBytes = byteArrayOf(1, 2, 3)
        viewModel.restoreBackup(validBytes, pass3) { failedRestored = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(failedRestored)
        assertEquals("Khôi phục thất bại. Kiểm tra mật khẩu hoặc tính hợp lệ của file.", viewModel.writeError.value)
        assertTrue(pass3.all { it == '\u0000' })
    }

    @Test
    fun `exportCsv and clearAllData invoke callbacks properly`() = runTest {
        val srcRepo = FakeNotificationSourceRepository(emptyList())
        val accRepo = FakeAccountRepository(emptyList())
        val catRepo = FakeCategoryRepository(emptyList())
        val dataRepo = com.easycash.ui.fake.FakeDataManagementRepository()
        val viewModel = SettingsViewModel(srcRepo, accRepo, catRepo, dataRepo)

        var csvOutput: String? = null
        viewModel.exportCsv { csvOutput = it }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("id,account_id\n1,1", csvOutput)

        var cleared = false
        viewModel.clearAllData { cleared = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(cleared)
        assertEquals("Đã xoá sạch dữ liệu và đưa về trạng thái ban đầu.", viewModel.infoMessage.value)
    }
}
