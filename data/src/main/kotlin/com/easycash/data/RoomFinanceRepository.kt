package com.easycash.data

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.easycash.data.local.AccountEntity
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.local.TransactionEntity
import com.easycash.domain.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigInteger

private val MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE)
private val MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE)

private fun BigInteger.toLongExactCompat(): Long {
    if (this < MIN_LONG || this > MAX_LONG) {
        throw ArithmeticException("BigInteger out of long range: $this")
    }
    return this.toLong()
}

class RoomFinanceRepository(private val database: EasyCashDatabase) : AccountRepository, TransactionRepository, StatsRepository {
    private val dao = database.financeDao()

    override fun observeAll() = combine(dao.observeAccounts(), dao.observeTransactions()) { accounts, transactions ->
        accounts.filterNot { it.isArchived }.map { account ->
            val balance = transactions.filter { counted(it) }.fold(BigInteger.valueOf(account.initialBalance)) { total, transaction ->
                val amount = BigInteger.valueOf(transaction.amount)
                when {
                    transaction.type == "TRANSFER" && transaction.toAccountId == account.id -> total + amount
                    transaction.accountId != account.id -> total
                    transaction.type == "EXPENSE" || transaction.type == "TRANSFER" -> total - amount
                    else -> total + amount
                }
            }
            Account(account.id, account.name, AccountType.valueOf(account.type), account.initialBalance, balance.toLongExactCompat(), account.isArchived)
        }
    }

    override suspend fun add(account: Account): WriteResult<Long> = write {
        if (account.name.isBlank()) return@write WriteResult.Failure.INVALID_INPUT
        if (dao.nameCount(account.name.trim(), 0) > 0) return@write WriteResult.Failure.CONFLICT
        val now = System.currentTimeMillis()
        WriteResult.Success(dao.insertAccount(AccountEntity(name = account.name.trim(), type = account.type.name,
            initialBalance = account.initialBalance, createdAt = now, updatedAt = now)))
    }

    override suspend fun update(account: Account): WriteResult<Unit> = write {
        val existing = dao.account(account.id) ?: return@write WriteResult.Failure.NOT_FOUND
        if (account.name.isBlank()) return@write WriteResult.Failure.INVALID_INPUT
        if (!account.isArchived && dao.nameCount(account.name.trim(), account.id) > 0) return@write WriteResult.Failure.CONFLICT
        dao.updateAccount(existing.copy(name = account.name.trim(), type = account.type.name,
            initialBalance = account.initialBalance, isArchived = account.isArchived, updatedAt = System.currentTimeMillis()))
        if (account.isArchived) database.reminderDao().deactivateForAccount(account.id, System.currentTimeMillis())
        WriteResult.Success(Unit)
    }

    override suspend fun archive(id: Long): WriteResult<Unit> = write {
        val existing = dao.account(id) ?: return@write WriteResult.Failure.NOT_FOUND
        val now = System.currentTimeMillis()
        dao.updateAccount(existing.copy(isArchived = true, updatedAt = now))
        database.reminderDao().deactivateForAccount(id, now)
        WriteResult.Success(Unit)
    }

    override fun observe(filter: TransactionFilter) = dao.observeTransactions().map { rows ->
        rows.filter { row ->
            (filter.range == null || row.occurredAt >= filter.range!!.startInclusiveMillis && row.occurredAt < filter.range!!.endExclusiveMillis) &&
                (filter.type == null || row.type == filter.type?.name) &&
                (filter.accountId == null || row.accountId == filter.accountId || row.toAccountId == filter.accountId) &&
                (filter.categoryId == null || row.categoryId == filter.categoryId) &&
                (filter.status == null || row.status == filter.status?.name) &&
                (filter.keyword == null || row.note.contains(filter.keyword.orEmpty(), ignoreCase = true))
        }.map { model(it) }
    }

    override fun observeById(id: Long) = dao.observeTransactions().map { rows -> rows.find { it.id == id }?.let { model(it) } }
    override fun observeNeedsReviewCount() = dao.observeTransactions().map { rows -> rows.count { it.status == "NEEDS_REVIEW" } }

    override suspend fun add(transaction: Transaction): WriteResult<Long> = write {
        if (!valid(transaction)) return@write WriteResult.Failure.INVALID_INPUT
        val now = System.currentTimeMillis()
        WriteResult.Success(dao.insertTransaction(entity(transaction, now).copy(id = 0)))
    }

    override suspend fun update(transaction: Transaction): WriteResult<Unit> = write {
        val existing = dao.transaction(transaction.id) ?: return@write WriteResult.Failure.NOT_FOUND
        if (!valid(transaction)) return@write WriteResult.Failure.INVALID_INPUT
        dao.updateTransaction(entity(transaction, existing.createdAt).copy(dedupKey = existing.dedupKey))
        WriteResult.Success(Unit)
    }

    override suspend fun delete(id: Long): WriteResult<Unit> = write {
        if (dao.deleteTransaction(id) == 0) WriteResult.Failure.NOT_FOUND else WriteResult.Success(Unit)
    }

