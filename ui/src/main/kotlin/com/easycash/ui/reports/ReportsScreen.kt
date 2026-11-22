package com.easycash.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.Category
import com.easycash.ui.components.*
import com.easycash.ui.reminders.RemindersScreen
import com.easycash.ui.reminders.RemindersViewModel
import com.easycash.ui.theme.EasyCashTheme
import com.easycash.ui.theme.ExpenseDark
import com.easycash.ui.theme.IncomeDark
import com.easycash.ui.theme.WarningDark
import com.easycash.ui.util.CurrencyUtils
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel? = null,
    remindersViewModel: RemindersViewModel? = null,
    modifier: Modifier = Modifier,
) {
    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Báo cáo & Ngân sách") },
                icon = { AppIcon(Glyph.Reports, modifier = Modifier.size(18.dp)) },
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Khoản định kỳ") },
                icon = { AppIcon(Glyph.Repeat, modifier = Modifier.size(18.dp)) },
            )
        }

        if (selectedSubTab == 0) {
            if (viewModel == null) {
                ReportsContent(
                    uiState = ReportsUiState.Success(
                        selectedPeriod = ReportPeriod.THIS_MONTH,
                        totalIncome = 15_000_000L,
                        totalExpense = 6_500_000L,
                        netSavings = 8_500_000L,
                        categorySpendings = listOf(
                            CategorySpendingUiModel(1L, "Ăn uống", 3_500_000L, 0.54f),
                            CategorySpendingUiModel(2L, "Mua sắm", 2_000_000L, 0.31f),
                            CategorySpendingUiModel(3L, "Di chuyển", 1_000_000L, 0.15f),
                        ),
                        budgetUsages = listOf(
                            BudgetUsageUiModel(1L, 1L, "Ăn uống", 4_000_000L, 3_500_000L, 0.875f, isWarning = true, isExceeded = false),
                            BudgetUsageUiModel(2L, 2L, "Mua sắm", 1_500_000L, 2_000_000L, 1.33f, isWarning = false, isExceeded = true),
                        ),
                    ),
                    onSelectPeriod = {},
                    onOpenSetBudget = { showSetBudgetDialog = true },
                    onDeleteBudget = {},
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val writeError by viewModel.writeError.collectAsStateWithLifecycle()
                if (writeError != null) {
                    AlertDialog(
                        onDismissRequest = viewModel::dismissWriteError,
                        title = { Text("Chưa lưu được thay đổi") },
                        text = { Text(writeError.orEmpty()) },
                        confirmButton = { TextButton(onClick = viewModel::dismissWriteError) { Text("Đã hiểu") } },
                    )
                }
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ReportsContent(
                    uiState = uiState,
                    onSelectPeriod = { viewModel.selectPeriod(it) },
                    onOpenSetBudget = { showSetBudgetDialog = true },
                    onDeleteBudget = { viewModel.deleteBudget(it) },
                    modifier = Modifier.fillMaxSize(),
                )

                if (showSetBudgetDialog && uiState is ReportsUiState.Success) {
                    val success = uiState as ReportsUiState.Success
                    SetBudgetDialog(
                        categories = success.expenseCategories,
                        onDismiss = { showSetBudgetDialog = false },
                        onConfirm = { catId, limit ->
                            viewModel.setBudget(catId, limit, onSuccess = { showSetBudgetDialog = false })
                        },
                    )
                }
            }
        } else {
            RemindersScreen(
                viewModel = remindersViewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun ReportsContent(
    uiState: ReportsUiState,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onOpenSetBudget: () -> Unit = {},
    onDeleteBudget: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (uiState is ReportsUiState.Loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val state = uiState as ReportsUiState.Success
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { ScreenHeader("Báo cáo", "Dòng tiền và hạn mức chi tiêu.") }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportPeriod.entries.forEach { period ->
                    FilterChip(selected = state.selectedPeriod == period, onClick = { onSelectPeriod(period) }, label = { Text(period.title) })
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("Chênh lệch thu chi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        CurrencyUtils.formatVnd(state.netSavings),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (state.netSavings >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(Glyph.Income, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(5.dp))
                                Text("Thu nhập", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(CurrencyUtils.formatVnd(state.totalIncome), style = MaterialTheme.typography.titleMedium)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(Glyph.Expense, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(5.dp))
                                Text("Chi tiêu", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(CurrencyUtils.formatVnd(state.totalExpense), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Phân bổ chi tiêu", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                AppIcon(Glyph.Reports, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.categorySpendings.isEmpty()) {
            item { EmptyPanel("Chưa có khoản chi", "Các giao dịch chi tiêu đã xác nhận sẽ xuất hiện ở đây.", Glyph.Reports) }
        } else {
            item { SpendingChart(state.categorySpendings, state.totalExpense) }
            items(state.categorySpendings, key = { it.categoryId }) { CategorySpendingItem(it) }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ngân sách tháng này", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenSetBudget) { AppIcon(Glyph.Plus, modifier = Modifier.size(18.dp)); Text(" Thêm") }
            }
        }
        if (state.budgetUsages.isEmpty()) {
            item { EmptyPanel("Chi tiêu theo kế hoạch", "Đặt hạn mức cho từng danh mục để theo dõi tiến độ trong tháng.", Glyph.Wallet, "Đặt ngân sách", onOpenSetBudget) }
        } else {
            items(state.budgetUsages, key = { it.budgetId }) { usage ->
                BudgetUsageItem(usage, { onDeleteBudget(usage.budgetId) })
            }
        }
    }
}

@Composable
private fun SpendingChart(items: List<CategorySpendingUiModel>, total: Long) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    )
    val description = items.joinToString(". ") { "${it.categoryName}: ${CurrencyUtils.formatVnd(it.amount)}" }
    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(190.dp).semantics { contentDescription = description }) {
            var start = -90f
            items.forEachIndexed { index, item ->
                val sweep = item.percentage.coerceIn(0f, 1f) * 360f
                if (sweep > 0) drawArc(
                    colors[index % colors.size],
                    start,
                    (sweep - 3f).coerceAtLeast(0.5f),
                    useCenter = false,
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round),
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tổng chi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(CurrencyUtils.formatVnd(total), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CategorySpendingItem(
    item: CategorySpendingUiModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CurrencyUtils.formatVnd(item.amount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.padding(start = 6.dp))
                    Text(
                        text = String.format(Locale.US, "(%.1f%%)", item.percentage * 100),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { item.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun BudgetUsageItem(
    item: BudgetUsageUiModel,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressColor = when {
        item.isExceeded -> ExpenseDark
        item.isWarning -> WarningDark
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (item.isExceeded) {
                        Spacer(modifier = Modifier.padding(start = 6.dp))
                        Text(
                            text = "VƯỢT HẠN MỨC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else if (item.isWarning) {
                        Spacer(modifier = Modifier.padding(start = 6.dp))
                        Text(
                            text = "SẮP ĐẠT HẠN MỨC",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = WarningDark,
                        )
                    }
                }

                TextButton(onClick = onDelete) {
                    Text("Xoá", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Đã chi: ${CurrencyUtils.formatVnd(item.spentAmount)} / ${CurrencyUtils.formatVnd(item.limitAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = String.format(Locale.US, "%.0f%%", item.percentage * 100),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { item.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun SetBudgetDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long, limitAmount: Long) -> Unit,
) {
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
    var limitText by remember { mutableStateOf("") }
    var limitError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thiết lập ngân sách tháng") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = "Chọn danh mục chi tiêu:", style = MaterialTheme.typography.labelMedium)
                Column {
                    categories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                            )
                            Text(text = cat.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        limitText = digits
                        limitError = when {
                            digits.isEmpty() -> "Vui lòng nhập hạn mức"
                            digits.toLongOrNull() == null -> "Hạn mức vượt quá giới hạn"
                            digits.toLong() <= 0L -> "Hạn mức phải lớn hơn 0"
                            else -> null
                        }
                    },
                    label = { Text("Hạn mức ngân sách (VND)") },
                    isError = limitError != null,
                    supportingText = {
                        if (limitError != null) {
                            Text(limitError!!, color = MaterialTheme.colorScheme.error)
                        } else {
                            val amt = limitText.toLongOrNull() ?: 0L
                            if (amt > 0L) {
                                Text(
                                    text = "= ${CurrencyUtils.formatVnd(amt)}",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            val amountValue = limitText.toLongOrNull() ?: 0L
            Button(
                onClick = {
                    if (amountValue > 0L && limitError == null) {
                        onConfirm(selectedCategoryId, amountValue)
                    }
                },
                enabled = amountValue > 0L && limitError == null && categories.isNotEmpty(),
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Huỷ")
            }
        },
    )
}

@Preview(name = "ReportsScreen Dark", showBackground = true)
@Composable
private fun ReportsScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        ReportsScreen()
    }
}
