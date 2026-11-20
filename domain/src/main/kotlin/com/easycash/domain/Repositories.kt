package com.easycash.domain

import kotlinx.coroutines.flow.Flow

sealed interface WriteResult<out T> {
    data class Success<T>(val value: T) : WriteResult<T>

    enum class Failure : WriteResult<Nothing> {
        INVALID_INPUT,
        NOT_FOUND,
        CONFLICT,
        STORAGE_ERROR,
    }
}

interface TransactionRepository {
    fun observe(filter: TransactionFilter = TransactionFilter()): Flow<List<Transaction>>
    fun observeById(id: Long): Flow<Transaction?>
    fun observeNeedsReviewCount(): Flow<Int>
    suspend fun add(transaction: Transaction): WriteResult<Long>
    suspend fun update(transaction: Transaction): WriteResult<Unit>
    suspend fun delete(id: Long): WriteResult<Unit>
    suspend fun confirm(id: Long): WriteResult<Unit>
    suspend fun reject(id: Long): WriteResult<Unit>
}

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    suspend fun add(account: Account): WriteResult<Long>
    suspend fun update(account: Account): WriteResult<Unit>
    suspend fun archive(id: Long): WriteResult<Unit>
}

interface CategoryRepository {
    fun observeAll(type: CategoryType): Flow<List<Category>>
    suspend fun add(category: Category): WriteResult<Long>
    suspend fun update(category: Category): WriteResult<Unit>
    suspend fun archive(id: Long): WriteResult<Unit>
}

interface BudgetRepository {
    fun observeUsage(month: Int): Flow<List<BudgetUsage>>
    suspend fun upsert(budget: Budget): WriteResult<Long>
    suspend fun delete(id: Long): WriteResult<Unit>
}

interface NotificationSourceRepository {
    fun observeAll(): Flow<List<NotificationSource>>
    fun observeAccessGranted(): Flow<Boolean>
    suspend fun setEnabled(id: Long, isEnabled: Boolean): WriteResult<Unit>
    suspend fun linkAccount(id: Long, accountId: Long?): WriteResult<Unit>
}

interface StatsRepository {
    fun observeSummary(range: TimeRange): Flow<Summary>
    fun observeSpendingByCategory(range: TimeRange): Flow<List<CategorySpending>>
}
