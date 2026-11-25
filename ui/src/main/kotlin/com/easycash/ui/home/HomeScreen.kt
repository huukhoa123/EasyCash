package com.easycash.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.*
import com.easycash.ui.components.*
import com.easycash.ui.theme.*
import com.easycash.ui.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
                totalEstimatedBalance = 24_500_000L,
                totalIncome = 15_000_000L,
                totalExpense = 4_200_000L,
                recentTransactions = emptyList(),
                needsReviewCount = 0,
                accountsMap = mapOf(1L to "BIDV", 2L to "MoMo"),
                isAccessGranted = true,
            ),
            isBalanceHidden = false,
            onToggleBalanceVisibility = {},
            onNavigateToTransactions = onNavigateToTransactions,
            onNavigateToAccounts = onNavigateToAccounts,
            onNavigateToSettings = onNavigateToSettings,
            modifier = modifier,
        )
    } else {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val hidden by viewModel.isBalanceHidden.collectAsStateWithLifecycle()
        HomeContent(
            uiState = state,
            isBalanceHidden = hidden,
            onToggleBalanceVisibility = viewModel::toggleBalanceVisibility,
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
    if (uiState is HomeUiState.Loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    val state = uiState as HomeUiState.Success
    val isDark = MaterialTheme.colorScheme.background == CanvasDark

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1. Header thấp: logo EasyCash + tên màn "Sổ tiền", nút ẩn số tiền có nhãn
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(
                                text = "SỔ TIỀN",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Text(
                            text = "EasyCash",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = LocalDate.now().format(
                            DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.forLanguageTag("vi-VN"))
                        ).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onToggleBalanceVisibility,
                    modifier = Modifier.size(48.dp),
                ) {
                    AppIcon(
                        glyph = if (isBalanceHidden) Glyph.EyeOff else Glyph.Eye,
                        contentDescription = if (isBalanceHidden) "Hiện số dư" else "Ẩn số dư",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // 2. Vùng “Số dư ước tính” màu Butter, con số lớn, dòng số tài khoản nhỏ (UI_LEDGER_DIRECTION.md)
        item {
            val butterBg = if (isDark) LedgerButterDark else LedgerButterLight
            val butterBorder = if (isDark) LedgerButterBorderDark else LedgerButterBorderLight
            val onButter = if (isDark) LedgerOnButterDark else LedgerOnButterLight

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = butterBg,
                border = BorderStroke(1.dp, butterBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "SỐ DƯ ƯỚC TÍNH",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = onButter.copy(alpha = 0.75f),
                            letterSpacing = 0.8.sp,
                        )
                        Text(
                            text = "${state.accountsMap.size} tài khoản",
                            style = MaterialTheme.typography.labelSmall,
                            color = onButter.copy(alpha = 0.8f),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (isBalanceHidden) "•••••••• ₫" else CurrencyUtils.formatVnd(state.totalEstimatedBalance),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = onButter,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tổng hợp nội bộ trên máy · Không kết nối internet",
                        style = MaterialTheme.typography.bodySmall,
                        color = onButter.copy(alpha = 0.65f),
                    )
                }
            }
        }

        // 3. Hai ô Thu / Chi tháng cùng hàng: Mint / Rose (UI_LEDGER_DIRECTION.md)
        item {
            val mintBg = if (isDark) LedgerMintDark else LedgerMintLight
            val mintBorder = if (isDark) LedgerMintBorderDark else LedgerMintBorderLight
            val onMint = if (isDark) LedgerOnMintDark else LedgerOnMintLight

            val roseBg = if (isDark) LedgerRoseDark else LedgerRoseLight
            val roseBorder = if (isDark) LedgerRoseBorderDark else LedgerRoseBorderLight
            val onRose = if (isDark) LedgerOnRoseDark else LedgerOnRoseLight

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Ô Thu (Mint)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = mintBg,
                    border = BorderStroke(1.dp, mintBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 84.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Thu tháng này (+)",
                            style = MaterialTheme.typography.labelSmall,
                            color = onMint.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isBalanceHidden) "•••• ₫" else "+${CurrencyUtils.formatVnd(state.totalIncome)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onMint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Ô Chi (Rose)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = roseBg,
                    border = BorderStroke(1.dp, roseBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 84.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Chi tháng này (−)",
                            style = MaterialTheme.typography.labelSmall,
                            color = onRose.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (isBalanceHidden) "•••• ₫" else "−${CurrencyUtils.formatVnd(state.totalExpense)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = onRose,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        // 4. “Ví của bạn”: tối đa 2 tài khoản đầu dạng lưới và nút “Xem tất cả” (UI_LEDGER_DIRECTION.md)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ví của bạn",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(
                    onClick = onNavigateToAccounts,
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Text(
                        text = "Xem tất cả",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item {
            val accountsList = state.accountsMap.entries.take(2).toList()
            if (accountsList.isEmpty()) {
                Surface(
                    onClick = onNavigateToAccounts,
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(Glyph.Wallet, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Chưa có tài khoản ví",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Bấm để thêm tài khoản ngân hàng hoặc tiền mặt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    accountsList.forEach { entry ->
                        Surface(
                            onClick = onNavigateToAccounts,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 72.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppIcon(
                                        glyph = Glyph.Wallet,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = entry.value,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Đang theo dõi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (accountsList.size == 1) {
                        Surface(
                            onClick = onNavigateToAccounts,
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 72.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                AppIcon(Glyph.Plus, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Thêm ví",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Banner xem lại có điều kiện (gọn gàng, không che màn hình)
        if (state.needsReviewCount > 0) {
            item {
                Surface(
                    onClick = onNavigateToTransactions,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(
                            glyph = Glyph.Transactions,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "${state.needsReviewCount} giao dịch cần xác nhận",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        AppIcon(
                            glyph = Glyph.Arrow,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // 6. “Dòng tiền gần đây”: danh sách phẳng chia ngày gợi sổ / cuống hóa đơn (UI_LEDGER_DIRECTION.md)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Dòng tiền gần đây",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                TextButton(
                    onClick = onNavigateToTransactions,
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Text(
                        text = "Tất cả",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (state.recentTransactions.isEmpty()) {
            item {
                EmptyPanel(
                    title = "Chưa có phát sinh chi tiêu",
                    description = "Ghi lại khoản chi tiêu hoặc thu nhập đầu tiên vào sổ tiền cá nhân.",
                    glyph = Glyph.Transactions,
                    action = if (state.accountsMap.isEmpty()) "Thêm tài khoản" else "Ghi giao dịch",
                    onAction = if (state.accountsMap.isEmpty()) onNavigateToAccounts else onNavigateToTransactions,
                )
            }
        } else {
            // Mẫu trang sổ / hóa đơn phẳng (Ledger Sheet Container)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        state.recentTransactions.forEachIndexed { index, transaction ->
                            val account = state.accountsMap[transaction.accountId] ?: "Tài khoản"
                            val label = if (transaction.type == TransactionType.TRANSFER) {
                                "$account → ${state.accountsMap[transaction.toAccountId] ?: "Tài khoản"}"
                            } else {
                                account
                            }
                            LedgerTransactionRow(
                                transaction = transaction,
                                accountName = label,
                                onClick = onNavigateToTransactions,
                            )
                            if (index < state.recentTransactions.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Hàng thông báo quyền gọn nhẹ (nếu chưa cấp)
        if (!state.isAccessGranted) {
            item {
                Surface(
                    onClick = onNavigateToSettings,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(Glyph.Shield, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Tự động nhận diện thông báo",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Bật quyền đọc thông báo để ghi tự động.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        AppIcon(
                            glyph = Glyph.Arrow,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hàng giao dịch phẳng gợi cuống hóa đơn / dòng sổ (UI_LEDGER_DIRECTION.md)
 * Sửa lỗi P2: Tên tài khoản và ngày được phân tách và chia width hợp lý, không đẩy ngày ra ngoài.
 */
@Composable
private fun LedgerTransactionRow(
    transaction: Transaction,
    accountName: String,
    onClick: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background == CanvasDark
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isTransfer = transaction.type == TransactionType.TRANSFER
    val color = when {
        isTransfer -> if (isDark) LedgerOnSlateDark else LedgerOnSlateLight
        isExpense -> if (isDark) ExpenseDark else ExpenseLight
        else -> if (isDark) IncomeDark else IncomeLight
    }
    val glyph = when {
        isTransfer -> Glyph.Transfer
        isExpense -> Glyph.Expense
        else -> Glyph.Income
    }
    val dateFormat = remember { SimpleDateFormat("dd/MM · HH:mm", Locale.forLanguageTag("vi-VN")) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(glyph = glyph, tint = color, modifier = Modifier.size(36.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = transaction.note.ifBlank {
                    when (transaction.type) {
                        TransactionType.EXPENSE -> "Chi tiêu"
                        TransactionType.INCOME -> "Thu nhập"
                        TransactionType.TRANSFER -> "Chuyển tiền nội bộ"
                        TransactionType.REFUND -> "Hoàn tiền"
                        TransactionType.ADJUSTMENT -> "Điều chỉnh"
                    }
                },
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Sửa lỗi P2: accountName có weight(1f, fill=false) và ngày có chỗ riêng biệt không bao giờ bị cắt
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = " · ${dateFormat.format(Date(transaction.occurredAtMillis))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isTransfer) {
                CurrencyUtils.formatVnd(transaction.amount)
            } else {
                CurrencyUtils.formatVndWithSign(transaction.amount, isExpense)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Preview(name = "HomeScreen Light")
@Composable
private fun HomeScreenLightPreview() {
    EasyCashTheme(darkTheme = false) {
        HomeScreen()
    }
}

@Preview(name = "HomeScreen Dark")
@Composable
private fun HomeScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        HomeScreen()
    }
}
