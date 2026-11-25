package com.easycash.ui.reports

import androidx.compose.foundation.BorderStroke
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
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
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    val state = uiState as ReportsUiState.Success
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeader("Báo cáo tài chính", "Dòng tiền và hạn mức chi tiêu thực tế.")
        }

        // Period filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = state.selectedPeriod == period,
                        onClick = { onSelectPeriod(period) },
                        label = { Text(period.title) },
                        shape = RoundedCornerShape(10.dp),
                    )
                }
            }
        }

        // Net savings & flow summary card (UI_REFRESH_LIGHT.md)
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                ) {
                    Text(
                        text = "Tiết kiệm ròng trong kỳ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatVnd(state.netSavings),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (state.netSavings >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconTile(Glyph.Income, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Tổng thu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    CurrencyUtils.formatVnd(state.totalIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconTile(Glyph.Expense, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Tổng chi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    CurrencyUtils.formatVnd(state.totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category breakdown section
        item {
            Text(
                text = "Phân bổ chi tiêu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (state.categorySpendings.isEmpty()) {
            item {
                EmptyPanel(
                    title = "Chưa có khoản chi",
                    description = "Các khoản chi tiêu được ghi nhận trong kỳ này sẽ hiển thị ở đây.",
                    glyph = Glyph.Reports,
                )
            }
        } else {
            item {
                SpendingChart(state.categorySpendings, state.totalExpense)
            }
            items(state.categorySpendings, key = { it.categoryId }) { item ->
                CategorySpendingItem(item)
            }
        }

        // Budget section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ngân sách tháng này",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(onClick = onOpenSetBudget) {
                    AppIcon(Glyph.Plus, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Thiết lập")
                }
            }
        }

        if (state.budgetUsages.isEmpty()) {
            item {
                EmptyPanel(
                    title = "Chưa đặt ngân sách",
                    description = "Đặt hạn mức chi tiêu cho từng danh mục để kiểm soát tài chính.",
                    glyph = Glyph.Wallet,
                    action = "Đặt ngân sách",
                    onAction = onOpenSetBudget,
                )
            }
        } else {
            items(state.budgetUsages, key = { it.budgetId }) { usage ->
                BudgetUsageItem(usage, onDelete = { onDeleteBudget(usage.budgetId) })
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(180.dp)
                .semantics { contentDescription = description },
        ) {
            var start = -90f
            items.forEachIndexed { index, item ->
                val sweep = item.percentage.coerceIn(0f, 1f) * 360f
                if (sweep > 0) {
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = start,
                        sweepAngle = (sweep - 3f).coerceAtLeast(0.5f),
                        useCenter = false,
                        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Tổng chi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = CurrencyUtils.formatVnd(total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CategorySpendingItem(
    item: CategorySpendingUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CurrencyUtils.formatVnd(item.amount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, "(%.1f%%)", item.percentage * 100),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { item.percentage.coerceIn(0f, 1f) },
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
    usage: BudgetUsageUiModel,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = when {
        usage.isExceeded -> MaterialTheme.colorScheme.error
        usage.isWarning -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = usage.categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    AppIcon(Glyph.Trash, "Xóa ngân sách", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Đã chi: ${CurrencyUtils.formatVnd(usage.spentAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
                Text(
                    text = "Hạn mức: ${CurrencyUtils.formatVnd(usage.limitAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { usage.percentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = statusColor,
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
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 0L) }
    var limitText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thiết lập ngân sách tháng") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Chọn danh mục:", style = MaterialTheme.typography.labelMedium)
                categories.forEach { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RadioButton(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                        )
                        Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it.filter { char -> char.isDigit() } },
                    label = { Text("Hạn mức chi tiêu (₫)") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitText.toLongOrNull()
                    if (limit != null && limit > 0 && selectedCategoryId != 0L) {
                        onConfirm(selectedCategoryId, limit)
                    }
                },
                enabled = limitText.toLongOrNull() != null && limitText.toLong() > 0 && selectedCategoryId != 0L,
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

@Preview(name = "ReportsScreen Light", showBackground = true)
@Composable
private fun ReportsScreenLightPreview() {
    EasyCashTheme(darkTheme = false) {
        ReportsScreen()
    }
}

@Preview(name = "ReportsScreen Dark", showBackground = true)
@Composable
private fun ReportsScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        ReportsScreen()
    }
}
