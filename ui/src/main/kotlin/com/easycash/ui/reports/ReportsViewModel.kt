package com.easycash.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easycash.domain.Budget
import com.easycash.domain.BudgetRepository
import com.easycash.domain.Category
import com.easycash.domain.CategoryRepository
import com.easycash.domain.CategorySpending
import com.easycash.domain.CategoryType
import com.easycash.domain.StatsRepository
import com.easycash.domain.Summary
import com.easycash.domain.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class ReportPeriod(val title: String) {
    THIS_MONTH("Tháng này"),
    LAST_MONTH("Tháng trước"),
    ALL_TIME("Toàn bộ"),
}

data class CategorySpendingUiModel(
    val categoryId: Long,
    val categoryName: String,
    val amount: Long,
    val percentage: Float, // 0.0f .. 1.0f
)

data class BudgetUsageUiModel(
    val budgetId: Long,
    val categoryId: Long,
    val categoryName: String,
    val limitAmount: Long,
    val spentAmount: Long,
    val percentage: Float, // 0.0f .. 1.0f (or > 1.0f)
    val isWarning: Boolean,
    val isExceeded: Boolean,
)

sealed interface ReportsUiState {
    data object Loading : ReportsUiState
    data class Success(
        val selectedPeriod: ReportPeriod,
        val totalIncome: Long,
        val totalExpense: Long,
        val netSavings: Long,
        val categorySpendings: List<CategorySpendingUiModel>,
        val budgetUsages: List<BudgetUsageUiModel> = emptyList(),
        val expenseCategories: List<Category> = emptyList(),
    ) : ReportsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.THIS_MONTH)
    val selectedPeriod: StateFlow<ReportPeriod> = _selectedPeriod.asStateFlow()

    fun selectPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
    }

    private val currentMonth: Int = run {
        val cal = Calendar.getInstance()
        cal.get(Calendar.YEAR) * 100 + (cal.get(Calendar.MONTH) + 1)
    }

    private val expenseCategoriesFlow = categoryRepository.observeAll(CategoryType.EXPENSE)
    private val budgetUsageFlow = budgetRepository.observeUsage(currentMonth)

    val uiState: StateFlow<ReportsUiState> = _selectedPeriod.flatMapLatest { period ->
        val range = calculateTimeRange(period)
        combine(
            statsRepository.observeSummary(range),
            statsRepository.observeSpendingByCategory(range),
            expenseCategoriesFlow,
            budgetUsageFlow,
        ) { summary, spendings, categories, usages ->
            val totalExpense = summary.expense
            val catMap = categories.associate { it.id to it.name }
            val spendingModels = spendings.map { item ->
                val ratio = if (totalExpense > 0L) {
                    (item.amount.toFloat() / totalExpense).coerceIn(0f, 1f)
                } else {
                    0f
                }
                CategorySpendingUiModel(
                    categoryId = item.categoryId ?: 0L,
                    categoryName = item.categoryId?.let { catMap[it] } ?: "Chưa phân loại",
                    amount = item.amount,
                    percentage = ratio,
                )
            }

            val budgetModels = usages.map { usage ->
                val limit = usage.budget.limitAmount
                val spent = usage.spent
                val ratio = if (limit > 0L) spent.toFloat() / limit else 0f
                val pct = if (limit > 0L) (spent * 100) / limit else 0L
                BudgetUsageUiModel(
                    budgetId = usage.budget.id,
                    categoryId = usage.budget.categoryId,
                    categoryName = catMap[usage.budget.categoryId] ?: "Danh mục #${usage.budget.categoryId}",
                    limitAmount = limit,
                    spentAmount = spent,
                    percentage = ratio,
                    isWarning = pct >= usage.budget.warnThresholdPercent && spent <= limit,
                    isExceeded = spent > limit,
                )
            }

            ReportsUiState.Success(
                selectedPeriod = period,
                totalIncome = summary.income,
                totalExpense = summary.expense,
                netSavings = summary.income - summary.expense,
                categorySpendings = spendingModels,
                budgetUsages = budgetModels,
                expenseCategories = categories,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ReportsUiState.Loading,
    )

    fun setBudget(categoryId: Long, limitAmount: Long, warnThresholdPercent: Int = 80) {
        if (limitAmount <= 0L) return
        viewModelScope.launch {
            budgetRepository.upsert(
                Budget(
                    id = 0L,
                    categoryId = categoryId,
                    month = currentMonth,
                    limitAmount = limitAmount,
                    warnThresholdPercent = warnThresholdPercent,
                )
            )
        }
    }

    fun deleteBudget(budgetId: Long) {
        viewModelScope.launch {
            budgetRepository.delete(budgetId)
        }
    }

    private fun calculateTimeRange(period: ReportPeriod): TimeRange {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when (period) {
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis
                TimeRange(start, end)
            }

            ReportPeriod.LAST_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val end = cal.timeInMillis
                cal.add(Calendar.MONTH, -1)
                val start = cal.timeInMillis
                TimeRange(start, end)
            }

            ReportPeriod.ALL_TIME -> {
                TimeRange(0L, Long.MAX_VALUE)
            }
        }
    }
}
