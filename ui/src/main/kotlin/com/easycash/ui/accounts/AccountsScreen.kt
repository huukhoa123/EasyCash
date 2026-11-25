package com.easycash.ui.accounts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.Account
import com.easycash.domain.AccountType
import com.easycash.ui.components.*
import com.easycash.ui.theme.EasyCashTheme
import com.easycash.ui.util.CurrencyUtils

@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel? = null,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }

    if (viewModel == null) {
        AccountsContent(
            uiState = AccountsUiState.Success(
                accounts = listOf(
                    Account(1L, "Tiền mặt", AccountType.CASH, 2_500_000L, 2_500_000L, false),
                    Account(2L, "BIDV SmartBanking", AccountType.BANK, 18_000_000L, 18_000_000L, false),
                    Account(3L, "Ví MoMo", AccountType.EWALLET, 4_000_000L, 4_000_000L, false),
                ),
                totalEstimatedBalance = 24_500_000L,
            ),
            onAddClick = { showAddDialog = true },
            onEditClick = {},
            onArchiveClick = {},
            modifier = modifier,
        )
    } else {
        val writeError by viewModel.writeError.collectAsStateWithLifecycle()
        if (writeError) {
            AlertDialog(
                onDismissRequest = viewModel::dismissWriteError,
                text = { Text("Không thể lưu thay đổi. Vui lòng kiểm tra và thử lại.") },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissWriteError) { Text("Đóng") }
                },
            )
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        AccountsContent(
            uiState = uiState,
            onAddClick = { showAddDialog = true },
            onEditClick = { editingAccount = it },
            onArchiveClick = { viewModel.archiveAccount(it) },
            modifier = modifier,
        )

        if (showAddDialog) {
            AddAccountDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, type, balance ->
                    viewModel.addAccount(name, type, balance) { showAddDialog = false }
                },
            )
        }

        editingAccount?.let { accToEdit ->
            EditAccountDialog(
                account = accToEdit,
                onDismiss = { editingAccount = null },
                onConfirm = { newName, newType ->
                    viewModel.updateAccount(
                        accToEdit.copy(name = newName, type = newType),
                        onSuccess = { editingAccount = null },
                    )
                },
            )
        }
    }
}

@Composable
fun AccountsContent(
    uiState: AccountsUiState,
    onAddClick: () -> Unit,
    onEditClick: (Account) -> Unit = {},
    onArchiveClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (uiState is AccountsUiState.Success && uiState.accounts.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    icon = { AppIcon(Glyph.Plus) },
                    text = { Text("Thêm ví / tài khoản") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScreenHeader("Sổ tài khoản", "Quản lý ví tiền mặt và các nguồn tiền cá nhân.")
            }

            when (uiState) {
                AccountsUiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                AccountsUiState.Empty -> item {
                    EmptyPanel(
                        title = "Tạo ví đầu tiên",
                        description = "Nhập số dư hiện tại của ngân hàng, ví hoặc tiền mặt để ghi chép thu chi.",
                        glyph = Glyph.Wallet,
                        action = "Thêm tài khoản",
                        onAction = onAddClick,
                    )
                }
                is AccountsUiState.Success -> {
                    // Summary card at top (ledger style)
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
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "TỔNG SỐ DƯ CÁC VÍ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.6.sp,
                                )
                                Text(
                                    text = CurrencyUtils.formatVnd(uiState.totalEstimatedBalance),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "${uiState.accounts.size} ví đang hoạt động ngoại tuyến",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    if (uiState.accounts.isEmpty()) {
                        item {
                            EmptyPanel(
                                title = "Chưa có tài khoản",
                                description = "Thêm tài khoản để bắt đầu ghi nhận thu và chi.",
                                glyph = Glyph.Wallet,
                                action = "Thêm tài khoản",
                                onAction = onAddClick,
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "Lưới ví & nguồn tiền",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }

                        // Lưới 2 cột cho ví (UI_LEDGER_DIRECTION.md)
                        val chunked = uiState.accounts.chunked(2)
                        items(chunked.size) { rowIndex ->
                            val rowAccounts = chunked[rowIndex]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowAccounts.forEach { account ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        LedgerAccountCard(
                                            account = account,
                                            onEdit = { onEditClick(account) },
                                            onArchive = { onArchiveClick(account.id) },
                                        )
                                    }
                                }
                                if (rowAccounts.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerAccountCard(
    account: Account,
    onEdit: () -> Unit = {},
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val typeLabel = when (account.type) {
        AccountType.CASH -> "Tiền mặt"
        AccountType.BANK -> "Ngân hàng"
        AccountType.EWALLET -> "Ví điện tử"
    }
    val glyph = when (account.type) {
        AccountType.CASH -> Glyph.Cash
        AccountType.BANK -> Glyph.Bank
        AccountType.EWALLET -> Glyph.Wallet
    }

    Surface(
        onClick = onEdit,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(glyph = glyph, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp),
                    ) {
                        AppIcon(Glyph.More, "Tùy chọn ví", modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Sửa tài khoản") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Ẩn tài khoản") },
                            onClick = {
                                showMenu = false
                                onArchive()
                            },
                        )
                    }
                }
            }
            Column {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = CurrencyUtils.formatVnd(account.estimatedBalance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType, balance: Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var balanceText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm ví / tài khoản") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên tài khoản (vd: BIDV, Tiền mặt)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(text = "Loại tài khoản:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AccountType.entries.forEach { type ->
                        val label = when (type) {
                            AccountType.CASH -> "Tiền mặt"
                            AccountType.BANK -> "Ngân hàng"
                            AccountType.EWALLET -> "Ví điện tử"
                        }
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label) },
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it.filter { char -> char.isDigit() } },
                    label = { Text("Số dư ban đầu (₫)") },
                    shape = RoundedCornerShape(12.dp),
                    isError = balanceText.isNotBlank() && balanceText.toLongOrNull() == null,
                    supportingText = {
                        val parsed = balanceText.toLongOrNull()
                        if (balanceText.isNotBlank() && parsed == null) {
                            Text("Số tiền vượt giới hạn")
                        } else if (parsed != null && parsed > 0L) {
                            Text(CurrencyUtils.formatVnd(parsed), color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balance = balanceText.ifBlank { "0" }.toLongOrNull() ?: return@Button
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedType, balance)
                    }
                },
                enabled = name.isNotBlank() && balanceText.ifBlank { "0" }.toLongOrNull() != null,
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Huỷ")
            }
        },
    )
}

@Composable
fun EditAccountDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: AccountType) -> Unit,
) {
    var name by remember { mutableStateOf(account.name) }
    var selectedType by remember { mutableStateOf(account.type) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa tài khoản") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên tài khoản") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(text = "Loại tài khoản:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AccountType.entries.forEach { type ->
                        val label = when (type) {
                            AccountType.CASH -> "Tiền mặt"
                            AccountType.BANK -> "Ngân hàng"
                            AccountType.EWALLET -> "Ví điện tử"
                        }
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label) },
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedType)
                    }
                },
                enabled = name.isNotBlank(),
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

@Preview(name = "AccountsScreen Light", showBackground = true)
@Composable
private fun AccountsScreenLightPreview() {
    EasyCashTheme(darkTheme = false) {
        AccountsScreen()
    }
}

@Preview(name = "AccountsScreen Dark", showBackground = true)
@Composable
private fun AccountsScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        AccountsScreen()
    }
}
