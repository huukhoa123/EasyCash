package com.easycash.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.*
import com.easycash.ui.components.*
import com.easycash.ui.theme.EasyCashTheme
import com.easycash.ui.util.CurrencyUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun HomeScreen(
    viewModel: HomeViewModel? = null,
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (viewModel == null) {
        HomeContent(HomeUiState.Success(0, 0, 0, emptyList(), 0, emptyMap(), false), false, {},
            onNavigateToTransactions, onNavigateToAccounts, onNavigateToSettings, modifier)
    } else {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val hidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
        HomeContent(state, hidden, viewModel::toggleBalanceVisibility, onNavigateToTransactions,
            onNavigateToAccounts, onNavigateToSettings, modifier)
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
    if (uiState is HomeUiState.Loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val state = uiState as HomeUiState.Success
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            ScreenHeader("Tổng quan", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.forLanguageTag("vi"))))
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(
                    Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    ))).padding(24.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Số dư ước tính", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = onToggleBalanceVisibility, modifier = Modifier.size(48.dp)) {
                            AppIcon(
                                if (isBalanceHidden) Glyph.EyeOff else Glyph.Eye,
                                if (isBalanceHidden) "Hiện số dư" else "Ẩn số dư",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(if (isBalanceHidden) "•••••••• ₫" else CurrencyUtils.formatVnd(state.totalEstimatedBalance),
                        style = MaterialTheme.typography.displaySmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(Glyph.Wallet, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${state.accountsMap.size} tài khoản đang theo dõi", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowCard("Tổng thu", state.totalIncome, isBalanceHidden, Glyph.Income, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                FlowCard("Tổng chi", state.totalExpense, isBalanceHidden, Glyph.Expense, MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = onNavigateToTransactions, modifier = Modifier.weight(1f).height(52.dp)) {
                    AppIcon(Glyph.Transactions, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Giao dịch")
                }
                OutlinedButton(onClick = onNavigateToAccounts, modifier = Modifier.weight(1f).height(52.dp)) {
                    AppIcon(Glyph.Wallet, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Tài khoản")
                }
            }
        }
        if (state.needsReviewCount > 0) {
            item {
                Card(onClick = onNavigateToTransactions, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Glyph.Transactions, MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${state.needsReviewCount} giao dịch cần xác nhận", style = MaterialTheme.typography.titleSmall)
                            Text("Kiểm tra trước khi tính vào số dư", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AppIcon(Glyph.Arrow)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Giao dịch gần đây", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onNavigateToTransactions) { Text("Tất cả") }
            }
        }
        if (state.recentTransactions.isEmpty()) {
            item {
                EmptyPanel("Bắt đầu theo dõi chi tiêu", "Tạo tài khoản và ghi lại giao dịch đầu tiên của bạn.",
                    Glyph.Transactions, if (state.accountsMap.isEmpty()) "Thêm tài khoản" else "Ghi giao dịch",
                    if (state.accountsMap.isEmpty()) onNavigateToAccounts else onNavigateToTransactions)
            }
        } else {
            items(state.recentTransactions, key = { it.id }) { transaction ->
                val account = state.accountsMap[transaction.accountId] ?: "Tài khoản"
                val label = if (transaction.type == TransactionType.TRANSFER)
                    "$account → ${state.accountsMap[transaction.toAccountId] ?: "Tài khoản"}" else account
                RecentTransactionItem(transaction, label)
            }
        }
        if (!state.isAccessGranted) {
            item {
                Card(onClick = onNavigateToSettings, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Glyph.Shield)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Tự động ghi nhận", style = MaterialTheme.typography.titleSmall)
                            Text("Kết nối thông báo ngân hàng", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AppIcon(Glyph.Arrow, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowCard(label: String, amount: Long, hidden: Boolean, glyph: Glyph, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIcon(glyph, tint = color, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (hidden) "•••• ₫" else CurrencyUtils.formatVnd(amount), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecentTransactionItem(transaction: Transaction, accountName: String) {
    val expense = transaction.type == TransactionType.EXPENSE
    val color = if (expense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val format = remember { SimpleDateFormat("dd/MM · HH:mm", Locale.forLanguageTag("vi")) }
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(if (transaction.type == TransactionType.TRANSFER) Glyph.Transactions else if (expense) Glyph.Expense else Glyph.Income, color)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(transaction.note.ifBlank { when (transaction.type) {
                    TransactionType.EXPENSE -> "Chi tiêu"
                    TransactionType.INCOME -> "Thu nhập"
                    TransactionType.TRANSFER -> "Chuyển tiền"
                    TransactionType.REFUND -> "Hoàn tiền"
                    TransactionType.ADJUSTMENT -> "Điều chỉnh"
                } }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(format.format(Date(transaction.occurredAtMillis)), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(CurrencyUtils.formatVndWithSign(transaction.amount, expense), style = MaterialTheme.typography.titleSmall, color = color)
        }
    }
}

@Preview
@Composable
private fun HomeScreenDarkPreview() { EasyCashTheme { HomeScreen() } }