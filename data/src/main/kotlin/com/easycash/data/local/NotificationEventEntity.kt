package com.easycash.data.local

import androidx.room.*

@Entity(tableName = "notification_events", indices = [Index(value = ["source_id", "fingerprint"], unique = true), Index("posted_at"), Index("expires_at")],
    foreignKeys = [ForeignKey(entity = NotificationSourceEntity::class, parentColumns = ["id"], childColumns = ["source_id"], onDelete = ForeignKey.CASCADE)])
data class NotificationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "source_id") val sourceId: Long,
    @ColumnInfo(name = "posted_at") val postedAt: Long,
    val fingerprint: String,
    val classification: String,
    @ColumnInfo(name = "parse_status") val parseStatus: String,
    @ColumnInfo(name = "parser_version") val parserVersion: Int,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
