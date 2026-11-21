package com.easycash.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easycash.domain.Account
import com.easycash.domain.AccountType
import com.easycash.domain.Category
import com.easycash.domain.CategoryType
import com.easycash.domain.NotificationSource
import com.easycash.ui.theme.EasyCashTheme

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
            onAddCategory = { _, _ -> },
            modifier = modifier,
        )
    } else {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SettingsContent(
            uiState = uiState,
            isDarkTheme = isDarkTheme,
            onDarkThemeToggle = onDarkThemeToggle,
            onToggleSource = { id, enabled -> viewModel.toggleSource(id, enabled) },
            onLinkAccount = { sourceId, accId -> viewModel.linkAccount(sourceId, accId) },
            onAddCategory = { name, type -> viewModel.addCategory(name, type) },
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
    onAddCategory: (String, CategoryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var editingSource by remember { mutableStateOf<NotificationSource?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showPermissionGuideDialog by remember { mutableStateOf(false) }

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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Chế độ tối",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (isDarkTheme) "Đang bật Dark mode (Mặc định)" else "Đang bật Light mode",
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
                        text = "Tự động nhận diện giao dịch (Phase 3)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Bảo vệ quyền riêng tư",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "EasyCash xử lý thông báo 100% trên thiết bị của bạn. Ứng dụng không có quyền INTERNET, không gửi dữ liệu ra ngoài và không bao giờ đọc mã OTP hay mật khẩu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
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
                                .padding(14.dp),
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
                            Text("+ Thêm danh mục")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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

                // Section 4: Về ứng dụng
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
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "EasyCash v1.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Quản lý tài chính cá nhân ngoại tuyến. Không tài khoản · Không lưu đám mây · Không quảng cáo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
                                    onAddCategory(newCatName, newCatType)
                                    showAddCategoryDialog = false
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
                                text = "Để tự động nhận diện biến động số dư từ MoMo và BIDV, ứng dụng cần quyền truy cập thông báo của hệ thống Android.",
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
        }
    }
}

@Preview(name = "SettingsScreen Dark", showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        SettingsScreen()
    }
}
