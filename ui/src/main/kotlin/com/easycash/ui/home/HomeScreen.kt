package com.easycash.ui.home

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.Transaction
import com.easycash.domain.TransactionOrigin
import com.easycash.domain.TransactionStatus
import com.easycash.domain.TransactionType
import com.easycash.ui.theme.EasyCashTheme
import com.easycash.ui.theme.ExpenseDark
import com.easycash.ui.theme.IncomeDark
import com.easycash.ui.theme.WarningDark
import com.easycash.ui.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel? = null,
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (viewModel == null) {
        HomeContent(
            uiState = HomeUiState.Success(
                totalEstimatedBalance = 8_350_000L,
                totalIncome = 15_000_000L,
                totalExpense = 6_650_000L,
                recentTransactions = emptyList(),
                needsReviewCount = 0,
                accountsMap = emptyMap(),
                isAccessGranted = false,
            ),
            isBalanceHidden = false,
            onToggleBalanceVisibility = {},
            onNavigateToTransactions = onNavigateToTransactions,
            onNavigateToAccounts = onNavigateToAccounts,
            onNavigateToSettings = onNavigateToSettings,
            modifier = modifier,
        )
    } else {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val isBalanceHidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()

        HomeContent(
            uiState = uiState,
            isBalanceHidden = isBalanceHidden,
            onToggleBalanceVisibility = { viewModel.toggleBalanceVisibility() },
            onNavigateToTransactions = onNavigateToTransactions,
            onNavigateToAccounts = onNavigateToAccounts,
            onNavigateToSettings = onNavigateToSettings,
            modifier = modifier,
        )
    }
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    isBalanceHidden: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is HomeUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is HomeUiState.Success -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Card 1: Tổng số dư ước tính
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tổng tài sản",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "ước tính",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val balanceDisplay = if (isBalanceHidden) {
                                    "•••••••• ₫"
                                } else {
                                    CurrencyUtils.formatVnd(uiState.totalEstimatedBalance)
                                }
                                Text(
                                    text = balanceDisplay,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )

                                TextButton(onClick = onToggleBalanceVisibility) {
                                    Text(if (isBalanceHidden) "Hiện" else "Ẩn")
                                }
                            }
                        }
                    }
                }

                // Row 2: Thẻ thu và chi
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Tổng thu",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBalanceHidden) "•••• ₫" else CurrencyUtils.formatVnd(uiState.totalIncome),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeDark,
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Tổng chi",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBalanceHidden) "•••• ₫" else CurrencyUtils.formatVnd(uiState.totalExpense),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseDark,
                                )
                            }
                        }
                    }
                }

                // Banner: Gợi ý cấp quyền thông báo nếu chưa cấp
                if (!uiState.isAccessGranted) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tự động nhận diện biến động",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "Cấp quyền đọc thông báo để EasyCash tự ghi nhận khi có biến động từ MoMo và BIDV.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = onNavigateToSettings) {
                                    Text("Cấp quyền")
                                }
                            }
                        }
                    }
                }

                // Banner: Cần xem lại nếu có
                if (uiState.needsReviewCount > 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Giao dịch cần xem lại",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = WarningDark,
                                    )
                                    Text(
                                        text = "${uiState.needsReviewCount} giao dịch tự động đang chờ bạn xác nhận",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = onNavigateToTransactions) {
                                    Text("Xem")
                                }
                            }
                        }
                    }
                }

                // Tiêu đề Giao dịch gần đây
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Giao dịch gần đây",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = onNavigateToTransactions) {
                            Text("Xem tất cả")
                        }
                    }
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Chưa có giao dịch gần đây",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(uiState.recentTransactions, key = { it.id }) { tx ->
                        val accountLabel = if (tx.type == TransactionType.TRANSFER && tx.toAccountId != null) {
                            val from = uiState.accountsMap[tx.accountId] ?: "TK"
                            val to = uiState.accountsMap[tx.toAccountId] ?: "TK"
                            "$from → $to"
                        } else {
                            uiState.accountsMap[tx.accountId] ?: "Tài khoản"
                        }
                        RecentTransactionItem(
                            transaction = tx,
                            accountName = accountLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionItem(
    transaction: Transaction,
    accountName: String,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm · dd/MM", Locale.getDefault()) }
    val timeString = remember(transaction.occurredAtMillis) {
        dateFormat.format(Date(transaction.occurredAtMillis))
    }
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = when {
        isExpense -> ExpenseDark
        isIncome -> IncomeDark
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifBlank { "Giao dịch" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$accountName · $timeString",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = CurrencyUtils.formatVndWithSign(transaction.amount, isExpense = isExpense),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
        }
    }
}

@Preview(name = "HomeScreen Dark", showBackground = true)
@Composable
private fun HomeScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        HomeScreen()
    }
}
