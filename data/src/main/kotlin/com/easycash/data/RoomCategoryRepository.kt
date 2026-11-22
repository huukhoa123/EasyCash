package com.easycash.data

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.local.CategoryEntity
import com.easycash.domain.Category
import com.easycash.domain.CategoryRepository
import com.easycash.domain.CategoryType
import com.easycash.domain.WriteResult
import kotlinx.coroutines.flow.map

class RoomCategoryRepository(private val database: EasyCashDatabase) : CategoryRepository {
    private val dao = database.categoryDao()

    override fun observeAll(type: CategoryType) = dao.observeAll(type.name).map { rows ->
        rows.map { row ->
            Category(row.id, row.name, CategoryType.valueOf(row.type), row.iconKey, row.colorKey, row.isSystem)
        }
    }

    override suspend fun add(category: Category): WriteResult<Long> {
        if (!valid(category) || category.isSystem) return WriteResult.Failure.INVALID_INPUT
        return write {
            val now = System.currentTimeMillis()
            WriteResult.Success(dao.insert(CategoryEntity(
                name = category.name.trim(), type = category.type.name,
                iconKey = category.iconKey, colorKey = category.colorKey,
                isSystem = false, createdAt = now, updatedAt = now,
            )))
        }
    }

    override suspend fun update(category: Category): WriteResult<Unit> {
        if (!valid(category)) return WriteResult.Failure.INVALID_INPUT
        return write {
            val existing = dao.findById(category.id) ?: return@write WriteResult.Failure.NOT_FOUND
            if (existing.isSystem != category.isSystem) return@write WriteResult.Failure.INVALID_INPUT
            if (existing.type != category.type.name && dao.referenceCount(category.id) > 0) return@write WriteResult.Failure.CONFLICT
            dao.update(existing.copy(
                name = category.name.trim(), type = category.type.name,
                iconKey = category.iconKey, colorKey = category.colorKey,
                updatedAt = System.currentTimeMillis(),
            ))
            WriteResult.Success(Unit)
        }
    }

    override suspend fun archive(id: Long): WriteResult<Unit> = write {
        val existing = dao.findById(id) ?: return@write WriteResult.Failure.NOT_FOUND
        if (existing.isSystem) return@write WriteResult.Failure.INVALID_INPUT
        dao.archive(id, System.currentTimeMillis())
        WriteResult.Success(Unit)
    }

    private suspend fun <T> write(operation: suspend () -> WriteResult<T>): WriteResult<T> = try {
        database.withTransaction { operation() }
    } catch (_: SQLiteConstraintException) {
        WriteResult.Failure.CONFLICT
    } catch (_: SQLiteException) {
        WriteResult.Failure.STORAGE_ERROR
    }

    companion object {
        internal fun valid(category: Category) =
            category.name.isNotBlank() && category.iconKey.isNotBlank() && category.colorKey.isNotBlank()
    }
}
