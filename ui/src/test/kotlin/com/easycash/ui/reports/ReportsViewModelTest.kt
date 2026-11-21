package com.easycash.ui.reports

import com.easycash.domain.Category
import com.easycash.domain.CategorySpending
import com.easycash.domain.CategoryType
import com.easycash.domain.StatsRepository
import com.easycash.domain.Summary
import com.easycash.domain.TimeRange
import com.easycash.ui.fake.FakeBudgetRepository
import com.easycash.ui.fake.FakeCategoryRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeStatsRepo = object : StatsRepository {
        override fun observeSummary(range: TimeRange): Flow<Summary> =
            flowOf(Summary(income = 10_000_000L, expense = 4_000_000L))

        override fun observeSpendingByCategory(range: TimeRange): Flow<List<CategorySpending>> =
            flowOf(
                listOf(
                    CategorySpending(1L, 3_000_000L),
                    CategorySpending(2L, 1_000_000L),
                )
            )
    }

    private val categories = listOf(
        Category(1L, "Ăn uống", CategoryType.EXPENSE, "food", "orange", true),
        Category(2L, "Di chuyển", CategoryType.EXPENSE, "transit", "blue", true),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel calculates summary, savings and category percentages correctly`() = runTest {
        val catRepo = FakeCategoryRepository(categories)
        val budgetRepo = FakeBudgetRepository()
        val viewModel = ReportsViewModel(fakeStatsRepo, catRepo, budgetRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.filterIsInstance<ReportsUiState.Success>().first()
        assertEquals(ReportPeriod.THIS_MONTH, state.selectedPeriod)
        assertEquals(10_000_000L, state.totalIncome)
        assertEquals(4_000_000L, state.totalExpense)
        assertEquals(6_000_000L, state.netSavings)
        assertEquals(2, state.categorySpendings.size)

        assertEquals("Ăn uống", state.categorySpendings[0].categoryName)
        assertEquals(0.75f, state.categorySpendings[0].percentage, 0.001f)

        assertEquals("Di chuyển", state.categorySpendings[1].categoryName)
        assertEquals(0.25f, state.categorySpendings[1].percentage, 0.001f)
    }

    @Test
    fun `selectPeriod updates selected period in state`() = runTest {
        val catRepo = FakeCategoryRepository(categories)
        val budgetRepo = FakeBudgetRepository()
        val viewModel = ReportsViewModel(fakeStatsRepo, catRepo, budgetRepo)

        viewModel.selectPeriod(ReportPeriod.LAST_MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.filterIsInstance<ReportsUiState.Success>().first()
        assertEquals(ReportPeriod.LAST_MONTH, state.selectedPeriod)
    }

    @Test
    fun `viewModel sets and deletes budgets correctly`() = runTest {
        val catRepo = FakeCategoryRepository(categories)
        val budgetRepo = FakeBudgetRepository()
        val viewModel = ReportsViewModel(fakeStatsRepo, catRepo, budgetRepo)

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setBudget(categoryId = 1L, limitAmount = 5_000_000L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state1 = viewModel.uiState.filterIsInstance<ReportsUiState.Success>().first { it.budgetUsages.isNotEmpty() }
        assertEquals(1, state1.budgetUsages.size)
        assertEquals("Ăn uống", state1.budgetUsages[0].categoryName)
        assertEquals(5_000_000L, state1.budgetUsages[0].limitAmount)

        viewModel.deleteBudget(state1.budgetUsages[0].budgetId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state2 = viewModel.uiState.filterIsInstance<ReportsUiState.Success>().first { it.budgetUsages.isEmpty() }
        assertEquals(0, state2.budgetUsages.size)
    }
}
