package com.easycash.ui.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.Category
import com.easycash.ui.theme.EasyCashTheme
import com.easycash.ui.theme.ExpenseDark
import com.easycash.ui.theme.IncomeDark
import com.easycash.ui.theme.WarningDark
import com.easycash.ui.util.CurrencyUtils
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel? = null,
    modifier: Modifier = Modifier,
) {
    var showSetBudgetDialog by remember { mutableStateOf(false) }

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
            modifier = modifier,
        )
    } else {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ReportsContent(
            uiState = uiState,
            onSelectPeriod = { viewModel.selectPeriod(it) },
            onOpenSetBudget = { showSetBudgetDialog = true },
            onDeleteBudget = { viewModel.deleteBudget(it) },
            modifier = modifier,
        )

        if (showSetBudgetDialog && uiState is ReportsUiState.Success) {
            val success = uiState as ReportsUiState.Success
            SetBudgetDialog(
                categories = success.expenseCategories,
                onDismiss = { showSetBudgetDialog = false },
                onConfirm = { catId, limit ->
                    viewModel.setBudget(catId, limit)
                    showSetBudgetDialog = false
                },
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
    when (uiState) {
        is ReportsUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is ReportsUiState.Success -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                item {
                    Text(
                        text = "Báo cáo tài chính",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Filter Chips theo kỳ
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReportPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = uiState.selectedPeriod == period,
                                onClick = { onSelectPeriod(period) },
                                label = { Text(period.title) },
                            )
                        }
                    }
                }

                // Card tổng quan: Thu / Chi / Tiết kiệm
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Dòng tiền trong kỳ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = "Tổng thu",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = CurrencyUtils.formatVnd(uiState.totalIncome),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeDark,
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Tổng chi",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = CurrencyUtils.formatVnd(uiState.totalExpense),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseDark,
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Tiết kiệm ròng",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val savingsColor = if (uiState.netSavings >= 0L) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                                Text(
                                    text = CurrencyUtils.formatVnd(uiState.netSavings),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = savingsColor,
                                )
                            }
                        }
                    }
                }

                // Mục Ngân sách tháng này (FR-BD-01, FR-BD-02, FR-BD-03)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            Text("+ Thiết lập")
                        }
                    }
                }

                if (uiState.budgetUsages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Chưa có ngân sách nào cho tháng này.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Bấm '+ Thiết lập' để đặt hạn mức chi tiêu theo danh mục.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.budgetUsages, key = { it.budgetId }) { usage ->
                        BudgetUsageItem(
                            item = usage,
                            onDelete = { onDeleteBudget(usage.budgetId) },
                        )
                    }
                }

                // Tiêu đề Chi tiêu theo danh mục
                item {
                    Text(
                        text = "Phân bổ chi tiêu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                if (uiState.categorySpendings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Chưa có khoản chi tiêu nào trong ${uiState.selectedPeriod.title.lowercase(Locale.getDefault())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(uiState.categorySpendings, key = { it.categoryId }) { item ->
                        CategorySpendingItem(item = item)
                    }
                }
            }
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
        Column(modifier = Modifier.padding(14.dp)) {
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
        Column(modifier = Modifier.padding(14.dp)) {
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
                            color = ExpenseDark,
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
                    supportingText = limitError?.let { { Text(it) } },
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
