package com.easycash.ui.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.*
import com.easycash.ui.components.*
import com.easycash.ui.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemindersScreen(
    viewModel: RemindersViewModel? = null,
    modifier: Modifier = Modifier,
) {
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var deletingReminderId by remember { mutableStateOf<Long?>(null) }

    if (viewModel == null) {
        val sampleReminders = listOf(
            ReminderUiModel(
                reminder = Reminder(
                    id = 1L,
                    name = "Tiền trọ & điện nước",
                    accountId = 1L,
                    categoryId = 1L,
                    type = CategoryType.EXPENSE,
                    amount = 3_500_000L,
                    frequency = ReminderFrequency.MONTHLY,
                    dayRule = 5,
                    nextDueAtMillis = System.currentTimeMillis() + 86400000L * 3,
                    remindBeforeDays = 3,
                    isActive = true,
                ),
                accountName = "Vietcombank",
                categoryName = "Hoá đơn",
                isDueSoon = true,
            ),
            ReminderUiModel(
                reminder = Reminder(
                    id = 2L,
                    name = "Gói Netflix",
                    accountId = 2L,
                    categoryId = 2L,
                    type = CategoryType.EXPENSE,
                    amount = 260_000L,
                    frequency = ReminderFrequency.MONTHLY,
                    dayRule = 20,
                    nextDueAtMillis = System.currentTimeMillis() + 86400000L * 15,
                    remindBeforeDays = 1,
                    isActive = true,
                ),
                accountName = "MoMo",
                categoryName = "Giải trí",
                isDueSoon = false,
            ),
        )
        RemindersContent(
            uiState = RemindersUiState.Success(
                reminders = sampleReminders,
                accounts = listOf(Account(1L, "Vietcombank", AccountType.BANK, 5_000_000L, 5_000_000L, false)),
                categories = listOf(Category(1L, "Hoá đơn", CategoryType.EXPENSE, "bill", "blue", true)),
                activeCount = 2,
                totalMonthlyEstimatedAmount = 3_760_000L,
            ),
            onToggleActive = { _, _ -> },
            onAddClick = { showAddEditDialog = true },
            onEditClick = { editingReminder = it; showAddEditDialog = true },
            onDeleteClick = { deletingReminderId = it },
            modifier = modifier,
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val writeError by viewModel.writeError.collectAsStateWithLifecycle()

    if (writeError != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissWriteError,
            title = { Text("Thông báo") },
            text = { Text(writeError.orEmpty()) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissWriteError) { Text("Đã hiểu") }
            },
        )
    }

    RemindersContent(
        uiState = uiState,
        onToggleActive = viewModel::toggleActive,
        onAddClick = { editingReminder = null; showAddEditDialog = true },
        onEditClick = { editingReminder = it; showAddEditDialog = true },
        onDeleteClick = { deletingReminderId = it },
        modifier = modifier,
    )

    if (showAddEditDialog && uiState is RemindersUiState.Success) {
        val success = uiState as RemindersUiState.Success
        AddEditReminderDialog(
            reminder = editingReminder,
            accounts = success.accounts,
            categories = success.categories,
            onDismiss = { showAddEditDialog = false; editingReminder = null },
            onConfirm = { id, name, type, amount, accountId, categoryId, frequency, dayRule, remindBeforeDays, isActive ->
                viewModel.saveReminder(
                    id = id,
                    name = name,
                    type = type,
                    amount = amount,
                    accountId = accountId,
                    categoryId = categoryId,
                    frequency = frequency,
                    dayRule = dayRule,
                    remindBeforeDays = remindBeforeDays,
                    isActive = isActive,
                    onSuccess = {
                        showAddEditDialog = false
                        editingReminder = null
                    },
                )
            },
        )
    }

    if (deletingReminderId != null) {
        val targetId = deletingReminderId ?: return
        AlertDialog(
            onDismissRequest = { deletingReminderId = null },
            title = { Text("Xoá khoản nhắc định kỳ?") },
            text = { Text("Khoản nhắc này sẽ bị xoá vĩnh viễn và không còn gửi thông báo nhắc hẹn nữa.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReminder(targetId) { deletingReminderId = null }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Xoá")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingReminderId = null }) { Text("Huỷ") }
            },
        )
    }
}

