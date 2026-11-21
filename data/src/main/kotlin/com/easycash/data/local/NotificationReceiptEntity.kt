package com.easycash.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_receipts")
data class NotificationReceiptEntity(@PrimaryKey val fingerprint: String)
