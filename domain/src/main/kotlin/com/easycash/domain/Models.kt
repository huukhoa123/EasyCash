package com.easycash.domain

enum class TransactionType { INCOME, EXPENSE, TRANSFER, REFUND, ADJUSTMENT }
enum class TransactionStatus { AUTO_RECORDED, USER_CONFIRMED, NEEDS_REVIEW, REJECTED }
enum class TransactionOrigin { MANUAL, NOTIFICATION }
enum class AccountType { CASH, BANK, EWALLET }
enum class CategoryType { INCOME, EXPENSE }

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val initialBalance: Long,
    val estimatedBalance: Long,
    val isArchived: Boolean,
)

data class Category(
    val id: Long,
    val name: String,
    val type: CategoryType,
    val iconKey: String,
    val colorKey: String,
    val isSystem: Boolean,
)

data class Transaction(
    val id: Long,
    val accountId: Long,
    val toAccountId: Long?,
    val categoryId: Long?,
    val type: TransactionType,
    val amount: Long,
    val note: String,
    val occurredAtMillis: Long,
    val status: TransactionStatus,
    val origin: TransactionOrigin,
)

data class Budget(
    val id: Long,
    val categoryId: Long,
    val month: Int,
    val limitAmount: Long,
    val warnThresholdPercent: Int,
)

data class BudgetUsage(val budget: Budget, val spent: Long)

data class NotificationSource(
    val id: Long,
    val packageName: String,
    val displayName: String,
    val isEnabled: Boolean,
    val linkedAccountId: Long?,
)

data class TimeRange(val startInclusiveMillis: Long, val endExclusiveMillis: Long)

data class TransactionFilter(
    val range: TimeRange? = null,
    val type: TransactionType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val status: TransactionStatus? = null,
    val keyword: String? = null,
)

data class Summary(val income: Long, val expense: Long)
data class CategorySpending(val categoryId: Long?, val amount: Long)