@Composable
fun RemindersContent(
    uiState: RemindersUiState,
    onToggleActive: (Reminder, Boolean) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (Reminder) -> Unit,
    onDeleteClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasNotificationPermission = isGranted
    }

    if (uiState is RemindersUiState.Loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val state = uiState as RemindersUiState.Success

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ScreenHeader("Khoản định kỳ", "Tự động nhắc lịch thanh toán hoá đơn, chi phí thường xuyên.")
            }
        }

        // Notification Permission banner (Android 13+)
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        IconTile(Glyph.Bell, tint = MaterialTheme.colorScheme.error)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Chưa bật quyền thông báo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Bật quyền để nhận thông báo nhắc nhở khi đến ngày thanh toán.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("Bật", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // Summary Card
        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Dự kiến chi định kỳ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "${state.activeCount} đang bật",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${CurrencyUtils.formatVnd(state.totalMonthlyEstimatedAmount)} / tháng",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "EasyCash chỉ gửi thông báo nhắc bạn và không bao giờ tự ý trừ tiền tài khoản.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Action header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Danh sách nhắc hẹn", style = MaterialTheme.typography.titleLarge)
                FilledTonalButton(onClick = onAddClick) {
                    AppIcon(Glyph.Plus, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Thêm khoản nhắc")
                }
            }
        }

        if (state.reminders.isEmpty()) {
            item {
                EmptyPanel(
                    title = "Chưa có khoản nhắc nào",
                    description = "Tạo nhắc nhở cho tiền phòng, tiền mạng, học phí hay các khoản thu định kỳ để không bị quên.",
                    glyph = Glyph.Repeat,
                    action = "Tạo khoản nhắc mới",
                    onAction = onAddClick,
                )
            }
        } else {
            items(state.reminders, key = { it.reminder.id }) { uiModel ->
                ReminderCard(
                    uiModel = uiModel,
                    onToggleActive = { onToggleActive(uiModel.reminder, it) },
                    onEditClick = { onEditClick(uiModel.reminder) },
                    onDeleteClick = { onDeleteClick(uiModel.reminder.id) },
                )
            }
        }
    }
}

