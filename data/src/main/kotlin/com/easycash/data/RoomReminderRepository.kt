package com.easycash.data

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.local.ReminderEntity
import com.easycash.domain.WriteResult

class RoomReminderRepository(private val database: EasyCashDatabase) {
    private val dao = database.reminderDao()

    fun observeAll() = dao.observeAll()

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

    suspend fun delete(id: Long): WriteResult<Unit> = write {
        if (dao.delete(id) == 0) WriteResult.Failure.NOT_FOUND else WriteResult.Success(Unit)
    }

    private suspend fun <T> write(operation: suspend () -> WriteResult<T>): WriteResult<T> = try {
        database.withTransaction { operation() }
    } catch (_: SQLiteException) {
        WriteResult.Failure.STORAGE_ERROR
    }
}
