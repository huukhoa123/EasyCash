package com.easycash.ui.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.Account
import com.easycash.domain.Category
import com.easycash.domain.CategoryType
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
fun TransactionsScreen(
    viewModel: TransactionsViewModel? = null,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    if (viewModel == null) {
        TransactionsContent(
            uiState = TransactionsUiState.Success(
                transactions = emptyList(),
                accounts = emptyList(),
                categories = emptyList(),
                selectedType = null,
                selectedStatus = null,
                keyword = "",
            ),
            onKeywordChange = {},
            onTypeSelect = {},
            onStatusSelect = {},
            onAddClick = { showAddDialog = true },
            onEditClick = {},
            onDeleteClick = {},
            onConfirmClick = {},
            onRejectClick = {},
            modifier = modifier,
        )
    } else {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val writeError by viewModel.writeError.collectAsStateWithLifecycle()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        TransactionsContent(
            uiState = uiState,
            snackbarHostState = snackbarHostState,
            onKeywordChange = { viewModel.setKeyword(it) },
            onTypeSelect = { viewModel.setSelectedType(it) },
            onStatusSelect = { viewModel.setSelectedStatus(it) },
            onAddClick = { showAddDialog = true },
            onEditClick = { editingTransaction = it },
            onDeleteClick = { txId ->
                val txToDelete = (uiState as? TransactionsUiState.Success)?.transactions?.find { it.id == txId }
                viewModel.deleteTransaction(txId)
                if (txToDelete != null) {
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Đã xoá: ${txToDelete.note.ifBlank { "Giao dịch" }}",
                            actionLabel = "Hoàn tác",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreTransaction(txToDelete)
                        }
                    }
                }
            },
            onConfirmClick = { viewModel.confirmTransaction(it) },
            onRejectClick = { viewModel.rejectTransaction(it) },
            modifier = modifier,
        )

        if (writeError) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissWriteError() },
                title = { Text("Lỗi thao tác") },
                text = { Text("Không thể lưu hoặc xoá giao dịch. Vui lòng kiểm tra lại.") },
                confirmButton = {
                    Button(onClick = { viewModel.dismissWriteError() }) {
                        Text("Đóng")
                    }
                },
            )
        }

        if (showAddDialog && uiState is TransactionsUiState.Success) {
            val success = uiState as TransactionsUiState.Success
            AddTransactionDialog(
                accounts = success.accounts,
                categories = success.categories,
                onDismiss = { showAddDialog = false },
                onConfirm = { accId, toAccId, catId, type, amount, note ->
                    viewModel.addTransaction(
                        accountId = accId,
                        toAccountId = toAccId,
                        categoryId = catId,
                        type = type,
                        amount = amount,
                        note = note,
                        onSuccess = { showAddDialog = false },
                    )
                },
            )
        }

        editingTransaction?.let { txToEdit ->
            if (uiState is TransactionsUiState.Success) {
                val success = uiState as TransactionsUiState.Success
                EditTransactionDialog(
                    transaction = txToEdit,
                    accounts = success.accounts,
                    categories = success.categories,
                    onDismiss = { editingTransaction = null },
                    onConfirm = { accId, toAccId, catId, amount, note ->
                        viewModel.updateTransaction(
                            txToEdit.copy(
                                accountId = accId,
                                toAccountId = toAccId,
                                categoryId = catId,
                                amount = amount,
                                note = note,
                                status = TransactionStatus.USER_CONFIRMED,
                            ),
                            onSuccess = { editingTransaction = null },
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun TransactionsContent(
    uiState: TransactionsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onKeywordChange: (String) -> Unit,
    onTypeSelect: (TransactionType?) -> Unit,
    onStatusSelect: (TransactionStatus?) -> Unit = {},
    onAddClick: () -> Unit,
    onEditClick: (Transaction) -> Unit = {},
    onDeleteClick: (Long) -> Unit,
    onConfirmClick: (Long) -> Unit,
    onRejectClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            is TransactionsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is TransactionsUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = uiState.keyword,
                        onValueChange = onKeywordChange,
                        placeholder = { Text("Tìm kiếm ghi chú giao dịch...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    // Type filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = uiState.selectedType == null && uiState.selectedStatus == null,
                            onClick = {
                                onTypeSelect(null)
                                onStatusSelect(null)
                            },
                            label = { Text("Tất cả") },
                        )
                        FilterChip(
                            selected = uiState.selectedStatus == TransactionStatus.NEEDS_REVIEW,
                            onClick = {
                                if (uiState.selectedStatus == TransactionStatus.NEEDS_REVIEW) {
                                    onStatusSelect(null)
                                } else {
                                    onStatusSelect(TransactionStatus.NEEDS_REVIEW)
                                }
                            },
                            label = { Text("Chờ xem lại") },
                        )
                        FilterChip(
                            selected = uiState.selectedType == TransactionType.EXPENSE,
                            onClick = { onTypeSelect(TransactionType.EXPENSE) },
                            label = { Text("Chi tiêu") },
                        )
                        FilterChip(
                            selected = uiState.selectedType == TransactionType.INCOME,
                            onClick = { onTypeSelect(TransactionType.INCOME) },
                            label = { Text("Thu nhập") },
                        )
                        FilterChip(
                            selected = uiState.selectedType == TransactionType.TRANSFER,
                            onClick = { onTypeSelect(TransactionType.TRANSFER) },
                            label = { Text("Chuyển tiền") },
                        )
                    }

                    if (uiState.transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (uiState.keyword.isNotBlank() || uiState.selectedType != null)
                                    "Không tìm thấy giao dịch phù hợp"
                                else
                                    "Chưa có giao dịch nào",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        val accountsMap = remember(uiState.accounts) {
                            uiState.accounts.associate { it.id to it.name }
                        }
                        val categoriesMap = remember(uiState.categories) {
                            uiState.categories.associate { it.id to it.name }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(uiState.transactions, key = { it.id }) { tx ->
                                val accountLabel = if (tx.type == TransactionType.TRANSFER && tx.toAccountId != null) {
                                    val from = accountsMap[tx.accountId] ?: "TK"
                                    val to = accountsMap[tx.toAccountId] ?: "TK"
                                    "$from → $to"
                                } else {
                                    accountsMap[tx.accountId] ?: "Tài khoản"
                                }
                                TransactionItemCard(
                                    transaction = tx,
                                    accountName = accountLabel,
                                    categoryName = tx.categoryId?.let { categoriesMap[it] },
                                    onEdit = { onEditClick(tx) },
                                    onDelete = { onDeleteClick(tx.id) },
                                    onConfirm = { onConfirmClick(tx.id) },
                                    onReject = { onRejectClick(tx.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionItemCard(
    transaction: Transaction,
    accountName: String,
    categoryName: String? = null,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.getDefault()) }
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

    val subtitle = remember(categoryName, accountName, timeString) {
        listOfNotNull(categoryName, accountName, timeString).joinToString(" · ")
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = transaction.note.ifBlank { "Giao dịch" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = CurrencyUtils.formatVndWithSign(transaction.amount, isExpense = isExpense),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                if (transaction.status == TransactionStatus.NEEDS_REVIEW) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Chờ xem lại", style = MaterialTheme.typography.labelSmall, color = WarningDark) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                } else if (transaction.status == TransactionStatus.REJECTED) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Đã từ chối", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                } else if (transaction.origin == TransactionOrigin.NOTIFICATION) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Tự động", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }

                if (transaction.status == TransactionStatus.NEEDS_REVIEW || transaction.status == TransactionStatus.AUTO_RECORDED) {
                    SuggestionChip(
                        onClick = onConfirm,
                        label = { Text("Xác nhận", style = MaterialTheme.typography.labelSmall, color = WarningDark) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    SuggestionChip(
                        onClick = onReject,
                        label = { Text("Từ chối", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }

                TextButton(onClick = onEdit) {
                    Text("Sửa")
                }

                TextButton(onClick = onDelete) {
                    Text("Xoá", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, toAccountId: Long?, categoryId: Long?, type: TransactionType, amount: Long, note: String) -> Unit,
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: 1L) }
    var selectedToAccountId by remember {
        mutableStateOf(accounts.getOrNull(1)?.id ?: accounts.firstOrNull()?.id ?: 1L)
    }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }

    val isTransfer = selectedType == TransactionType.TRANSFER
    val relevantCategories = remember(categories, selectedType) {
        val targetType = when (selectedType) {
            TransactionType.INCOME -> CategoryType.INCOME
            else -> CategoryType.EXPENSE
        }
        categories.filter { it.type == targetType }
    }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }

    val transferAccountError = if (isTransfer && accounts.size >= 2 && selectedAccountId == selectedToAccountId) {
        "Tài khoản nhận phải khác tài khoản gửi"
    } else if (isTransfer && accounts.size < 2) {
        "Cần tối thiểu 2 tài khoản để chuyển tiền"
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isTransfer) "Chuyển tiền nội bộ" else "Thêm giao dịch") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Type selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedType = TransactionType.EXPENSE },
                        label = { Text("Chi tiêu") },
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        label = { Text("Thu nhập") },
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.TRANSFER,
                        onClick = { selectedType = TransactionType.TRANSFER },
                        label = { Text("Chuyển tiền") },
                    )
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        val digits = it.filter { char -> char.isDigit() }
                        amountText = digits
                        amountError = when {
                            digits.isEmpty() -> "Số tiền không được rỗng"
                            digits.toLongOrNull() == null -> "Số tiền vượt giới hạn"
                            digits.toLong() <= 0L -> "Số tiền phải lớn hơn 0"
                            else -> null
                        }
                    },
                    label = { Text("Số tiền (VNĐ)") },
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isTransfer) "Ghi chú (vd: Nạp ví, Rút tiền)" else "Ghi chú (vd: Ăn trưa, Lương)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!isTransfer && relevantCategories.isNotEmpty()) {
                    Text(text = "Danh mục:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Không") },
                        )
                        relevantCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                            )
                        }
                    }
                }

                if (accounts.isNotEmpty()) {
                    Text(
                        text = if (isTransfer) "Từ tài khoản (Nguồn):" else "Tài khoản:",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    accounts.forEach { acc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                            )
                            Text(text = acc.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (isTransfer) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Đến tài khoản (Đích):", style = MaterialTheme.typography.labelMedium)
                        accounts.forEach { acc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                RadioButton(
                                    selected = selectedToAccountId == acc.id,
                                    onClick = { selectedToAccountId = acc.id },
                                )
                                Text(text = acc.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        transferAccountError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull()
                    if (amount != null && amount > 0L) {
                        onConfirm(
                            selectedAccountId,
                            if (isTransfer) selectedToAccountId else null,
                            if (!isTransfer) selectedCategoryId else null,
                            selectedType,
                            amount,
                            note,
                        )
                    }
                },
                enabled = amountText.isNotBlank() &&
                    amountError == null &&
                    accounts.isNotEmpty() &&
                    transferAccountError == null,
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

@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long, toAccountId: Long?, categoryId: Long?, amount: Long, note: String) -> Unit,
) {
    var selectedAccountId by remember { mutableStateOf(transaction.accountId) }
    var selectedToAccountId by remember {
        mutableStateOf(transaction.toAccountId ?: accounts.getOrNull(1)?.id ?: transaction.accountId)
    }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var note by remember { mutableStateOf(transaction.note) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val isTransfer = transaction.type == TransactionType.TRANSFER
    val relevantCategories = remember(categories, transaction.type) {
        val targetType = when (transaction.type) {
            TransactionType.INCOME -> CategoryType.INCOME
            else -> CategoryType.EXPENSE
        }
        categories.filter { it.type == targetType }
    }

    val transferAccountError = if (isTransfer && accounts.size >= 2 && selectedAccountId == selectedToAccountId) {
        "Tài khoản nhận phải khác tài khoản gửi"
    } else if (isTransfer && accounts.size < 2) {
        "Cần tối thiểu 2 tài khoản để chuyển tiền"
    } else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chỉnh sửa giao dịch") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        val digits = it.filter { char -> char.isDigit() }
                        amountText = digits
                        amountError = when {
                            digits.isEmpty() -> "Số tiền không được rỗng"
                            digits.toLongOrNull() == null -> "Số tiền vượt giới hạn"
                            digits.toLong() <= 0L -> "Số tiền phải lớn hơn 0"
                            else -> null
                        }
                    },
                    label = { Text("Số tiền (VNĐ)") },
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!isTransfer && relevantCategories.isNotEmpty()) {
                    Text(text = "Danh mục:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Không") },
                        )
                        relevantCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                            )
                        }
                    }
                }

                if (accounts.isNotEmpty()) {
                    Text(
                        text = if (isTransfer) "Từ tài khoản (Nguồn):" else "Tài khoản:",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    accounts.forEach { acc ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            RadioButton(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                            )
                            Text(text = acc.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (isTransfer) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Đến tài khoản (Đích):", style = MaterialTheme.typography.labelMedium)
                        accounts.forEach { acc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                RadioButton(
                                    selected = selectedToAccountId == acc.id,
                                    onClick = { selectedToAccountId = acc.id },
                                )
                                Text(text = acc.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        transferAccountError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull()
                    if (amount != null && amount > 0L) {
                        onConfirm(
                            selectedAccountId,
                            if (isTransfer) selectedToAccountId else null,
                            if (!isTransfer) selectedCategoryId else null,
                            amount,
                            note,
                        )
                    }
                },
                enabled = amountText.isNotBlank() &&
                    amountError == null &&
                    accounts.isNotEmpty() &&
                    transferAccountError == null,
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

@Preview(name = "TransactionsScreen Dark", showBackground = true)
@Composable
private fun TransactionsScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        TransactionsScreen()
    }
}
