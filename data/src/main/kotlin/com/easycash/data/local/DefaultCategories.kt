package com.easycash.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

object DefaultCategories : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            db.query("SELECT COUNT(*) FROM categories WHERE is_system = 1").use { cursor ->
                cursor.moveToFirst()
                if (cursor.getLong(0) > 0) {
                    db.setTransactionSuccessful()
                    return
                }
            }
            insert(db, "Ăn uống", "EXPENSE", "food", "orange", now)
            insert(db, "Di chuyển", "EXPENSE", "transit", "blue", now)
            insert(db, "Mua sắm", "EXPENSE", "shopping", "purple", now)
            insert(db, "Hoá đơn", "EXPENSE", "bill", "red", now)
            insert(db, "Lương", "INCOME", "salary", "green", now)
            insert(db, "Thưởng", "INCOME", "bonus", "emerald", now)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun insert(db: SupportSQLiteDatabase, name: String, type: String, icon: String, color: String, now: Long) {
        db.execSQL(
            "INSERT OR IGNORE INTO categories (name, type, icon_key, color_key, is_system, is_archived, sort_order, created_at, updated_at) VALUES (?, ?, ?, ?, 1, 0, 0, ?, ?)",
            arrayOf<Any>(name, type, icon, color, now, now),
        )
    }
}
