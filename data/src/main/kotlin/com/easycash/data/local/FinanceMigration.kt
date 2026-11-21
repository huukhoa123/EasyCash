package com.easycash.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, initial_balance INTEGER NOT NULL, is_archived INTEGER NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_type ON accounts(type)")
        db.execSQL("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, account_id INTEGER NOT NULL, to_account_id INTEGER, category_id INTEGER, type TEXT NOT NULL, amount INTEGER NOT NULL, note TEXT NOT NULL, occurred_at INTEGER NOT NULL, status TEXT NOT NULL, origin TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(to_account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT, FOREIGN KEY(category_id) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_occurred_at ON transactions(occurred_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_account_id_occurred_at ON transactions(account_id, occurred_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_to_account_id ON transactions(to_account_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category_id_occurred_at ON transactions(category_id, occurred_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_status ON transactions(status)")
    }
}
