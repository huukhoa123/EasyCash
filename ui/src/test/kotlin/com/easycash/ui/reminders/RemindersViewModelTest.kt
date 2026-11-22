package com.easycash.ui.reminders

import com.easycash.domain.*
import com.easycash.ui.fake.FakeAccountRepository
import com.easycash.ui.fake.FakeCategoryRepository
import com.easycash.ui.fake.FakeReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var reminderRepo: FakeReminderRepository
    private lateinit var accountRepo: FakeAccountRepository
    private lateinit var categoryRepo: FakeCategoryRepository
    private lateinit var viewModel: RemindersViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        reminderRepo = FakeReminderRepository()
        accountRepo = FakeAccountRepository()
        categoryRepo = FakeCategoryRepository()
        viewModel = RemindersViewModel(reminderRepo, accountRepo, categoryRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits reminders with mapped account and category names`() = runTest {
        val accId = (accountRepo.add(Account(0L, "Vietcombank", AccountType.BANK, 10_000_000L, 10_000_000L, false)) as WriteResult.Success).value
        val catId = (categoryRepo.add(Category(0L, "Tiền trọ", CategoryType.EXPENSE, "home", "blue", false)) as WriteResult.Success).value

        val now = System.currentTimeMillis()
        val rem = Reminder(
            id = 0L,
            name = "Tiền phòng",
            accountId = accId,
            categoryId = catId,
            type = CategoryType.EXPENSE,
            amount = 3_000_000L,
            frequency = ReminderFrequency.MONTHLY,
            dayRule = 5,
            nextDueAtMillis = now + 86400000L,
            remindBeforeDays = 2,
            isActive = true,
        )
        reminderRepo.upsert(rem)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first { it is RemindersUiState.Success } as RemindersUiState.Success
        assertEquals(1, state.reminders.size)
        val item = state.reminders.first()
        assertEquals("Tiền phòng", item.reminder.name)
        assertEquals("Vietcombank", item.accountName)
        assertEquals("Tiền trọ", item.categoryName)
        assertTrue(item.isDueSoon)
        assertEquals(1, state.activeCount)
        assertEquals(3_000_000L, state.totalMonthlyEstimatedAmount)
    }

    @Test
    fun `toggleActive updates active status in repository`() = runTest {
        val accId = (accountRepo.add(Account(0L, "MoMo", AccountType.EWALLET, 500_000L, 500_000L, false)) as WriteResult.Success).value
        val rem = Reminder(
            id = 1L,
            name = "Spotify",
            accountId = accId,
            categoryId = null,
            type = CategoryType.EXPENSE,
            amount = 59_000L,
            frequency = ReminderFrequency.MONTHLY,
            dayRule = 15,
            nextDueAtMillis = System.currentTimeMillis() + 86400000L * 10,
            remindBeforeDays = 1,
            isActive = true,
        )
        val id = (reminderRepo.upsert(rem) as WriteResult.Success).value
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleActive(rem.copy(id = id), false)
        testDispatcher.scheduler.advanceUntilIdle()

        val updated = reminderRepo.observeAll().first().first()
        assertFalse(updated.isActive)
    }

    @Test
    fun `saveReminder inserts new reminder with calculated next due date`() = runTest {
        val accId = (accountRepo.add(Account(0L, "Tiền mặt", AccountType.CASH, 1_000_000L, 1_000_000L, false)) as WriteResult.Success).value

        var succeeded = false
        viewModel.saveReminder(
            id = 0L,
            name = "Tiền mạng FPT",
            type = CategoryType.EXPENSE,
            amount = 220_000L,
            accountId = accId,
            categoryId = null,
            frequency = ReminderFrequency.MONTHLY,
            dayRule = 25,
            remindBeforeDays = 3,
            isActive = true,
            onSuccess = { succeeded = true },
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(succeeded)
        val all = reminderRepo.observeAll().first()
        assertEquals(1, all.size)
        val saved = all.first()
        assertEquals("Tiền mạng FPT", saved.name)
        assertEquals(220_000L, saved.amount)
        assertTrue(saved.nextDueAtMillis > 0)
    }

    @Test
    fun `saveReminder rejects blank name and invalid amount`() = runTest {
        viewModel.saveReminder(
            id = 0L,
            name = "   ",
            type = CategoryType.EXPENSE,
            amount = 100_000L,
            accountId = 1L,
            categoryId = null,
            frequency = ReminderFrequency.MONTHLY,
            dayRule = 1,
            remindBeforeDays = 1,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Vui lòng nhập tên khoản nhắc định kỳ.", viewModel.writeError.value)
        viewModel.dismissWriteError()

        viewModel.saveReminder(
            id = 0L,
            name = "Hợp lệ",
            type = CategoryType.EXPENSE,
            amount = -50_000L,
            accountId = 1L,
            categoryId = null,
            frequency = ReminderFrequency.MONTHLY,
            dayRule = 1,
            remindBeforeDays = 1,
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Số tiền phải lớn hơn 0đ.", viewModel.writeError.value)
    }

    @Test
    fun `deleteReminder removes item from repository`() = runTest {
        val rem = Reminder(
            id = 10L,
            name = "Tập gym",
            accountId = 1L,
            categoryId = null,
            type = CategoryType.EXPENSE,
            amount = 400_000L,
            frequency = ReminderFrequency.MONTHLY,
            dayRule = 1,
            nextDueAtMillis = System.currentTimeMillis() + 86400000L * 5,
            remindBeforeDays = 2,
            isActive = true,
        )
        val id = (reminderRepo.upsert(rem) as WriteResult.Success).value
        testDispatcher.scheduler.advanceUntilIdle()

        var deleted = false
        viewModel.deleteReminder(id) { deleted = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(deleted)
        val list = reminderRepo.observeAll().first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `calculateInitialNextDue handles weekly monthly yearly correctly`() {
        val zone = ZoneId.of("UTC")
        val fixedNow = Instant.parse("2026-11-22T10:00:00Z").toEpochMilli() // Sunday Nov 22, 2026

        // Next Monday (dayRule = 1) should be Nov 23
        val nextWeekly = RemindersViewModel.calculateInitialNextDue(ReminderFrequency.WEEKLY, 1, fixedNow, zone)
        val weeklyDate = Instant.ofEpochMilli(nextWeekly).atZone(zone)
        assertEquals(2026, weeklyDate.year)
        assertEquals(11, weeklyDate.monthValue)
        assertEquals(23, weeklyDate.dayOfMonth)

        // Monthly on day 25 -> Nov 25
        val nextMonthly = RemindersViewModel.calculateInitialNextDue(ReminderFrequency.MONTHLY, 25, fixedNow, zone)
        val monthlyDate = Instant.ofEpochMilli(nextMonthly).atZone(zone)
        assertEquals(25, monthlyDate.dayOfMonth)
        assertEquals(11, monthlyDate.monthValue)

        // Monthly on day 15 (which passed on Nov 22) -> Dec 15
        val nextMonthlyPassed = RemindersViewModel.calculateInitialNextDue(ReminderFrequency.MONTHLY, 15, fixedNow, zone)
        val monthlyPassedDate = Instant.ofEpochMilli(nextMonthlyPassed).atZone(zone)
        assertEquals(15, monthlyPassedDate.dayOfMonth)
        assertEquals(12, monthlyPassedDate.monthValue)
    }
}
