package com.easycash.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["to_account_id"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("occurred_at"), Index(value = ["account_id", "occurred_at"]), Index("to_account_id"), Index(value = ["category_id", "occurred_at"]), Index("status"), Index(value = ["dedup_key"], unique = true)],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "account_id") val accountId: Long,
    @ColumnInfo(name = "to_account_id") val toAccountId: Long?,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    val type: String,
    val amount: Long,
    val note: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    val status: String,
    val origin: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "dedup_key") val dedupKey: String? = null,
)
