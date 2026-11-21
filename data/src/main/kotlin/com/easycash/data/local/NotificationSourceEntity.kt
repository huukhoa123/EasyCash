package com.easycash.data.local

import androidx.room.*

@Entity(tableName = "notification_sources", indices = [Index(value = ["package_name"], unique = true), Index("linked_account_id")],
    foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["linked_account_id"], onDelete = ForeignKey.SET_NULL)])
data class NotificationSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "parser_id") val parserId: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = false,
    @ColumnInfo(name = "linked_account_id") val linkedAccountId: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
