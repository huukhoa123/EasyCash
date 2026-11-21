package com.easycash.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easycash.domain.Account
import com.easycash.domain.AccountRepository
import com.easycash.domain.Category
import com.easycash.domain.CategoryRepository
import com.easycash.domain.CategoryType
import com.easycash.domain.NotificationSource
import com.easycash.domain.NotificationSourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

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
            notificationSourceRepository.setEnabled(sourceId, isEnabled)
        }
    }

    fun linkAccount(sourceId: Long, accountId: Long?) {
        viewModelScope.launch {
            notificationSourceRepository.linkAccount(sourceId, accountId)
        }
    }

    fun addCategory(name: String, type: CategoryType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.add(
                Category(
                    id = 0L,
                    name = name.trim(),
                    type = type,
                    iconKey = if (type == CategoryType.EXPENSE) "expense" else "income",
                    colorKey = if (type == CategoryType.EXPENSE) "orange" else "green",
                    isSystem = false,
                )
            )
        }
    }
}
