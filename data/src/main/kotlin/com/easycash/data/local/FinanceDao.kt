package com.easycash.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts")
    suspend fun accounts(): List<AccountEntity>

    @Query("SELECT * FROM transactions")
    suspend fun transactions(): List<TransactionEntity>

    @Query("SELECT * FROM accounts ORDER BY id")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun account(id: Long): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts WHERE name = :name AND is_archived = 0 AND id != :exceptId")
    suspend fun nameCount(name: String, exceptId: Long): Int

    @Insert
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("SELECT * FROM transactions ORDER BY occurred_at DESC, id DESC")
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun transaction(id: Long): TransactionEntity?

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long): Int
}
