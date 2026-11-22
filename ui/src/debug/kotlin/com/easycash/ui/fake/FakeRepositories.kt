package com.easycash.ui.fake

import com.easycash.domain.Account
import com.easycash.domain.AccountRepository
import com.easycash.domain.AccountType
import com.easycash.domain.Budget
import com.easycash.domain.BudgetRepository
import com.easycash.domain.BudgetUsage
import com.easycash.domain.Category
import com.easycash.domain.CategoryRepository
import com.easycash.domain.CategorySpending
import com.easycash.domain.CategoryType
import com.easycash.domain.DataManagementRepository
import com.easycash.domain.NotificationSource
import com.easycash.domain.NotificationSourceRepository
import com.easycash.domain.StatsRepository
import com.easycash.domain.Summary
import com.easycash.domain.TimeRange
import com.easycash.domain.Transaction
import com.easycash.domain.TransactionFilter
import com.easycash.domain.TransactionOrigin
import com.easycash.domain.TransactionRepository
import com.easycash.domain.TransactionStatus
import com.easycash.domain.TransactionType
import com.easycash.domain.WriteResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

class FakeAccountRepository(
    initialAccounts: List<Account> = defaultAccounts,
) : AccountRepository {
    private val nextId = AtomicLong(initialAccounts.maxOfOrNull { it.id }?.plus(1) ?: 1L)
    private val accounts = MutableStateFlow(initialAccounts)

    override fun observeAll(): Flow<List<Account>> =
        accounts.asStateFlow().map { list -> list.filter { !it.isArchived } }

    override suspend fun add(account: Account): WriteResult<Long> {
        val id = nextId.getAndIncrement()
        accounts.value += account.copy(id = id)
        return WriteResult.Success(id)
    }

    override suspend fun update(account: Account): WriteResult<Unit> {
        val current = accounts.value
        val index = current.indexOfFirst { it.id == account.id }
        if (index == -1) return WriteResult.Failure.NOT_FOUND
        val updated = current.toMutableList()
        updated[index] = account
        accounts.value = updated
        return WriteResult.Success(Unit)
    }

    override suspend fun archive(id: Long): WriteResult<Unit> {
        val current = accounts.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return WriteResult.Failure.NOT_FOUND
        val updated = current.toMutableList()
        updated[index] = updated[index].copy(isArchived = true)
        accounts.value = updated
        return WriteResult.Success(Unit)
    }

    companion object {
        val defaultAccounts = listOf(
            Account(1L, "Tiền mặt", AccountType.CASH, 2_000_000L, 2_000_000L, false),
            Account(2L, "BIDV", AccountType.BANK, 5_500_000L, 5_500_000L, false),
            Account(3L, "MoMo", AccountType.EWALLET, 850_000L, 850_000L, false),
        )
    }
}

class FakeCategoryRepository(
    initialCategories: List<Category> = defaultCategories,
) : CategoryRepository {
    private val nextId = AtomicLong(initialCategories.maxOfOrNull { it.id }?.plus(1) ?: 1L)
    private val categories = MutableStateFlow(initialCategories)

    override fun observeAll(type: CategoryType): Flow<List<Category>> =
        categories.asStateFlow().map { list -> list.filter { it.type == type } }

    override suspend fun add(category: Category): WriteResult<Long> {
        val id = nextId.getAndIncrement()
        categories.value += category.copy(id = id)
        return WriteResult.Success(id)
    }

    override suspend fun update(category: Category): WriteResult<Unit> {
        val current = categories.value
        val index = current.indexOfFirst { it.id == category.id }
        if (index == -1) return WriteResult.Failure.NOT_FOUND
        val updated = current.toMutableList()
        updated[index] = category
        categories.value = updated
        return WriteResult.Success(Unit)
    }

    override suspend fun archive(id: Long): WriteResult<Unit> {
        val current = categories.value
        categories.value = current.filter { it.id != id }
        return WriteResult.Success(Unit)
    }

    companion object {
        val defaultCategories = listOf(
            Category(1L, "Ăn uống", CategoryType.EXPENSE, "food", "orange", true),
            Category(2L, "Di chuyển", CategoryType.EXPENSE, "transit", "blue", true),
            Category(3L, "Mua sắm", CategoryType.EXPENSE, "shopping", "purple", true),
            Category(4L, "Lương", CategoryType.INCOME, "salary", "green", true),
            Category(5L, "Thưởng", CategoryType.INCOME, "bonus", "emerald", true),
        )
    }
}

