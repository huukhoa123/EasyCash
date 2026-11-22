package com.easycash.domain

interface DataManagementRepository {
    suspend fun exportBackup(password: CharArray): WriteResult<ByteArray>
    suspend fun restoreBackup(bytes: ByteArray, password: CharArray): WriteResult<Unit>
    suspend fun exportCsv(): WriteResult<String>
    suspend fun clearAll(): WriteResult<Unit>
}
