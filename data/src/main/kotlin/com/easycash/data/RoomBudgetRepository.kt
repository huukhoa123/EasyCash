package com.easycash.data

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.easycash.data.local.BudgetEntity
import com.easycash.data.local.EasyCashDatabase
import com.easycash.domain.*
import kotlinx.coroutines.flow.combine
import java.time.DateTimeException
import java.time.YearMonth
import java.time.ZoneId

class RoomBudgetRepository(private val database: EasyCashDatabase, private val zone: ZoneId = ZoneId.systemDefault()) : BudgetRepository {
    private val dao = database.budgetDao()

    override fun observeUsage(month: Int) = combine(dao.observe(month), database.financeDao().observeTransactions()) { budgets, transactions ->
        val yearMonth = YearMonth.of(month / 100, month % 100)
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        budgets.map { row ->
            val spent = transactions.filter {
                it.categoryId == row.categoryId && it.type == "EXPENSE" &&
                    it.status in listOf("AUTO_RECORDED", "USER_CONFIRMED") && it.occurredAt >= start && it.occurredAt < end
            }.fold(0L) { total, transaction -> Math.addExact(total, transaction.amount) }
            BudgetUsage(Budget(row.id, row.categoryId, row.month, row.limitAmount, row.warnThresholdPercent), spent)
        }
    }

    override suspend fun upsert(budget: Budget): WriteResult<Long> {
        if (budget.month / 100 !in 1..9999 || budget.limitAmount <= 0 || budget.warnThresholdPercent !in 1..100) return WriteResult.Failure.INVALID_INPUT
        try {
            YearMonth.of(budget.month / 100, budget.month % 100)
        } catch (_: DateTimeException) {
            return WriteResult.Failure.INVALID_INPUT
        }
        return write {
            val category = database.categoryDao().findById(budget.categoryId) ?: return@write WriteResult.Failure.INVALID_INPUT
            if (category.isArchived || category.type != "EXPENSE") return@write WriteResult.Failure.INVALID_INPUT
            val existing = dao.find(budget.categoryId, budget.month)
            val now = System.currentTimeMillis()
            val row = BudgetEntity(existing?.id ?: 0, budget.categoryId, budget.month, budget.limitAmount,
                budget.warnThresholdPercent, existing?.createdAt ?: now, now)
            if (existing == null) WriteResult.Success(dao.insert(row)) else {
                dao.update(row)
                WriteResult.Success(existing.id)
            }
        }
    }

    override suspend fun delete(id: Long): WriteResult<Unit> = write {
        if (dao.delete(id) == 0) WriteResult.Failure.NOT_FOUND else WriteResult.Success(Unit)
    }

    private suspend fun <T> write(operation: suspend () -> WriteResult<T>): WriteResult<T> = try {
        database.withTransaction { operation() }
    } catch (_: SQLiteException) {
        WriteResult.Failure.STORAGE_ERROR
    }
}