class FakeTransactionRepository(
    initialTransactions: List<Transaction> = defaultTransactions,
) : TransactionRepository {
    private val nextId = AtomicLong(initialTransactions.maxOfOrNull { it.id }?.plus(1) ?: 1L)
    private val transactions = MutableStateFlow(initialTransactions)

    override fun observe(filter: TransactionFilter): Flow<List<Transaction>> =
        transactions.asStateFlow().map { list ->
            list.filter { tx ->
                val matchRange = filter.range?.let { r ->
                    tx.occurredAtMillis >= r.startInclusiveMillis && tx.occurredAtMillis < r.endExclusiveMillis
                } ?: true
                val matchType = filter.type?.let { it == tx.type } ?: true
                val matchAccount = filter.accountId?.let { it == tx.accountId || it == tx.toAccountId } ?: true
                val matchCategory = filter.categoryId?.let { it == tx.categoryId } ?: true
                val matchStatus = filter.status?.let { it == tx.status } ?: true
                val matchKeyword = filter.keyword?.takeIf { it.isNotBlank() }?.let { kw ->
                    tx.note.contains(kw, ignoreCase = true)
                } ?: true

                matchRange && matchType && matchAccount && matchCategory && matchStatus && matchKeyword
            }.sortedByDescending { it.occurredAtMillis }
        }

    override fun observeById(id: Long): Flow<Transaction?> =
        transactions.asStateFlow().map { list -> list.find { it.id == id } }

    override fun observeNeedsReviewCount(): Flow<Int> =
        transactions.asStateFlow().map { list ->
            list.count { it.status == TransactionStatus.NEEDS_REVIEW || it.status == TransactionStatus.AUTO_RECORDED }
        }

    override suspend fun add(transaction: Transaction): WriteResult<Long> {
        val id = nextId.getAndIncrement()
        transactions.value += transaction.copy(id = id)
        return WriteResult.Success(id)
    }

    override suspend fun update(transaction: Transaction): WriteResult<Unit> {
        val current = transactions.value
        val index = current.indexOfFirst { it.id == transaction.id }
        if (index == -1) return WriteResult.Failure.NOT_FOUND
        val updated = current.toMutableList()
        updated[index] = transaction
        transactions.value = updated
        return WriteResult.Success(Unit)
    }

    override suspend fun delete(id: Long): WriteResult<Unit> {
        val current = transactions.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return WriteResult.Failure.NOT_FOUND
        transactions.value = current.filter { it.id != id }
        return WriteResult.Success(Unit)
    }

    override suspend fun confirm(id: Long): WriteResult<Unit> {
        val current = transactions.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return WriteResult.Failure.NOT_FOUND
        val updated = current.toMutableList()
        updated[index] = updated[index].copy(status = TransactionStatus.USER_CONFIRMED)
        transactions.value = updated
        return WriteResult.Success(Unit)
    }

    override suspend fun reject(id: Long): WriteResult<Unit> {
        val current = transactions.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return WriteResult.Failure.NOT_FOUND
        val updated = current.toMutableList()
        updated[index] = updated[index].copy(status = TransactionStatus.REJECTED)
        transactions.value = updated
        return WriteResult.Success(Unit)
    }

    companion object {
        val defaultTransactions = listOf(
            Transaction(
                id = 1L,
                accountId = 2L,
                toAccountId = null,
                categoryId = 1L,
                type = TransactionType.EXPENSE,
                amount = 45_000L,
                note = "Ăn trưa cơm tấm",
                occurredAtMillis = System.currentTimeMillis() - 3_600_000L,
                status = TransactionStatus.USER_CONFIRMED,
                origin = TransactionOrigin.MANUAL,
            ),
            Transaction(
                id = 2L,
                accountId = 2L,
                toAccountId = 3L,
                categoryId = null,
                type = TransactionType.TRANSFER,
                amount = 60_000L,
                note = "Nạp ví MoMo từ BIDV",
                occurredAtMillis = System.currentTimeMillis() - 7_200_000L,
                status = TransactionStatus.AUTO_RECORDED,
                origin = TransactionOrigin.NOTIFICATION,
            ),
        )
    }
}

class FakeNotificationSourceRepository(
    initialSources: List<NotificationSource> = defaultSources,
    initialAccessGranted: Boolean = true,
) : NotificationSourceRepository {
    private val sources = MutableStateFlow(initialSources)
    private val accessGranted = MutableStateFlow(initialAccessGranted)

    override fun observeAll(): Flow<List<NotificationSource>> = sources.asStateFlow()

    override fun observeAccessGranted(): Flow<Boolean> = accessGranted.asStateFlow()

    fun setAccessGranted(granted: Boolean) {
        accessGranted.value = granted
    }

    override suspend fun setEnabled(id: Long, isEnabled: Boolean): WriteResult<Unit> {
        val current = sources.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return WriteResult.Failure.NOT_FOUND
        val updated = current.toMutableList()
        updated[index] = updated[index].copy(isEnabled = isEnabled)
        sources.value = updated
        return WriteResult.Success(Unit)
    }

    override suspend fun linkAccount(id: Long, accountId: Long?): WriteResult<Unit> {
        val current = sources.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return WriteResult.Failure.NOT_FOUND
        val updated = current.toMutableList()
        updated[index] = updated[index].copy(linkedAccountId = accountId)
        sources.value = updated
        return WriteResult.Success(Unit)
    }

    companion object {
        val defaultSources = listOf(
            NotificationSource(
                id = 1L,
                packageName = "com.mservice.momopay",
                displayName = "Ví MoMo",
                isEnabled = true,
                linkedAccountId = 3L,
            ),
            NotificationSource(
                id = 2L,
                packageName = "com.vnpay.bidv",
                displayName = "BIDV SmartBanking",
                isEnabled = true,
                linkedAccountId = 2L,
            ),
        )
    }
}

