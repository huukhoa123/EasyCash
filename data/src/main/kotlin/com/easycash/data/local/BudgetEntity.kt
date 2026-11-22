package com.easycash.data.local

import androidx.room.*

@Entity(tableName = "budgets", indices = [Index(value = ["category_id", "month"], unique = true)],
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.CASCADE)])
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val month: Int,
    @ColumnInfo(name = "limit_amount") val limitAmount: Long,
    @ColumnInfo(name = "warn_threshold_percent") val warnThresholdPercent: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
