package com.easycash.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easycash.domain.Account
import com.easycash.domain.AccountRepository
import com.easycash.domain.Category
import com.easycash.domain.CategoryRepository
import com.easycash.domain.CategoryType
import com.easycash.domain.DataManagementRepository
import com.easycash.domain.NotificationSource
import com.easycash.domain.NotificationSourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.easycash.domain.WriteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(
        val sources: List<NotificationSource>,
        val isAccessGranted: Boolean,
        val accounts: List<Account>,
        val categories: List<Category>,
    ) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notificationSourceRepository: NotificationSourceRepository,
    accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val dataManagementRepository: DataManagementRepository? = null,
) : ViewModel() {

    private val _writeError = MutableStateFlow<String?>(null)
    val writeError = _writeError.asStateFlow()

    private val _infoMessage = MutableStateFlow<String?>(null)
    val infoMessage = _infoMessage.asStateFlow()

    fun dismissWriteError() { _writeError.value = null }
    fun dismissInfoMessage() { _infoMessage.value = null }
    fun showInfoMessage(msg: String) { _infoMessage.value = msg }
    fun showWriteError(msg: String) { _writeError.value = msg }

    private val categoriesFlow = combine(
        categoryRepository.observeAll(CategoryType.EXPENSE),
        categoryRepository.observeAll(CategoryType.INCOME),
    ) { exp, inc -> exp + inc }

    val uiState: StateFlow<SettingsUiState> = combine(
        notificationSourceRepository.observeAll(),
        notificationSourceRepository.observeAccessGranted(),
        accountRepository.observeAll(),
        categoriesFlow,
    ) { sources, accessGranted, accounts, categories ->
        SettingsUiState.Success(
            sources = sources,
            isAccessGranted = accessGranted,
            accounts = accounts,
            categories = categories,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SettingsUiState.Loading,
    )

    fun toggleSource(sourceId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            val result = notificationSourceRepository.setEnabled(sourceId, isEnabled)
            _writeError.value = if (result is WriteResult.Failure) "Không thể cập nhật nguồn thông báo. Vui lòng thử lại." else null
        }
    }

    fun linkAccount(sourceId: Long, accountId: Long?) {
        viewModelScope.launch {
            val result = notificationSourceRepository.linkAccount(sourceId, accountId)
            _writeError.value = if (result is WriteResult.Failure) "Không thể liên kết tài khoản. Kiểm tra tài khoản đang hoạt động." else null
        }
    }

    fun addCategory(name: String, type: CategoryType, onSuccess: () -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = categoryRepository.add(
                Category(
                    id = 0L,
                    name = name.trim(),
                    type = type,
                    iconKey = if (type == CategoryType.EXPENSE) "expense" else "income",
                    colorKey = if (type == CategoryType.EXPENSE) "orange" else "green",
                    isSystem = false,
                )
            )
            _writeError.value = when (result) {
                WriteResult.Failure.CONFLICT -> "Tên danh mục đã tồn tại."
                is WriteResult.Failure -> "Không thể lưu danh mục. Vui lòng thử lại."
                else -> null
            }
            if (result is WriteResult.Success) onSuccess()
        }
    }

    companion object {
        const val MAX_BACKUP_SIZE = 32 * 1024 * 1024 + 48
    }

    fun exportBackup(password: CharArray, onReady: (ByteArray) -> Unit) {
        if (password.size < 8 || password.size > 1024) {
            password.fill('\u0000')
            _writeError.value = "Mật khẩu sao lưu phải có từ 8 đến 1024 ký tự."
            return
        }
        val repo = dataManagementRepository
        if (repo == null) {
            password.fill('\u0000')
            return
        }
        viewModelScope.launch {
            try {
                when (val result = repo.exportBackup(password)) {
                    is WriteResult.Success -> onReady(result.value)
                    is WriteResult.Failure -> _writeError.value = "Không thể tạo bản sao lưu. Vui lòng thử lại."
                }
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun restoreBackup(bytes: ByteArray, password: CharArray, onSuccess: () -> Unit) {
        if (bytes.size > MAX_BACKUP_SIZE) {
            password.fill('\u0000')
            _writeError.value = "File sao lưu vượt quá dung lượng tối đa 32MB."
            return
        }
        val repo = dataManagementRepository
        if (repo == null) {
            password.fill('\u0000')
            return
        }
        viewModelScope.launch {
            try {
                when (val result = repo.restoreBackup(bytes, password)) {
                    is WriteResult.Success -> {
                        _infoMessage.value = "Khôi phục dữ liệu thành công!"
                        onSuccess()
                    }
                    is WriteResult.Failure -> _writeError.value = "Khôi phục thất bại. Kiểm tra mật khẩu hoặc tính hợp lệ của file."
                }
            } finally {
                password.fill('\u0000')
            }
        }
    }

    fun exportCsv(onReady: (String) -> Unit) {
        val repo = dataManagementRepository ?: return
        viewModelScope.launch {
            when (val result = repo.exportCsv()) {
                is WriteResult.Success -> onReady(result.value)
                is WriteResult.Failure -> _writeError.value = "Không thể xuất file CSV. Vui lòng thử lại."
            }
        }
    }

    fun clearAllData(onSuccess: () -> Unit) {
        val repo = dataManagementRepository ?: return
        viewModelScope.launch {
            when (val result = repo.clearAll()) {
                is WriteResult.Success -> {
                    _infoMessage.value = "Đã xoá sạch dữ liệu và đưa về trạng thái ban đầu."
                    onSuccess()
                }
                is WriteResult.Failure -> _writeError.value = "Không thể xoá dữ liệu. Vui lòng thử lại."
            }
        }
    }
}
