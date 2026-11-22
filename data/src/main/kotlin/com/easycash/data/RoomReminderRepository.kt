package com.easycash.data

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.local.ReminderEntity
import com.easycash.domain.CategoryType
import com.easycash.domain.Reminder
import com.easycash.domain.ReminderFrequency
import com.easycash.domain.ReminderRepository
import com.easycash.domain.WriteResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomReminderRepository(private val database: EasyCashDatabase) : ReminderRepository {
    private val dao = database.reminderDao()

    override fun observeAll(): Flow<List<Reminder>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    override suspend fun upsert(reminder: Reminder): WriteResult<Long> = upsert(reminder.toEntity())

    suspend fun upsert(reminder: ReminderEntity): WriteResult<Long> = write {
        if (reminder.name.isBlank() || reminder.type !in listOf("INCOME", "EXPENSE") ||
            reminder.frequency !in listOf("WEEKLY", "MONTHLY", "YEARLY") ||
            reminder.nextDueAt < 0 || reminder.remindBeforeDays !in 0..30 || reminder.dayRule !in 1..31 ||
            reminder.frequency == "WEEKLY" && reminder.dayRule > 7 || reminder.amount != null && reminder.amount <= 0) {
            return@write WriteResult.Failure.INVALID_INPUT
        }
        if (database.financeDao().account(reminder.accountId)?.isArchived != false) return@write WriteResult.Failure.INVALID_INPUT
        val categoryId = reminder.categoryId
        if (categoryId != null) {
            val category = database.categoryDao().findById(categoryId) ?: return@write WriteResult.Failure.INVALID_INPUT
            if (category.isArchived || category.type != reminder.type) return@write WriteResult.Failure.INVALID_INPUT
        }
        val existing = if (reminder.id == 0L) null else dao.find(reminder.id) ?: return@write WriteResult.Failure.NOT_FOUND
        val now = System.currentTimeMillis()
        val row = reminder.copy(name = reminder.name.trim(), createdAt = existing?.createdAt ?: now, updatedAt = now)
        if (existing == null) WriteResult.Success(dao.insert(row)) else {
            dao.update(row)
            WriteResult.Success(row.id)
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

internal fun ReminderEntity.toDomain() = Reminder(
    id = id,
    name = name,
    accountId = accountId,
    categoryId = categoryId,
    type = CategoryType.valueOf(type),
    amount = amount,
    frequency = ReminderFrequency.valueOf(frequency),
    dayRule = dayRule,
    nextDueAtMillis = nextDueAt,
    remindBeforeDays = remindBeforeDays,
    isActive = isActive,
)

internal fun Reminder.toEntity(createdAt: Long = 0, updatedAt: Long = 0) = ReminderEntity(
    id = id,
    name = name,
    type = type.name,
    amount = amount,
    accountId = accountId,
    categoryId = categoryId,
    frequency = frequency.name,
    dayRule = dayRule,
    nextDueAt = nextDueAtMillis,
    remindBeforeDays = remindBeforeDays,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
