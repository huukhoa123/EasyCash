package com.easycash.ui.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easycash.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class ReminderUiModel(
    val reminder: Reminder,
    val accountName: String,
    val categoryName: String?,
    val isDueSoon: Boolean,
)

sealed interface RemindersUiState {
    data object Loading : RemindersUiState
    data class Success(
        val reminders: List<ReminderUiModel>,
        val accounts: List<Account>,
        val categories: List<Category>,
        val activeCount: Int,
        val totalMonthlyEstimatedAmount: Long?,
        val isTotalAmountOverflow: Boolean = false,
    ) : RemindersUiState
}

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _writeError = MutableStateFlow<String?>(null)
    val writeError: StateFlow<String?> = _writeError.asStateFlow()

    fun dismissWriteError() {
        _writeError.value = null
    }

    private val accountsFlow = accountRepository.observeAll()
    private val categoriesFlow = combine(
        categoryRepository.observeAll(CategoryType.EXPENSE),
        categoryRepository.observeAll(CategoryType.INCOME),
    ) { exp, inc -> exp + inc }

    val uiState: StateFlow<RemindersUiState> = combine(
        reminderRepository.observeAll(),
        accountsFlow,
        categoriesFlow,
    ) { reminders, accounts, categories ->
        val accountMap = accounts.associate { it.id to it.name }
        val categoryMap = categories.associate { it.id to it.name }
        val now = System.currentTimeMillis()

        val uiModels = reminders.map { rem ->
            val dueMillis = rem.nextDueAtMillis
            val remindBeforeMillis = rem.remindBeforeDays * 24L * 60L * 60L * 1000L
            val isDueSoon = rem.isActive && (now >= dueMillis - remindBeforeMillis)
            ReminderUiModel(
                reminder = rem,
                accountName = accountMap[rem.accountId] ?: "Tài khoản #${rem.accountId}",
                categoryName = rem.categoryId?.let { categoryMap[it] },
                isDueSoon = isDueSoon,
            )
        }

        val activeCount = reminders.count { it.isActive }
        val maxLongBig = BigInteger.valueOf(Long.MAX_VALUE)
        var totalBig = BigInteger.ZERO

        for (rem in reminders) {
            val amount = rem.amount
            if (!rem.isActive || rem.type != CategoryType.EXPENSE || amount == null) continue
            val amtBig = BigInteger.valueOf(amount)
            val monthlyBig = when (rem.frequency) {
                ReminderFrequency.WEEKLY -> amtBig.multiply(BigInteger.valueOf(4))
                ReminderFrequency.MONTHLY -> amtBig
                ReminderFrequency.YEARLY -> amtBig.divide(BigInteger.valueOf(12))
            }
            totalBig = totalBig.add(monthlyBig)
        }

        val isOverflow = totalBig > maxLongBig
        val totalAmount = if (isOverflow) null else totalBig.toLong()

        RemindersUiState.Success(
            reminders = uiModels,
            accounts = accounts.filter { !it.isArchived },
            categories = categories,
            activeCount = activeCount,
            totalMonthlyEstimatedAmount = totalAmount,
            isTotalAmountOverflow = isOverflow,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RemindersUiState.Loading,
    )

    fun toggleActive(reminder: Reminder, isActive: Boolean) {
        viewModelScope.launch {
            val updated = reminder.copy(isActive = isActive)
            when (val result = reminderRepository.upsert(updated)) {
                is WriteResult.Success -> Unit
                is WriteResult.Failure -> _writeError.value = mapFailure(result)
            }
        }
    }

    fun saveReminder(
        id: Long,
        name: String,
        type: CategoryType,
        amount: Long?,
        accountId: Long,
        categoryId: Long?,
        frequency: ReminderFrequency,
        dayRule: Int,
        remindBeforeDays: Int,
        isActive: Boolean = true,
        onSuccess: () -> Unit = {},
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _writeError.value = "Vui lòng nhập tên khoản nhắc định kỳ."
            return
        }
        if (amount != null && amount <= 0L) {
            _writeError.value = "Số tiền phải lớn hơn 0đ."
            return
        }

        viewModelScope.launch {
            val nextDue = calculateInitialNextDue(frequency, dayRule)
            val reminder = Reminder(
                id = id,
                name = trimmedName,
                accountId = accountId,
                categoryId = categoryId,
                type = type,
                amount = amount,
                frequency = frequency,
                dayRule = dayRule,
                nextDueAtMillis = nextDue,
                remindBeforeDays = remindBeforeDays.coerceIn(0, 30),
                isActive = isActive,
            )
            when (val res = reminderRepository.upsert(reminder)) {
                is WriteResult.Success -> onSuccess()
                is WriteResult.Failure -> _writeError.value = mapFailure(res)
            }
        }
    }

    fun deleteReminder(id: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = reminderRepository.delete(id)) {
                is WriteResult.Success -> onSuccess()
                is WriteResult.Failure -> _writeError.value = mapFailure(res)
            }
        }
    }

    private fun mapFailure(failure: WriteResult.Failure): String = when (failure) {
        WriteResult.Failure.INVALID_INPUT -> "Thông tin không hợp lệ. Vui lòng kiểm tra lại tài khoản hoặc số tiền."
        WriteResult.Failure.NOT_FOUND -> "Không tìm thấy khoản nhắc để cập nhật."
        WriteResult.Failure.CONFLICT -> "Khoản nhắc bị trùng lặp."
        WriteResult.Failure.STORAGE_ERROR -> "Lỗi lưu trữ cơ sở dữ liệu."
    }

    companion object {
        fun calculateInitialNextDue(
            frequency: ReminderFrequency,
            dayRule: Int,
            nowMillis: Long = System.currentTimeMillis(),
            zone: ZoneId = ZoneId.systemDefault(),
        ): Long {
            val zdt = Instant.ofEpochMilli(nowMillis).atZone(zone)
            return when (frequency) {
                ReminderFrequency.WEEKLY -> {
                    val targetDow = java.time.DayOfWeek.of(dayRule.coerceIn(1, 7))
                    var candidate = zdt.with(TemporalAdjusters.nextOrSame(targetDow))
                        .withHour(9).withMinute(0).withSecond(0).withNano(0)
                    if (candidate.toInstant().toEpochMilli() < nowMillis) {
                        candidate = candidate.plusWeeks(1)
                    }
                    candidate.toInstant().toEpochMilli()
                }
                ReminderFrequency.MONTHLY -> {
                    val lengthOfMonth = zdt.toLocalDate().lengthOfMonth()
                    val safeDay = minOf(dayRule.coerceIn(1, 31), lengthOfMonth)
                    var candidate = zdt.withDayOfMonth(safeDay).withHour(9).withMinute(0).withSecond(0).withNano(0)
                    if (candidate.toInstant().toEpochMilli() < nowMillis) {
                        val nextMonth = zdt.plusMonths(1)
                        val nextSafeDay = minOf(dayRule.coerceIn(1, 31), nextMonth.toLocalDate().lengthOfMonth())
                        candidate = nextMonth.withDayOfMonth(nextSafeDay).withHour(9).withMinute(0).withSecond(0).withNano(0)
                    }
                    candidate.toInstant().toEpochMilli()
                }
                ReminderFrequency.YEARLY -> {
                    val lengthOfMonth = zdt.toLocalDate().lengthOfMonth()
                    val safeDay = minOf(dayRule.coerceIn(1, 31), lengthOfMonth)
                    var candidate = zdt.withDayOfMonth(safeDay).withHour(9).withMinute(0).withSecond(0).withNano(0)
                    if (candidate.toInstant().toEpochMilli() < nowMillis) {
                        val nextYear = zdt.plusYears(1)
                        val nextSafeDay = minOf(dayRule.coerceIn(1, 31), nextYear.toLocalDate().lengthOfMonth())
                        candidate = nextYear.withDayOfMonth(nextSafeDay).withHour(9).withMinute(0).withSecond(0).withNano(0)
                    }
                    candidate.toInstant().toEpochMilli()
                }
            }
        }
    }
}
