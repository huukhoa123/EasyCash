package com.easycash.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easycash.domain.Account
import com.easycash.domain.AccountRepository
import com.easycash.domain.Category
import com.easycash.domain.CategoryRepository
import com.easycash.domain.CategoryType
import com.easycash.domain.Transaction
import com.easycash.domain.TransactionFilter
import com.easycash.domain.TransactionOrigin
import com.easycash.domain.TransactionRepository
import com.easycash.domain.TransactionStatus
import com.easycash.domain.TransactionType
import com.easycash.domain.WriteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState
    data class Success(
        val transactions: List<Transaction>,
        val accounts: List<Account>,
        val categories: List<Category>,
        val selectedType: TransactionType?,
        val selectedStatus: TransactionStatus?,
        val keyword: String,
    ) : TransactionsUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _selectedType = MutableStateFlow<TransactionType?>(null)
    val selectedType = _selectedType.asStateFlow()

    private val _selectedStatus = MutableStateFlow<TransactionStatus?>(null)
    val selectedStatus = _selectedStatus.asStateFlow()

    private val _keyword = MutableStateFlow("")
    val keyword = _keyword.asStateFlow()

    private val _writeError = MutableStateFlow(false)
    val writeError = _writeError.asStateFlow()

    fun dismissWriteError() { _writeError.value = false }

    fun setKeyword(query: String) {
        _keyword.value = query
    }

    fun setSelectedType(type: TransactionType?) {
        _selectedType.value = type
    }

    fun setSelectedStatus(status: TransactionStatus?) {
        _selectedStatus.value = status
    }

    private val accountsFlow = accountRepository.observeAll()
    private val categoriesFlow = combine(
        categoryRepository.observeAll(CategoryType.EXPENSE),
        categoryRepository.observeAll(CategoryType.INCOME),
    ) { exp, inc -> exp + inc }

    val uiState: StateFlow<TransactionsUiState> = combine(_selectedType, _selectedStatus, _keyword) { type, status, kw ->
        TransactionFilter(
            type = type,
            status = status,
            keyword = kw.takeIf { it.isNotBlank() },
        )
    }.flatMapLatest { filter ->
        combine(
            transactionRepository.observe(filter),
            accountsFlow,
            categoriesFlow,
        ) { txs, accounts, categories ->
            TransactionsUiState.Success(
                transactions = txs,
                accounts = accounts,
                categories = categories,
                selectedType = filter.type,
                selectedStatus = filter.status,
                keyword = filter.keyword ?: "",
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = TransactionsUiState.Loading,
    )

    fun addTransaction(
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long?,
        type: TransactionType,
        amount: Long,
        note: String,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val result = transactionRepository.add(
                Transaction(
                    id = 0L,
                    accountId = accountId,
                    toAccountId = toAccountId,
                    categoryId = categoryId,
                    type = type,
                    amount = amount,
                    note = note.trim(),
                    occurredAtMillis = System.currentTimeMillis(),
                    status = TransactionStatus.USER_CONFIRMED,
                    origin = TransactionOrigin.MANUAL,
                )
            )
            _writeError.value = result is WriteResult.Failure
            if (result is WriteResult.Success) onSuccess()
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            _writeError.value = transactionRepository.delete(id) is WriteResult.Failure
        }
    }

    fun confirmTransaction(id: Long) {
        viewModelScope.launch {
            _writeError.value = transactionRepository.confirm(id) is WriteResult.Failure
        }
    }

    fun rejectTransaction(id: Long) {
        viewModelScope.launch {
            _writeError.value = transactionRepository.reject(id) is WriteResult.Failure
        }
    }

    fun restoreTransaction(transaction: Transaction) {
        viewModelScope.launch {
            _writeError.value = transactionRepository.add(transaction) is WriteResult.Failure
        }
    }

    fun updateTransaction(transaction: Transaction, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = transactionRepository.update(transaction)
            _writeError.value = result is WriteResult.Failure
            if (result is WriteResult.Success) onSuccess()
        }
    }
}
