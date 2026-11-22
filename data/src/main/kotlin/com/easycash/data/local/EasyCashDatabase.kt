package com.easycash.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CategoryEntity::class, AccountEntity::class, TransactionEntity::class, NotificationSourceEntity::class, NotificationEventEntity::class, NotificationReceiptEntity::class, BudgetEntity::class, ReminderEntity::class, AlertReceiptEntity::class], version = 6, exportSchema = true)
abstract class EasyCashDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun financeDao(): FinanceDao
    abstract fun notificationDao(): NotificationDao
    abstract fun budgetDao(): BudgetDao
    abstract fun reminderDao(): ReminderDao
}
