package com.easycash.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.Account
import com.easycash.domain.Category
import com.easycash.domain.CategoryType
import com.easycash.domain.Transaction
import com.easycash.domain.TransactionStatus
import com.easycash.domain.TransactionType
import com.easycash.ui.components.*
import com.easycash.ui.theme.*
import com.easycash.ui.util.CategoryMatcher
import com.easycash.ui.util.CurrencyUtils
import kotlinx.coroutines.launch
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (uiState is TransactionsUiState.Success && uiState.transactions.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    AppIcon(Glyph.Plus, "Thêm giao dịch")
                }
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
                val hasNeedsReview = remember(uiState.transactions) {
                    uiState.transactions.any { it.status == TransactionStatus.NEEDS_REVIEW }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    ScreenHeader(
                        title = "Sổ giao dịch",
                        subtitle = "Nhật ký thu chi minh bạch và chính xác.",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )

                    // Sửa lỗi P2: placeholder chỉ tìm theo nội dung ghi chú vì Room repo chỉ tìm theo row.note
                    OutlinedTextField(
                        value = uiState.keyword,
                        onValueChange = onKeywordChange,
                        placeholder = { Text("Tìm theo nội dung ghi chú...") },
                        leadingIcon = { AppIcon(Glyph.Search, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (uiState.keyword.isNotEmpty()) {
                                IconButton(onClick = { onKeywordChange("") }) {
                                    AppIcon(Glyph.Close, "Xóa tìm kiếm", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                    )

                    // Type filter chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = uiState.selectedType == null && uiState.selectedStatus == null,
                            onClick = {
                                onTypeSelect(null)
                                onStatusSelect(null)
                            },
                            label = { Text("Tất cả") },
                            shape = RoundedCornerShape(10.dp),
                        )

                        if (hasNeedsReview || uiState.selectedStatus == TransactionStatus.NEEDS_REVIEW) {
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
                                shape = RoundedCornerShape(10.dp),
                            )
                        }

                        FilterChip(
                            selected = uiState.selectedType == TransactionType.EXPENSE,
                            onClick = { onTypeSelect(TransactionType.EXPENSE) },
                            label = { Text("Chi tiêu") },
                            shape = RoundedCornerShape(10.dp),
                        )
                        FilterChip(
                            selected = uiState.selectedType == TransactionType.INCOME,
                            onClick = { onTypeSelect(TransactionType.INCOME) },
                            label = { Text("Thu nhập") },
                            shape = RoundedCornerShape(10.dp),
                        )
                        FilterChip(
                            selected = uiState.selectedType == TransactionType.TRANSFER,
                            onClick = { onTypeSelect(TransactionType.TRANSFER) },
                            label = { Text("Chuyển tiền") },
                            shape = RoundedCornerShape(10.dp),
                        )
                    }

                    if (uiState.transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uiState.keyword.isNotBlank() || uiState.selectedType != null || uiState.selectedStatus != null) {
                                EmptyPanel(
                                    title = "Không tìm thấy giao dịch",
                                    description = "Không có ghi chú nào khớp với từ khóa tìm kiếm.",
                                    glyph = Glyph.Search,
                                    action = "Xóa bộ lọc",
                                    onAction = {
                                        onKeywordChange("")
                                        onTypeSelect(null)
                                        onStatusSelect(null)
                                    },
                                )
                            } else {
                                EmptyPanel(
                                    title = "Chưa có bản ghi giao dịch",
                                    description = "Ghi lại chi tiêu hoặc thu nhập đầu tiên vào sổ tiền.",
                                    glyph = Glyph.Transactions,
                                    action = "Thêm giao dịch",
                                    onAction = onAddClick,
                                )
                            }
                        }
                    } else {
                        val accountsMap = remember(uiState.accounts) {
                            uiState.accounts.associate { it.id to it.name }
                        }
                        val categoriesMap = remember(uiState.categories) {
                            uiState.categories.associate { it.id to it.name }
                        }

                        // Nhóm giao dịch theo ngày (gợi sổ / hóa đơn phẳng, không bao từng card viền riêng)
                        val grouped = remember(uiState.transactions) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            uiState.transactions.groupBy { tx ->
                                sdf.format(Date(tx.occurredAtMillis))
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            grouped.forEach { (dateStr, txList) ->
                                item(key = "header_$dateStr") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Ngày $dateStr",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = "${txList.size} giao dịch",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        )
                                    }
                                }

                                item(key = "sheet_$dateStr") {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            txList.forEachIndexed { index, tx ->
                                                val accountLabel = if (tx.type == TransactionType.TRANSFER && tx.toAccountId != null) {
                                                    val from = accountsMap[tx.accountId] ?: "TK"
                                                    val to = accountsMap[tx.toAccountId] ?: "TK"
                                                    "$from → $to"
                                                } else {
                                                    accountsMap[tx.accountId] ?: "Tài khoản"
                                                }

                                                LedgerTransactionRowItem(
                                                    transaction = tx,
                                                    accountName = accountLabel,
                                                    categoryName = tx.categoryId?.let { categoriesMap[it] },
                                                    onEdit = { onEditClick(tx) },
                                                    onDelete = { onDeleteClick(tx.id) },
                                                    onConfirm = { onConfirmClick(tx.id) },
                                                    onReject = { onRejectClick(tx.id) },
                                                )

                                                if (index < txList.size - 1) {
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
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hàng giao dịch phẳng trong phiếu sổ, không bao card viền riêng (UI_LEDGER_DIRECTION.md)
 */
@Composable
private fun LedgerTransactionRowItem(
    transaction: Transaction,
    accountName: String,
    categoryName: String? = null,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.forLanguageTag("vi-VN")) }
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
    var menuExpanded by remember { mutableStateOf(false) }
    val title = transaction.note.ifBlank {
        when (transaction.type) {
            TransactionType.EXPENSE -> "Chi tiêu"
            TransactionType.INCOME -> "Thu nhập"
            TransactionType.TRANSFER -> "Chuyển tiền nội bộ"
            TransactionType.REFUND -> "Hoàn tiền"
            TransactionType.ADJUSTMENT -> "Điều chỉnh"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(glyph = glyph, tint = color, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Sửa lỗi P2: Tên tài khoản chia weight hợp lý, không đẩy ngày giờ ra ngoài
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val subInfo = listOfNotNull(categoryName, accountName).joinToString(" · ")
                    Text(
                        text = subInfo,
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
            Column(horizontalAlignment = Alignment.End) {
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
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp),
                ) {
                    AppIcon(Glyph.More, "Tùy chọn giao dịch", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Sửa giao dịch") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa giao dịch") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
        }

        // Quick review actions if pending
        if (transaction.status == TransactionStatus.NEEDS_REVIEW || transaction.status == TransactionStatus.AUTO_RECORDED) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Xác nhận", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Từ chối", style = MaterialTheme.typography.labelMedium)
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
    var isCategoryManuallyPicked by remember { mutableStateOf(false) }

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
                        shape = RoundedCornerShape(10.dp),
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        label = { Text("Thu nhập") },
                        shape = RoundedCornerShape(10.dp),
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.TRANSFER,
                        onClick = { selectedType = TransactionType.TRANSFER },
                        label = { Text("Chuyển tiền") },
                        shape = RoundedCornerShape(10.dp),
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
                    shape = RoundedCornerShape(12.dp),
                    isError = amountError != null,
                    supportingText = {
                        if (amountError != null) {
                            Text(amountError!!)
                        } else if (amountText.isNotBlank() && amountText.toLongOrNull() != null) {
                            Text(CurrencyUtils.formatVnd(amountText.toLong()), color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { newNote ->
                        note = newNote
                        if (!isCategoryManuallyPicked && !isTransfer) {
                            val suggested = CategoryMatcher.suggestCategory(newNote, relevantCategories)
                            if (suggested != null) {
                                selectedCategoryId = suggested.id
                            }
                        }
                    },
                    label = { Text(if (isTransfer) "Ghi chú (vd: Nạp ví, Rút tiền)" else "Ghi chú (vd: Ăn trưa, Lương)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!isTransfer && relevantCategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Danh mục:", style = MaterialTheme.typography.labelMedium)
                        val suggested = if (!isCategoryManuallyPicked && note.isNotBlank()) {
                            CategoryMatcher.suggestCategory(note, relevantCategories)
                        } else null
                        if (suggested != null && selectedCategoryId == suggested.id) {
                            Text(
                                text = "Gợi ý: ${suggested.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = {
                                selectedCategoryId = null
                                isCategoryManuallyPicked = true
                            },
                            label = { Text("Không") },
                            shape = RoundedCornerShape(10.dp),
                        )
                        relevantCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    selectedCategoryId = cat.id
                                    isCategoryManuallyPicked = true
                                },
                                label = { Text(cat.name) },
                                shape = RoundedCornerShape(10.dp),
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
    var isCategoryManuallyPicked by remember { mutableStateOf(transaction.categoryId != null) }
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
                    shape = RoundedCornerShape(12.dp),
                    isError = amountError != null,
                    supportingText = {
                        if (amountError != null) {
                            Text(amountError!!)
                        } else if (amountText.isNotBlank() && amountText.toLongOrNull() != null) {
                            Text(CurrencyUtils.formatVnd(amountText.toLong()), color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { newNote ->
                        note = newNote
                        if (!isCategoryManuallyPicked && !isTransfer) {
                            val suggested = CategoryMatcher.suggestCategory(newNote, relevantCategories)
                            if (suggested != null) {
                                selectedCategoryId = suggested.id
                            }
                        }
                    },
                    label = { Text("Ghi chú") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!isTransfer && relevantCategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Danh mục:", style = MaterialTheme.typography.labelMedium)
                        val suggested = if (!isCategoryManuallyPicked && note.isNotBlank()) {
                            CategoryMatcher.suggestCategory(note, relevantCategories)
                        } else null
                        if (suggested != null && selectedCategoryId == suggested.id) {
                            Text(
                                text = "Gợi ý: ${suggested.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = {
                                selectedCategoryId = null
                                isCategoryManuallyPicked = true
                            },
                            label = { Text("Không") },
                            shape = RoundedCornerShape(10.dp),
                        )
                        relevantCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    selectedCategoryId = cat.id
                                    isCategoryManuallyPicked = true
                                },
                                label = { Text(cat.name) },
                                shape = RoundedCornerShape(10.dp),
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

@Preview(name = "TransactionsScreen Light", showBackground = true)
@Composable
private fun TransactionsScreenLightPreview() {
    EasyCashTheme(darkTheme = false) {
        TransactionsScreen()
    }
}

@Preview(name = "TransactionsScreen Dark", showBackground = true)
@Composable
private fun TransactionsScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        TransactionsScreen()
    }
}
