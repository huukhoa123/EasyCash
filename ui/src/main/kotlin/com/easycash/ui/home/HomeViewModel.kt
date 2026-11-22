package com.easycash.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easycash.domain.Account
import com.easycash.domain.AccountRepository
import com.easycash.domain.NotificationSourceRepository
import com.easycash.domain.StatsRepository
import com.easycash.domain.Transaction
import com.easycash.domain.TransactionFilter
import com.easycash.domain.TransactionRepository
import com.easycash.domain.TransactionType
import com.easycash.domain.TransactionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val totalEstimatedBalance: Long,
        val totalIncome: Long,
        val totalExpense: Long,
        val recentTransactions: List<Transaction>,
        val needsReviewCount: Int,
        val accountsMap: Map<Long, String>,
        val isAccessGranted: Boolean = true,
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    accountRepository: AccountRepository,
    transactionRepository: TransactionRepository,
    statsRepository: StatsRepository,
    notificationSourceRepository: NotificationSourceRepository,
) : ViewModel() {

    private val _isBalanceHidden = MutableStateFlow(false)
    val isBalanceHidden = _isBalanceHidden.asStateFlow()

    fun toggleBalanceVisibility() {
        _isBalanceHidden.value = !_isBalanceHidden.value
    }

    val uiState: StateFlow<HomeUiState> = combine(
        accountRepository.observeAll(),
        transactionRepository.observe(TransactionFilter()),
        transactionRepository.observeNeedsReviewCount(),
        notificationSourceRepository.observeAccessGranted(),
    ) { accounts, allTxs, reviewCount, isGranted ->
        val totalBalance = accounts.sumOf { it.estimatedBalance }
        val counted = allTxs.filter { it.status == TransactionStatus.AUTO_RECORDED || it.status == TransactionStatus.USER_CONFIRMED }
        val income = counted.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expense = counted.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val recent = allTxs.take(5)
        val accMap = accounts.associate { it.id to it.name }

        HomeUiState.Success(
            totalEstimatedBalance = totalBalance,
            totalIncome = income,
            totalExpense = expense,
            recentTransactions = recent,
            needsReviewCount = reviewCount,
            accountsMap = accMap,
            isAccessGranted = isGranted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState.Loading,
    )
}
