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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(text = "+", style = MaterialTheme.typography.titleLarge)
            }
        },
    ) { padding ->
        when (uiState) {
            is AccountsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is AccountsUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Chưa có tài khoản nào",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onAddClick) {
                            Text("Thêm tài khoản đầu tiên")
                        }
                    }
                }
            }

            is AccountsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
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
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = CurrencyUtils.formatVnd(uiState.totalEstimatedBalance),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Tài khoản & Ví (${uiState.accounts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    items(uiState.accounts, key = { it.id }) { account ->
                        AccountItemCard(
                            account = account,
                            onEdit = { onEditClick(account) },
                            onArchive = { onArchiveClick(account.id) },
                        )
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
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val typeLabel = when (account.type) {
                        AccountType.CASH -> "Tiền mặt"
                        AccountType.BANK -> "Ngân hàng"
                        AccountType.EWALLET -> "Ví điện tử"
                    }
                    SuggestionChip(
                        onClick = {},
                        label = { Text(typeLabel, style = MaterialTheme.typography.labelSmall) },
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = CurrencyUtils.formatVnd(account.estimatedBalance),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onEdit) {
                        Text("Sửa")
                    }
                    TextButton(onClick = onArchive) {
                        Text("Ẩn", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
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
                    label = { Text("Tên tài khoản (vd: BIDV, MoMo)") },
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
                    label = { Text("Số dư ban đầu (VNĐ)") },
                    isError = balanceText.isNotBlank() && balanceText.toLongOrNull() == null,
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
