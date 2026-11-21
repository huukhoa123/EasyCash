package com.easycash.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easycash.domain.Account
import com.easycash.domain.AccountRepository
import com.easycash.domain.AccountType
import com.easycash.domain.WriteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AccountsUiState {
    data object Loading : AccountsUiState
    data object Empty : AccountsUiState
    data class Success(
        val accounts: List<Account>,
        val totalEstimatedBalance: Long,
    ) : AccountsUiState
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = accountRepository.observeAll()
        .map { accounts ->
            if (accounts.isEmpty()) {
                AccountsUiState.Empty
            } else {
                AccountsUiState.Success(
                    accounts = accounts,
                    totalEstimatedBalance = accounts.sumOf { it.estimatedBalance },
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AccountsUiState.Loading,
        )

    private val _writeError = MutableStateFlow(false)
    val writeError = _writeError.asStateFlow()

    fun dismissWriteError() { _writeError.value = false }

    fun addAccount(name: String, type: AccountType, initialBalance: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = accountRepository.add(
                Account(
                    id = 0L,
                    name = name.trim(),
                    type = type,
                    initialBalance = initialBalance,
                    estimatedBalance = initialBalance,
                    isArchived = false,
                )
            )
            _writeError.value = result is WriteResult.Failure
            if (result is WriteResult.Success) onSuccess()
        }
    }

    fun archiveAccount(id: Long) {
        viewModelScope.launch {
            _writeError.value = accountRepository.archive(id) is WriteResult.Failure
        }
    }

    fun updateAccount(account: Account, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = accountRepository.update(account)
            _writeError.value = result is WriteResult.Failure
            if (result is WriteResult.Success) onSuccess()
        }
    }
}
