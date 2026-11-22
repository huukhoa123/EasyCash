package com.easycash.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT (SELECT COUNT(*) FROM transactions WHERE category_id = :id) + (SELECT COUNT(*) FROM budgets WHERE category_id = :id) + (SELECT COUNT(*) FROM recurring_transactions WHERE category_id = :id)")
    suspend fun referenceCount(id: Long): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE type = :type AND is_archived = 0 ORDER BY sort_order, id")
    fun observeAll(type: String): Flow<List<CategoryEntity>>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity): Int

    @Query("UPDATE categories SET is_archived = 1, updated_at = :updatedAt WHERE id = :id AND is_system = 0")
    suspend fun archive(id: Long, updatedAt: Long): Int
}
