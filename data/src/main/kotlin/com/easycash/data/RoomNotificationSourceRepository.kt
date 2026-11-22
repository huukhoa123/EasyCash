package com.easycash.data

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.notification.SupportedNotificationSources
import com.easycash.domain.NotificationSource
import com.easycash.domain.NotificationSourceRepository
import com.easycash.domain.WriteResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class RoomNotificationSourceRepository(
    private val database: EasyCashDatabase,
    private val accessGranted: Flow<Boolean>,
) : NotificationSourceRepository {
    private val dao = database.notificationDao()

    override fun observeAll() = dao.observeSources().onStart {
        val now = System.currentTimeMillis()
        SupportedNotificationSources.defaults(now).forEach { dao.insertSource(it) }
    }.map { sources -> sources.map { NotificationSource(it.id, it.packageName, it.displayName, it.isEnabled, it.linkedAccountId) } }

    override fun observeAccessGranted() = accessGranted

    override suspend fun setEnabled(id: Long, isEnabled: Boolean): WriteResult<Unit> = write {
        val source = dao.source(id) ?: return@write WriteResult.Failure.NOT_FOUND
        if (!SupportedNotificationSources.supports(source.packageName)) return@write WriteResult.Failure.INVALID_INPUT
        dao.updateSource(source.copy(isEnabled = isEnabled, updatedAt = System.currentTimeMillis()))
        WriteResult.Success(Unit)
    }

    override suspend fun linkAccount(id: Long, accountId: Long?): WriteResult<Unit> = write {
        val source = dao.source(id) ?: return@write WriteResult.Failure.NOT_FOUND
        if (accountId != null && database.financeDao().account(accountId)?.isArchived != false) {
            return@write WriteResult.Failure.INVALID_INPUT
        }
        dao.updateSource(source.copy(linkedAccountId = accountId, updatedAt = System.currentTimeMillis()))
        WriteResult.Success(Unit)
    }

    private suspend fun <T> write(operation: suspend () -> WriteResult<T>): WriteResult<T> = try {
        database.withTransaction { operation() }
    } catch (_: SQLiteException) {
        WriteResult.Failure.STORAGE_ERROR
    }
}