class FakeStatsRepository(
    private val transactionRepository: TransactionRepository,
) : StatsRepository {
    override fun observeSummary(range: TimeRange): Flow<Summary> =
        transactionRepository.observe(TransactionFilter(range = range)).map { txs ->
            val inc = txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            Summary(income = inc, expense = exp)
        }

    override fun observeSpendingByCategory(range: TimeRange): Flow<List<CategorySpending>> =
        transactionRepository.observe(TransactionFilter(range = range, type = TransactionType.EXPENSE)).map { txs ->
            txs.groupBy { it.categoryId }.map { (catId, group) ->
                CategorySpending(
                    categoryId = catId,
                    amount = group.sumOf { it.amount },
                )
            }.sortedByDescending { it.amount }
        }
}

class FakeBudgetRepository(
    initialBudgets: List<Budget> = emptyList(),
    private val transactionRepository: TransactionRepository? = null,
) : BudgetRepository {
    private val nextId = AtomicLong(initialBudgets.maxOfOrNull { it.id }?.plus(1) ?: 1L)
    private val budgets = MutableStateFlow(initialBudgets)

    override fun observeUsage(month: Int): Flow<List<BudgetUsage>> {
        return combine(budgets.asStateFlow(), transactionRepository?.observe() ?: flowOf(emptyList())) { bList, txs ->
            val monthBudgets = bList.filter { it.month == month }
            monthBudgets.map { b ->
                val spent = txs.filter { tx ->
                    tx.categoryId == b.categoryId && tx.type == TransactionType.EXPENSE
                }.sumOf { it.amount }
                BudgetUsage(budget = b, spent = spent)
            }
        }
    }

    override suspend fun upsert(budget: Budget): WriteResult<Long> {
        val current = budgets.value.toMutableList()
        val index = current.indexOfFirst { it.categoryId == budget.categoryId && it.month == budget.month }
        val id = if (index >= 0) {
            val existing = current[index]
            current[index] = budget.copy(id = existing.id)
            existing.id
        } else {
            val newId = nextId.getAndIncrement()
            current.add(budget.copy(id = newId))
            newId
        }
        budgets.value = current
        return WriteResult.Success(id)
    }

    override suspend fun delete(id: Long): WriteResult<Unit> {
        budgets.value = budgets.value.filter { it.id != id }
        return WriteResult.Success(Unit)
    }
}

class FakeDataManagementRepository : DataManagementRepository {
    var exportResult: WriteResult<ByteArray> = WriteResult.Success(byteArrayOf(1, 2, 3))
    var restoreResult: WriteResult<Unit> = WriteResult.Success(Unit)
    var csvResult: WriteResult<String> = WriteResult.Success("id,account_id\n1,1")
    var clearResult: WriteResult<Unit> = WriteResult.Success(Unit)

    override suspend fun exportBackup(password: CharArray): WriteResult<ByteArray> = exportResult
    override suspend fun restoreBackup(bytes: ByteArray, password: CharArray): WriteResult<Unit> = restoreResult
    override suspend fun exportCsv(): WriteResult<String> = csvResult
    override suspend fun clearAll(): WriteResult<Unit> = clearResult
}


class FakeReminderRepository(
    initialReminders: List<com.easycash.domain.Reminder> = emptyList(),
) : com.easycash.domain.ReminderRepository {
    private val nextId = AtomicLong(initialReminders.maxOfOrNull { it.id }?.plus(1) ?: 1L)
    private val reminders = MutableStateFlow(initialReminders)

    override fun observeAll(): Flow<List<com.easycash.domain.Reminder>> = reminders.asStateFlow()

    override suspend fun upsert(reminder: com.easycash.domain.Reminder): WriteResult<Long> {
        val current = reminders.value.toMutableList()
        val index = if (reminder.id == 0L) -1 else current.indexOfFirst { it.id == reminder.id }
        val id = if (index >= 0) {
            current[index] = reminder
            reminder.id
        } else {
            val assignedId = if (reminder.id == 0L) nextId.getAndIncrement() else reminder.id
            current.add(reminder.copy(id = assignedId))
            assignedId
        }
        reminders.value = current
        return WriteResult.Success(id)
    }

    override suspend fun delete(id: Long): WriteResult<Unit> {
        val current = reminders.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return WriteResult.Failure.NOT_FOUND
        current.removeAt(index)
        reminders.value = current
        return WriteResult.Success(Unit)
    }
}
