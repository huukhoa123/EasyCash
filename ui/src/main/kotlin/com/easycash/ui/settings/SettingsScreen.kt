package com.easycash.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.Account
import com.easycash.domain.AccountType
import com.easycash.domain.Category
import com.easycash.domain.CategoryType
import com.easycash.domain.NotificationSource
import com.easycash.ui.components.AppIcon
import com.easycash.ui.components.Glyph
import com.easycash.ui.components.IconTile
import com.easycash.ui.components.ScreenHeader
import com.easycash.ui.theme.EasyCashTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_RESTORE_FILE_SIZE = 32 * 1024 * 1024 + 48

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean = true,
    onDarkThemeToggle: (Boolean) -> Unit = {},
    viewModel: SettingsViewModel? = null,
    modifier: Modifier = Modifier,
) {
    if (viewModel == null) {
        SettingsContent(
            uiState = SettingsUiState.Success(
                sources = listOf(
                    NotificationSource(1L, "com.mservice.momopay", "Ví MoMo", true, 3L),
                    NotificationSource(2L, "com.vnpay.bidv", "BIDV SmartBanking", true, 2L),
                ),
                isAccessGranted = false,
                accounts = listOf(
                    Account(1L, "Tiền mặt", AccountType.CASH, 0L, 0L, false),
                    Account(2L, "BIDV", AccountType.BANK, 0L, 0L, false),
                    Account(3L, "MoMo", AccountType.EWALLET, 0L, 0L, false),
                ),
                categories = listOf(
                    Category(1L, "Ăn uống", CategoryType.EXPENSE, "food", "orange", true),
                    Category(2L, "Di chuyển", CategoryType.EXPENSE, "transit", "blue", true),
                    Category(3L, "Lương", CategoryType.INCOME, "salary", "green", true),
                ),
            ),
            isDarkTheme = isDarkTheme,
            onDarkThemeToggle = onDarkThemeToggle,
            onToggleSource = { _, _ -> },
            onLinkAccount = { _, _ -> },
            onAddCategory = { _, _, _ -> },
            modifier = modifier,
        )
    } else {
        val writeError by viewModel.writeError.collectAsStateWithLifecycle()
        if (writeError != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissWriteError,
                title = { Text("Chưa lưu được thay đổi") },
                text = { Text(writeError.orEmpty()) },
                confirmButton = { TextButton(onClick = viewModel::dismissWriteError) { Text("Đã hiểu") } },
            )
        }

        val infoMessage by viewModel.infoMessage.collectAsStateWithLifecycle()
        if (infoMessage != null) {
            AlertDialog(
                onDismissRequest = viewModel::dismissInfoMessage,
                title = { Text("Thông báo") },
                text = { Text(infoMessage.orEmpty()) },
                confirmButton = { TextButton(onClick = viewModel::dismissInfoMessage) { Text("Đã hiểu") } },
            )
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SettingsContent(
            uiState = uiState,
            isDarkTheme = isDarkTheme,
            onDarkThemeToggle = onDarkThemeToggle,
            onToggleSource = { id, enabled -> viewModel.toggleSource(id, enabled) },
            onLinkAccount = { sourceId, accId -> viewModel.linkAccount(sourceId, accId) },
            onAddCategory = { name, type, onSuccess -> viewModel.addCategory(name, type, onSuccess) },
            onExportBackup = { password, onReady -> viewModel.exportBackup(password, onReady) },
            onRestoreBackup = { bytes, password, onSuccess -> viewModel.restoreBackup(bytes, password, onSuccess) },
            onExportCsv = { onReady -> viewModel.exportCsv(onReady) },
            onClearAllData = { onSuccess -> viewModel.clearAllData(onSuccess) },
            onShowInfo = { msg -> viewModel.showInfoMessage(msg) },
            onShowError = { err -> viewModel.showWriteError(err) },
            modifier = modifier,
        )
    }
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    isDarkTheme: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit,
    onToggleSource: (Long, Boolean) -> Unit,
    onLinkAccount: (Long, Long?) -> Unit,
    onAddCategory: (String, CategoryType, () -> Unit) -> Unit,
    onExportBackup: (CharArray, (ByteArray) -> Unit) -> Unit = { _, _ -> },
    onRestoreBackup: (ByteArray, CharArray, () -> Unit) -> Unit = { _, _, _ -> },
    onExportCsv: ((String) -> Unit) -> Unit = {},
    onClearAllData: (() -> Unit) -> Unit = {},
    onShowInfo: (String) -> Unit = {},
    onShowError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var editingSource by remember { mutableStateOf<NotificationSource?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showPermissionGuideDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isReadingRestoreFile by remember { mutableStateOf(false) }

    var pendingBackupBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingCsvData by remember { mutableStateOf<String?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        val bytes = pendingBackupBytes
        pendingBackupBytes = null
        if (uri != null) {
            if (bytes == null) {
                onShowError("Dữ liệu sao lưu không khả dụng.")
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                try {
                    val ok = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
                        stream.use { os ->
                            os.write(bytes)
                            os.flush()
                        }
                        true
                    }
                    if (ok) {
                        onShowInfo("Đã lưu bản sao lưu thành công vào máy!")
                    } else {
                        onShowError("Không thể tạo luồng ghi file sao lưu.")
                    }
                } catch (e: Exception) {
                    onShowError("Không thể ghi file sao lưu: ${e.localizedMessage}")
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isReadingRestoreFile = true
                try {
                    val (bytes, error) = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openInputStream(uri)
                        if (stream == null) {
                            return@withContext null to "Không thể mở file sao lưu đã chọn."
                        }
                        stream.use { input ->
                            val buffer = ByteArrayOutputStream()
                            val chunk = ByteArray(16384)
                            var totalRead = 0
                            var byteCount: Int
                            var isOversized = false
                            while (input.read(chunk).also { byteCount = it } != -1) {
                                totalRead += byteCount
                                if (totalRead > MAX_RESTORE_FILE_SIZE) {
                                    isOversized = true
                                    break
                                }
                                buffer.write(chunk, 0, byteCount)
                            }
                            if (isOversized) {
                                null to "File sao lưu vượt quá dung lượng tối đa 32MB."
                            } else {
                                buffer.toByteArray() to null
                            }
                        }
                    }
                    if (error != null) {
                        onShowError(error)
                    } else if (bytes != null) {
                        pendingRestoreBytes = bytes
                        showRestoreDialog = true
                    }
                } catch (e: Exception) {
                    onShowError("Không thể mở file sao lưu: ${e.localizedMessage}")
                } finally {
                    isReadingRestoreFile = false
                }
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        val csv = pendingCsvData
        pendingCsvData = null
        if (uri != null) {
            if (csv == null) {
                onShowError("Dữ liệu CSV không khả dụng.")
                return@rememberLauncherForActivityResult
            }
            coroutineScope.launch {
                try {
                    val ok = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri) ?: return@withContext false
                        stream.use { os ->
                            os.write(csv.toByteArray(Charsets.UTF_8))
                            os.flush()
                        }
                        true
                    }
                    if (ok) {
                        onShowInfo("Đã xuất danh sách giao dịch sang file CSV thành công!")
                    } else {
                        onShowError("Không thể tạo luồng ghi file CSV.")
                    }
                } catch (e: Exception) {
                    onShowError("Không thể ghi file CSV: ${e.localizedMessage}")
                }
            }
        }
    }

    when (uiState) {
        is SettingsUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        is SettingsUiState.Success -> {
            val accountsMap = remember(uiState.accounts) {
                uiState.accounts.associate { it.id to it.name }
            }

            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { ScreenHeader("Cài đặt", "Cá nhân hóa và quản lý dữ liệu.") }

                // Section 1: Giao diện
                item {
                    Text(
                        text = "Giao diện",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconTile(if (isDarkTheme) Glyph.Moon else Glyph.Sun)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Chế độ tối",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (isDarkTheme) "Tối · Êm dịu cho mắt" else "Sáng · Rõ ràng mỗi ngày",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = isDarkTheme, onCheckedChange = onDarkThemeToggle)
                        }
                    }
                }

                // Section 2: Nguồn đọc thông báo
                item {
                    Text(
                        text = "Tự động ghi nhận",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconTile(Glyph.Shield)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "Bảo vệ quyền riêng tư",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Thông báo giao dịch được xử lý 100% trên thiết bị.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Trạng thái quyền: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (uiState.isAccessGranted) "Đã cấp quyền" else "Chưa cấp quyền",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isAccessGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    if (uiState.isAccessGranted) {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                        } catch (_: Exception) {
                                        }
                                    } else {
                                        showPermissionGuideDialog = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (uiState.isAccessGranted) "Quản lý quyền trong cài đặt Android" else "Cấp quyền đọc thông báo ngay")
                            }
                        }
                    }
                }

                // Danh sách nguồn thông báo
                items(uiState.sources, key = { it.id }) { source ->
                    val linkedName = source.linkedAccountId?.let { accountsMap[it] } ?: "Chưa liên kết tài khoản"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = source.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Gán vào: $linkedName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(
                                    onClick = { editingSource = source },
                                    contentPadding = PaddingValues(0.dp),
                                ) {
                                    Text("Đổi tài khoản đích")
                                }
                            }

                            Switch(
                                checked = source.isEnabled,
                                onCheckedChange = { onToggleSource(source.id, it) },
                            )
                        }
                    }
                }

                // Section 3: Quản lý danh mục thu/chi
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Danh mục thu / chi",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        TextButton(onClick = { showAddCategoryDialog = true }) {
                            Text("Thêm danh mục")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            if (uiState.categories.isEmpty()) {
                                Text(
                                    text = "Chưa có danh mục nào trong cơ sở dữ liệu. Bạn có thể bấm '+ Thêm danh mục' để tạo mới.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    text = "Đang có ${uiState.categories.size} danh mục:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    uiState.categories.forEach { cat ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = {
                                                Text("${cat.name} (${if (cat.type == CategoryType.EXPENSE) "Chi" else "Thu"})")
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Quản lý dữ liệu (P-002: Backup, Restore, CSV, Clear all)
                item {
                    Text(
                        text = "Quản lý dữ liệu",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DataActionRow(
                                glyph = Glyph.Download,
                                title = "Sao lưu dữ liệu mã hoá",
                                subtitle = "Xuất file .ecb mã hoá AES-GCM với mật khẩu riêng của bạn.",
                                onClick = { showBackupDialog = true },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            DataActionRow(
                                glyph = Glyph.Upload,
                                title = "Khôi phục từ bản sao lưu",
                                subtitle = "Nhập file .ecb và mật khẩu (toàn bộ dữ liệu hiện tại sẽ bị thay thế).",
                                onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            DataActionRow(
                                glyph = Glyph.Document,
                                title = "Xuất danh sách sang CSV",
                                subtitle = "Xuất toàn bộ giao dịch để mở trong Excel, Google Sheets.",
                                onClick = {
                                    onExportCsv { csv ->
                                        pendingCsvData = csv
                                        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                        csvLauncher.launch("EasyCash_Transactions_$dateStr.csv")
                                    }
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            DataActionRow(
                                glyph = Glyph.Trash,
                                title = "Xoá toàn bộ dữ liệu",
                                subtitle = "Xoá sạch tài khoản, danh mục, giao dịch và đưa về ban đầu.",
                                tint = MaterialTheme.colorScheme.error,
                                onClick = { showClearConfirmDialog = true },
                            )
                        }
                    }
                }

                // Section 5: Về ứng dụng
                item {
                    Text(
                        text = "Thông tin",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconTile(Glyph.Shield)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "EasyCash v1.0",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Quản lý tài chính cá nhân ngoại tuyến · Không đám mây · Không quảng cáo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Dialog chọn tài khoản liên kết
            editingSource?.let { src ->
                AlertDialog(
                    onDismissRequest = { editingSource = null },
                    title = { Text("Liên kết ${src.displayName}") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Chọn tài khoản mặc định để tự động ghi nhận giao dịch từ ${src.displayName}:",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                RadioButton(
                                    selected = src.linkedAccountId == null,
                                    onClick = {
                                        onLinkAccount(src.id, null)
                                        editingSource = null
                                    },
                                )
                                Text(text = "Không liên kết", style = MaterialTheme.typography.bodyMedium)
                            }
                            uiState.accounts.forEach { acc ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    RadioButton(
                                        selected = src.linkedAccountId == acc.id,
                                        onClick = {
                                            onLinkAccount(src.id, acc.id)
                                            editingSource = null
                                        },
                                    )
                                    Text(text = acc.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { editingSource = null }) {
                            Text("Đóng")
                        }
                    },
                )
            }

            // Dialog Thêm danh mục mới
            if (showAddCategoryDialog) {
                var newCatName by remember { mutableStateOf("") }
                var newCatType by remember { mutableStateOf(CategoryType.EXPENSE) }

                AlertDialog(
                    onDismissRequest = { showAddCategoryDialog = false },
                    title = { Text("Thêm danh mục mới") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = newCatType == CategoryType.EXPENSE,
                                    onClick = { newCatType = CategoryType.EXPENSE },
                                    label = { Text("Chi tiêu") },
                                )
                                FilterChip(
                                    selected = newCatType == CategoryType.INCOME,
                                    onClick = { newCatType = CategoryType.INCOME },
                                    label = { Text("Thu nhập") },
                                )
                            }
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = { newCatName = it },
                                label = { Text("Tên danh mục (vd: Xăng xe, Cà phê)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newCatName.isNotBlank()) {
                                    onAddCategory(newCatName, newCatType) { showAddCategoryDialog = false }
                                }
                            },
                            enabled = newCatName.isNotBlank(),
                        ) {
                            Text("Thêm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddCategoryDialog = false }) {
                            Text("Huỷ")
                        }
                    },
                )
            }

            // Dialog hướng dẫn cấp quyền đọc thông báo Android (đặc biệt cho Android 13+)
            if (showPermissionGuideDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionGuideDialog = false },
                    title = { Text("Hướng dẫn cấp quyền đọc thông báo") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Để nhận diện giao dịch từ nguồn bạn bật, EasyCash cần quyền truy cập thông báo của Android.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "1. Bước 1 (Nếu dùng Android 13+ bị chặn \"Cài đặt bị hạn chế\"):\n" +
                                    "Bấm nút \"Thông tin ứng dụng\" bên dưới -> Bấm dấu 3 chấm góc phải trên -> Chọn \"Cho phép cài đặt bị hạn chế\".",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "2. Bước 2: Bấm nút \"Cài đặt thông báo\" bên dưới -> Tìm và gạt Bật cho EasyCash.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                                    } catch (_: Exception) {
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Mở cài đặt quyền thông báo")
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Mở thông tin ứng dụng EasyCash")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showPermissionGuideDialog = false }) {
                            Text("Đóng")
                        }
                    },
                )
            }

            // Dialog Nhập mật khẩu sao lưu AES-GCM
            if (showBackupDialog) {
                var password by remember { mutableStateOf("") }
                var passwordVisible by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = {
                        showBackupDialog = false
                        password = ""
                    },
                    title = { Text("Sao lưu mã hoá dữ liệu") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "File sao lưu sẽ được mã hoá bằng thuật toán AES-GCM với mật khẩu riêng của bạn.\n\n" +
                                    "LƯU Ý: EasyCash không lưu mật khẩu này. Hãy ghi nhớ hoặc lưu trữ cẩn thận để có thể khôi phục sau này.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Mật khẩu sao lưu (từ 8 ký tự)") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        AppIcon(
                                            glyph = if (passwordVisible) Glyph.EyeOff else Glyph.Eye,
                                            contentDescription = "Ẩn/hiện mật khẩu",
                                        )
                                    }
                                },
                                isError = password.isNotEmpty() && password.length < 8,
                                supportingText = {
                                    if (password.isNotEmpty() && password.length < 8) {
                                        Text(
                                            "Mật khẩu cần ít nhất 8 ký tự (hiện có ${password.length})",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    } else {
                                        Text("Độ dài hợp lệ: từ 8 đến 1024 ký tự")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val passChars = password.toCharArray()
                                password = ""
                                onExportBackup(passChars) { bytes ->
                                    pendingBackupBytes = bytes
                                    val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    backupLauncher.launch("EasyCash_Backup_$dateStr.ecb")
                                }
                                showBackupDialog = false
                            },
                            enabled = password.length in 8..1024,
                        ) {
                            Text("Tạo bản sao lưu")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showBackupDialog = false
                            password = ""
                        }) {
                            Text("Huỷ")
                        }
                    },
                )
            }

            // Dialog Khôi phục từ bản sao lưu
            if (showRestoreDialog && pendingRestoreBytes != null) {
                var restorePassword by remember { mutableStateOf("") }
                var restorePasswordVisible by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = {
                        showRestoreDialog = false
                        pendingRestoreBytes = null
                        restorePassword = ""
                    },
                    title = { Text("Khôi phục từ bản sao lưu") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "CẢNH BÁO QUAN TRỌNG:\n" +
                                        "Toàn bộ tài khoản, giao dịch và cài đặt hiện tại sẽ bị XOÁ VÀ THAY THẾ bằng dữ liệu trong bản sao lưu này.\n" +
                                        "Các nguồn đọc thông báo tự động sẽ được đặt về trạng thái tắt để đảm bảo an toàn.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                            Text(
                                text = "Dung lượng file: ${pendingRestoreBytes!!.size / 1024} KB. Nhập mật khẩu đã dùng khi tạo bản sao lưu:",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedTextField(
                                value = restorePassword,
                                onValueChange = { restorePassword = it },
                                label = { Text("Mật khẩu giải mã") },
                                singleLine = true,
                                visualTransformation = if (restorePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { restorePasswordVisible = !restorePasswordVisible }) {
                                        AppIcon(
                                            glyph = if (restorePasswordVisible) Glyph.EyeOff else Glyph.Eye,
                                            contentDescription = "Ẩn/hiện mật khẩu",
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val bytes = pendingRestoreBytes!!
                                val passChars = restorePassword.toCharArray()
                                restorePassword = ""
                                onRestoreBackup(bytes, passChars) {
                                    showRestoreDialog = false
                                    pendingRestoreBytes = null
                                }
                            },
                            enabled = restorePassword.isNotEmpty(),
                        ) {
                            Text("Bắt đầu khôi phục")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showRestoreDialog = false
                                pendingRestoreBytes = null
                                restorePassword = ""
                            },
                        ) {
                            Text("Huỷ")
                        }
                    },
                )
            }

            // Dialog hiển thị trạng thái đang đọc file lớn
            if (isReadingRestoreFile) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Đang đọc file sao lưu") },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text("Vui lòng đợi trong giây lát...")
                        }
                    },
                    confirmButton = {},
                )
            }

            // Dialog Xoá sạch toàn bộ dữ liệu
            if (showClearConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmDialog = false },
                    title = { Text("Xoá sạch toàn bộ dữ liệu?") },
                    text = {
                        Text(
                            text = "Hành động này sẽ xoá vĩnh viễn:\n" +
                                "• Mọi tài khoản và số dư\n" +
                                "• Toàn bộ lịch sử giao dịch và quy tắc phân loại\n" +
                                "• Mọi hạn mức ngân sách\n\n" +
                                "EasyCash sẽ được đưa về trạng thái như khi mới cài đặt ứng dụng. Thao tác này KHÔNG THỂ HOÀN TÁC.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showClearConfirmDialog = false
                                onClearAllData {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text("Xoá vĩnh viễn")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirmDialog = false }) {
                            Text("Giữ lại dữ liệu")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DataActionRow(
    glyph: Glyph,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(glyph, tint = tint)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        AppIcon(
            glyph = Glyph.Arrow,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(name = "SettingsScreen Dark", showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        SettingsScreen()
    }
}

@Preview(name = "SettingsScreen Light", showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    EasyCashTheme(darkTheme = false) {
        SettingsScreen(isDarkTheme = false)
    }
}