@Composable
fun ReminderCard(
    uiModel: ReminderUiModel,
    onToggleActive: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reminder = uiModel.reminder
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val nextDueDateStr = remember(reminder.nextDueAtMillis) { dateFormat.format(Date(reminder.nextDueAtMillis)) }

    val freqLabel = remember(reminder.frequency, reminder.dayRule) {
        when (reminder.frequency) {
            ReminderFrequency.WEEKLY -> {
                val dow = when (reminder.dayRule) {
                    1 -> "Thứ Hai"
                    2 -> "Thứ Ba"
                    3 -> "Thứ Tư"
                    4 -> "Thứ Năm"
                    5 -> "Thứ Sáu"
                    6 -> "Thứ Bảy"
                    else -> "Chủ Nhật"
                }
                "Hàng tuần ($dow)"
            }
            ReminderFrequency.MONTHLY -> "Hàng tháng (Ngày ${reminder.dayRule})"
            ReminderFrequency.YEARLY -> "Hàng năm (Ngày ${reminder.dayRule})"
        }
    }

    val remindNotice = remember(reminder.remindBeforeDays) {
        if (reminder.remindBeforeDays == 0) "Nhắc đúng ngày" else "Nhắc trước ${reminder.remindBeforeDays} ngày"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (reminder.isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (uiModel.isDueSoon) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header row with Frequency badge & Switch
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (reminder.type == CategoryType.EXPENSE) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AppIcon(
                            if (reminder.type == CategoryType.EXPENSE) Glyph.Expense else Glyph.Income,
                            modifier = Modifier.size(14.dp),
                            tint = if (reminder.type == CategoryType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = freqLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (reminder.type == CategoryType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (reminder.isActive) "Đang bật" else "Tắt nhắc",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reminder.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                        checked = reminder.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.size(width = 44.dp, height = 24.dp),
                    )
                }
            }

            // Name and Amount
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = reminder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (uiModel.isDueSoon) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Sắp đến hạn thanh toán!",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = reminder.amount?.let { CurrencyUtils.formatVnd(it) } ?: "Tuỳ thời điểm",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (reminder.type == CategoryType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Details metadata
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppIcon(Glyph.Bank, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tài khoản: ${uiModel.accountName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (uiModel.categoryName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppIcon(Glyph.Tag, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Danh mục: ${uiModel.categoryName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppIcon(Glyph.Calendar, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Kỳ tới: $nextDueDateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppIcon(Glyph.Bell, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(remindNotice, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Actions: Edit and Delete
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onEditClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 40.dp),
                ) {
                    AppIcon(Glyph.Edit, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Chỉnh sửa", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(40.dp)) {
                    AppIcon(Glyph.Trash, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddEditReminderDialog(
    reminder: Reminder?,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (
        id: Long,
        name: String,
        type: CategoryType,
        amount: Long?,
        accountId: Long,
        categoryId: Long?,
        frequency: ReminderFrequency,
        dayRule: Int,
        remindBeforeDays: Int,
        isActive: Boolean,
    ) -> Unit,
) {
    var name by remember { mutableStateOf(reminder?.name.orEmpty()) }
    var selectedType by remember { mutableStateOf(reminder?.type ?: CategoryType.EXPENSE) }
    var amountText by remember { mutableStateOf(reminder?.amount?.toString().orEmpty()) }
    var selectedAccountId by remember { mutableStateOf(reminder?.accountId ?: accounts.firstOrNull()?.id ?: 0L) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(reminder?.categoryId) }
    var selectedFrequency by remember { mutableStateOf(reminder?.frequency ?: ReminderFrequency.MONTHLY) }
    var dayRule by remember { mutableStateOf(reminder?.dayRule ?: 1) }
    var remindBeforeDays by remember { mutableStateOf(reminder?.remindBeforeDays ?: 1) }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    val parsedAmount = amountText.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reminder == null) "Thêm khoản định kỳ" else "Sửa khoản định kỳ") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên khoản nhắc *") },
                    placeholder = { Text("Ví dụ: Tiền phòng, Netflix, Tiền điện...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Type selector: Chi tiêu vs Thu nhập
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == CategoryType.EXPENSE,
                        onClick = { selectedType = CategoryType.EXPENSE },
                        label = { Text("Chi tiêu") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = selectedType == CategoryType.INCOME,
                        onClick = { selectedType = CategoryType.INCOME },
                        label = { Text("Thu nhập") },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Amount
                Column {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) amountText = it },
                        label = { Text("Số tiền dự kiến (tuỳ chọn)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (parsedAmount != null && parsedAmount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "= ${CurrencyUtils.formatVnd(parsedAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Account
                Text("Tài khoản liên kết *", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = selectedAccountId == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) },
                        )
                    }
                }

                // Category
                if (filteredCategories.isNotEmpty()) {
                    Text("Danh mục (tuỳ chọn)", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Không chọn") },
                        )
                        filteredCategories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                            )
                        }
                    }
                }

                // Frequency
                Text("Tần suất lặp lại *", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedFrequency == ReminderFrequency.WEEKLY,
                        onClick = {
                            selectedFrequency = ReminderFrequency.WEEKLY
                            dayRule = dayRule.coerceIn(1, 7)
                        },
                        label = { Text("Hàng tuần") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = selectedFrequency == ReminderFrequency.MONTHLY,
                        onClick = { selectedFrequency = ReminderFrequency.MONTHLY },
                        label = { Text("Hàng tháng") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = selectedFrequency == ReminderFrequency.YEARLY,
                        onClick = { selectedFrequency = ReminderFrequency.YEARLY },
                        label = { Text("Hàng năm") },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Day rule selection
                when (selectedFrequency) {
                    ReminderFrequency.WEEKLY -> {
                        Text("Vào ngày trong tuần", style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                1 to "Thứ 2",
                                2 to "Thứ 3",
                                3 to "Thứ 4",
                                4 to "Thứ 5",
                                5 to "Thứ 6",
                                6 to "Thứ 7",
                                7 to "Chủ Nhật",
                            ).forEach { (dow, label) ->
                                FilterChip(
                                    selected = dayRule == dow,
                                    onClick = { dayRule = dow },
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                    ReminderFrequency.MONTHLY, ReminderFrequency.YEARLY -> {
                        Text("Vào ngày trong tháng (1..31)", style = MaterialTheme.typography.labelMedium)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 5, 10, 15, 20, 25, 28, 30, 31).forEach { day ->
                                FilterChip(
                                    selected = dayRule == day,
                                    onClick = { dayRule = day },
                                    label = { Text("Ngày $day") },
                                )
                            }
                        }
                        Text(
                            text = "Đối với các tháng ngắn hơn (như tháng 2 có 28/29 ngày), EasyCash sẽ tự động tính vào ngày cuối tháng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Remind before days
                Text("Thời gian gửi thông báo", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0 to "Đúng ngày",
                        1 to "Trước 1 ngày",
                        2 to "Trước 2 ngày",
                        3 to "Trước 3 ngày",
                        7 to "Trước 7 ngày",
                    ).forEach { (days, label) ->
                        FilterChip(
                            selected = remindBeforeDays == days,
                            onClick = { remindBeforeDays = days },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        reminder?.id ?: 0L,
                        name,
                        selectedType,
                        parsedAmount,
                        selectedAccountId,
                        selectedCategoryId,
                        selectedFrequency,
                        dayRule,
                        remindBeforeDays,
                        reminder?.isActive ?: true,
                    )
                },
                enabled = name.isNotBlank() && selectedAccountId != 0L,
            ) {
                Text(if (reminder == null) "Thêm khoản nhắc" else "Lưu thay đổi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ") }
        },
    )
}
