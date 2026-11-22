package com.easycash.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE recurring_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, amount INTEGER, account_id INTEGER NOT NULL, category_id INTEGER, frequency TEXT NOT NULL, day_rule INTEGER NOT NULL, next_due_at INTEGER NOT NULL, remind_before_days INTEGER NOT NULL, is_active INTEGER NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(category_id) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
        db.execSQL("CREATE INDEX index_recurring_transactions_is_active_next_due_at ON recurring_transactions(is_active, next_due_at)")
        db.execSQL("CREATE INDEX index_recurring_transactions_account_id ON recurring_transactions(account_id)")
        db.execSQL("CREATE INDEX index_recurring_transactions_category_id ON recurring_transactions(category_id)")
        db.execSQL("CREATE TABLE alert_receipts (\u0060key\u0060 TEXT NOT NULL PRIMARY KEY)")
    }
}
