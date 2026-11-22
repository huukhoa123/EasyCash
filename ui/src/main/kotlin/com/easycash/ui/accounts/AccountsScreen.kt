package com.easycash.ui.accounts

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import com.easycash.ui.components.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import com.easycash.domain.Account
import com.easycash.domain.AccountType

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
                accounts = emptyList<Account>(),
                totalEstimatedBalance = emptyList<Account>().sumOf { it.estimatedBalance },
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
                    text = { Text("Thêm tài khoản") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ScreenHeader("Tài khoản", "Tiền của bạn, ở một nơi.") }
            when (uiState) {
                AccountsUiState.Loading -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                AccountsUiState.Empty -> item {
                    EmptyPanel("Tạo tài khoản đầu tiên", "Nhập số dư hiện tại của ngân hàng, ví hoặc tiền mặt để bắt đầu theo dõi.",
                        Glyph.Wallet, "Thêm tài khoản", onAddClick)
                }
                is AccountsUiState.Success -> {
                    item {
                        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Số dư ước tính", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(CurrencyUtils.formatVnd(uiState.totalEstimatedBalance), style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${uiState.accounts.size} tài khoản đang hoạt động", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    if (uiState.accounts.isEmpty()) {
                        item { EmptyPanel("Chưa có tài khoản", "Thêm tài khoản để bắt đầu ghi nhận thu và chi.", Glyph.Wallet) }
                    } else {
                        item { Text("Tài khoản của bạn", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
                        items(uiState.accounts, key = { it.id }) { account ->
                            AccountItemCard(account, { onEditClick(account) }, { onArchiveClick(account.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountItemCard(
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
    Card(onClick = onEdit, modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    when (account.type) {
                        AccountType.CASH -> Glyph.Cash
                        AccountType.BANK -> Glyph.Bank
                        AccountType.EWALLET -> Glyph.Wallet
                    }
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(typeLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(account.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                        AppIcon(Glyph.More, "Tùy chọn tài khoản")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Sửa tài khoản") }, onClick = { showMenu = false; onEdit() })
                        DropdownMenuItem(text = { Text("Ẩn tài khoản") }, onClick = { showMenu = false; onArchive() })
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(CurrencyUtils.formatVnd(account.estimatedBalance), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(3.dp))
            Text("Số dư ước tính", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        title = { Text("Thêm tài khoản") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên tài khoản") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(text = "Loại tài khoản:", style = MaterialTheme.typography.labelMedium)
                Column {
                    AccountType.entries.forEach { type ->
                        val label = when (type) {
                            AccountType.CASH -> "Tiền mặt"
                            AccountType.BANK -> "Ngân hàng"
                            AccountType.EWALLET -> "Ví điện tử"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                            )
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it.filter { char -> char.isDigit() } },
                    label = { Text("Số dư hiện tại (₫)") },
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
                        onConfirm(name, selectedType, balance)
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
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(text = "Loại tài khoản:", style = MaterialTheme.typography.labelMedium)
                Column {
                    AccountType.entries.forEach { type ->
                        val label = when (type) {
                            AccountType.CASH -> "Tiền mặt"
                            AccountType.BANK -> "Ngân hàng"
                            AccountType.EWALLET -> "Ví điện tử"
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                            )
                            Text(text = label, style = MaterialTheme.typography.bodyMedium)
                        }
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

@Preview(name = "AccountsScreen Dark", showBackground = true)
@Composable
private fun AccountsScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        AccountsScreen()
    }
}
