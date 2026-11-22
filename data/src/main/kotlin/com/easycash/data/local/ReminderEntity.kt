package com.easycash.data.local

import androidx.room.*

@Entity(tableName = "recurring_transactions", indices = [Index(value = ["is_active", "next_due_at"]), Index("account_id"), Index("category_id")],
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.SET_NULL)])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val amount: Long?,
    @ColumnInfo(name = "account_id") val accountId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    val frequency: String,
    @ColumnInfo(name = "day_rule") val dayRule: Int,
    @ColumnInfo(name = "next_due_at") val nextDueAt: Long,
    @ColumnInfo(name = "remind_before_days") val remindBeforeDays: Int,
    @ColumnInfo(name = "is_active") val isActive: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "alert_receipts")
data class AlertReceiptEntity(@PrimaryKey val key: String)
