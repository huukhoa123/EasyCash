package com.easycash.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN dedup_key TEXT")
        db.execSQL("CREATE UNIQUE INDEX index_transactions_dedup_key ON transactions(dedup_key)")
        db.execSQL("CREATE TABLE IF NOT EXISTS notification_receipts (fingerprint TEXT NOT NULL PRIMARY KEY)")
    }
}