    override suspend fun confirm(id: Long) = setStatus(id, TransactionStatus.USER_CONFIRMED)
    override suspend fun reject(id: Long) = setStatus(id, TransactionStatus.REJECTED)

    private suspend fun setStatus(id: Long, status: TransactionStatus): WriteResult<Unit> = write {
        val existing = dao.transaction(id) ?: return@write WriteResult.Failure.NOT_FOUND
        if (existing.origin != "NOTIFICATION") return@write WriteResult.Failure.INVALID_INPUT
        dao.updateTransaction(existing.copy(status = status.name, updatedAt = System.currentTimeMillis()))
        WriteResult.Success(Unit)
    }

    override fun observeSummary(range: TimeRange) = observe(TransactionFilter(range = range)).map { rows ->
        val counted = rows.filter { it.status == TransactionStatus.AUTO_RECORDED || it.status == TransactionStatus.USER_CONFIRMED }
        Summary(
            counted.filter { it.type == TransactionType.INCOME }.fold(0L) { total, row -> Math.addExact(total, row.amount) },
            counted.filter { it.type == TransactionType.EXPENSE }.fold(0L) { total, row -> Math.addExact(total, row.amount) },
        )
    }

    override fun observeSpendingByCategory(range: TimeRange) = observe(TransactionFilter(range = range)).map { rows ->
        rows.filter { it.type == TransactionType.EXPENSE && (it.status == TransactionStatus.AUTO_RECORDED || it.status == TransactionStatus.USER_CONFIRMED) }
            .groupBy { it.categoryId }.map { (id, group) ->
                CategorySpending(id, group.fold(0L) { total, row -> Math.addExact(total, row.amount) })
            }
    }

    private suspend fun valid(transaction: Transaction): Boolean {
        if (transaction.amount == 0L || transaction.type != TransactionType.ADJUSTMENT && transaction.amount < 0L) return false
        if (transaction.origin == TransactionOrigin.MANUAL && transaction.status != TransactionStatus.USER_CONFIRMED) return false
        val account = dao.account(transaction.accountId) ?: return false
        if (account.isArchived) return false
        if (transaction.type == TransactionType.TRANSFER) {
            val target = transaction.toAccountId ?: return false
            if (target == transaction.accountId || dao.account(target)?.isArchived != false) return false
        } else if (transaction.toAccountId != null) return false
        if (transaction.categoryId != null) {
            val category = database.categoryDao().findById(transaction.categoryId!!) ?: return false
            if (category.isArchived) return false
            val requiredType = when (transaction.type) {
                TransactionType.EXPENSE, TransactionType.REFUND -> "EXPENSE"
                TransactionType.INCOME -> "INCOME"
                else -> return false
            }
            if (category.type != requiredType) return false
        }
        return true
    }

    private fun counted(row: TransactionEntity) = row.status == "AUTO_RECORDED" || row.status == "USER_CONFIRMED"

    private fun model(row: TransactionEntity) = Transaction(row.id, row.accountId, row.toAccountId, row.categoryId,
        TransactionType.valueOf(row.type), row.amount, row.note, row.occurredAt,
        TransactionStatus.valueOf(row.status), TransactionOrigin.valueOf(row.origin))

    private fun entity(transaction: Transaction, createdAt: Long) = TransactionEntity(transaction.id, transaction.accountId,
        transaction.toAccountId, transaction.categoryId, transaction.type.name, transaction.amount, transaction.note,
        transaction.occurredAtMillis, transaction.status.name, transaction.origin.name, createdAt, System.currentTimeMillis())

    private suspend fun <T> write(operation: suspend () -> WriteResult<T>): WriteResult<T> = try {
        database.withTransaction {
            val result = operation()
            if (result is WriteResult.Success) verifyMoneyRange()
            result
        }
    } catch (_: ArithmeticException) {
        WriteResult.Failure.INVALID_INPUT
    } catch (_: SQLiteConstraintException) {
        WriteResult.Failure.CONFLICT
    } catch (_: SQLiteException) {
        WriteResult.Failure.STORAGE_ERROR
    }

    internal suspend fun verifyMoneyRange() {
        val transactions = dao.transactions().filter { counted(it) }
        var totalVisibleBalance = BigInteger.ZERO
        for (account in dao.accounts()) {
            val balance = transactions.fold(BigInteger.valueOf(account.initialBalance)) { total, row ->
                val amount = BigInteger.valueOf(row.amount)
                when {
                    row.type == "TRANSFER" && row.toAccountId == account.id -> total + amount
                    row.accountId != account.id -> total
                    row.type == "EXPENSE" || row.type == "TRANSFER" -> total - amount
                    else -> total + amount
                }
            }
            balance.toLongExactCompat()
            if (!account.isArchived) totalVisibleBalance += balance
        }
        totalVisibleBalance.toLongExactCompat()
        for (type in listOf("INCOME", "EXPENSE")) {
            transactions.filter { it.type == type }.fold(0L) { total, row -> Math.addExact(total, row.amount) }
        }
    }
}
