package com.easycash.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.easycash.data.local.DefaultCategories
import com.easycash.data.local.EasyCashDatabase
import com.easycash.domain.DataManagementRepository
import com.easycash.domain.WriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.security.GeneralSecurityException

class RoomDataManagementRepository(private val database: EasyCashDatabase) : DataManagementRepository {
    private val tables = listOf("accounts", "categories", "notification_sources", "transactions",
        "notification_events", "notification_receipts", "budgets", "recurring_transactions", "alert_receipts")

    override suspend fun exportBackup(password: CharArray): WriteResult<ByteArray> = perform {
        val snapshot = database.withTransaction {
            val db = database.openHelper.writableDatabase
            val root = JSONObject().put("schema", db.version)
            for (table in tables) {
                val rows = JSONArray()
                db.query("SELECT * FROM $table").use { cursor ->
                    while (cursor.moveToNext()) {
                        val row = JSONObject()
                        for (index in 0 until cursor.columnCount) {
                            val value = when (cursor.getType(index)) {
                                Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                                Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
                                else -> error("Unsupported backup column")
                            }
                            row.put(cursor.getColumnName(index), value)
                        }
                        rows.put(row)
                    }
                }
                root.put(table, rows)
            }
            root.toString().toByteArray(Charsets.UTF_8)
        }
        try {
            WriteResult.Success(BackupCrypto.encrypt(snapshot, password))
        } finally {
            snapshot.fill(0)
        }
    }

    override suspend fun restoreBackup(bytes: ByteArray, password: CharArray): WriteResult<Unit> = perform {
        val plain = BackupCrypto.decrypt(bytes, password)
        val snapshot = try {
            JSONObject(plain.toString(Charsets.UTF_8))
        } finally {
            plain.fill(0)
        }
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            require(snapshot.get("schema") is Int || snapshot.get("schema") is Long)
            if (snapshot.getLong("schema") == 5L && db.version == 6) {
                val oldTables = tables - setOf("recurring_transactions", "alert_receipts")
                require(snapshot.keys().asSequence().toSet() == oldTables.toSet() + "schema")
                snapshot.put("recurring_transactions", JSONArray())
                snapshot.put("alert_receipts", JSONArray())
                snapshot.put("schema", 6)
            }
            require(snapshot.getLong("schema") == db.version.toLong())
            require(snapshot.keys().asSequence().toSet() == tables.toSet() + "schema")
            for (table in tables.reversed()) db.execSQL("DELETE FROM $table")
            for (table in tables) {
                val columns = db.query("PRAGMA table_info($table)").use { cursor ->
                    buildMap { while (cursor.moveToNext()) put(cursor.getString(1), cursor.getString(2)) }
                }
                val rows = snapshot.getJSONArray(table)
                for (index in 0 until rows.length()) {
                    val row = rows.getJSONObject(index)
                    require(row.keys().asSequence().toSet() == columns.keys)
                    val values = ContentValues()
                    for ((column, type) in columns) {
                        val raw = row.get(column)
                        require(raw == JSONObject.NULL || type == "INTEGER" && (raw is Int || raw is Long) || type == "TEXT" && raw is String)
                        when (val value = row.get(column)) {
                            JSONObject.NULL -> values.putNull(column)
                            is Int -> values.put(column, value)
                            is Long -> values.put(column, value)
                            is String -> values.put(column, value)
                            else -> throw IllegalArgumentException("Invalid backup value")
                        }
                    }
                    validateRow(table, row)
                    if (table == "notification_sources") values.put("is_enabled", 0)
                    db.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
                }
            }
            db.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) }
            db.query("SELECT name FROM accounts WHERE is_archived = 0 GROUP BY name HAVING COUNT(*) > 1").use { require(!it.moveToFirst()) }
            db.query("SELECT b.id FROM budgets b JOIN categories c ON c.id = b.category_id WHERE c.type != 'EXPENSE'").use { require(!it.moveToFirst()) }
            db.query("SELECT r.id FROM recurring_transactions r JOIN categories c ON c.id = r.category_id WHERE r.type != c.type").use { require(!it.moveToFirst()) }
            db.query("SELECT t.id FROM transactions t JOIN categories c ON c.id = t.category_id WHERE (t.type IN ('EXPENSE', 'REFUND') AND c.type != 'EXPENSE') OR (t.type = 'INCOME' AND c.type != 'INCOME') OR t.type IN ('TRANSFER', 'ADJUSTMENT')").use { require(!it.moveToFirst()) }
            RoomFinanceRepository(database).verifyMoneyRange()
        }
        WriteResult.Success(Unit)
    }

    override suspend fun exportCsv(): WriteResult<String> = perform {
        val output = StringBuilder("\uFEFFid,account_id,to_account_id,category_id,type,amount,note,occurred_at,status,origin\r\n")
        database.withTransaction {
            database.openHelper.readableDatabase.query("SELECT id, account_id, to_account_id, category_id, type, amount, note, occurred_at, status, origin FROM transactions ORDER BY occurred_at, id").use { cursor ->
                while (cursor.moveToNext()) {
                    val cells = (0 until cursor.columnCount).map { index ->
                        var value = if (cursor.isNull(index)) "" else cursor.getString(index)
                        if (index == 6 && value.trimStart().firstOrNull() in listOf('=', '+', '-', '@', '\t', '\r')) value = "'$value"
                        "\"${value.replace("\"", "\"\"")}\""
                    }
                    output.append(cells.joinToString(",")).append("\r\n")
                }
            }
        }
        WriteResult.Success(output.toString())
    }

    override suspend fun clearAll(): WriteResult<Unit> = perform {
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            for (table in tables.reversed()) db.execSQL("DELETE FROM $table")
            db.execSQL("DELETE FROM sqlite_sequence")
            DefaultCategories.onOpen(db)
            val now = System.currentTimeMillis()
            com.easycash.data.notification.SupportedNotificationSources.defaults(now).forEach {
                database.notificationDao().insertSource(it)
            }
        }
        WriteResult.Success(Unit)
    }

    private fun validateRow(table: String, row: JSONObject) {
        if (row.has("id")) require(row.getLong("id") > 0)
        for (flag in listOf("is_archived", "is_system", "is_enabled", "is_active")) {
            if (row.has(flag)) require(row.getLong(flag) in 0L..1L)
        }
        if (table in listOf("notification_events", "notification_receipts")) require(row.getString("fingerprint").matches(Regex("[a-f0-9]{64}")))
        if (table == "accounts" || table == "categories") require(row.getString("name").isNotBlank())
        if (table == "accounts") require(row.getString("type") in listOf("CASH", "BANK", "EWALLET"))
        if (table == "categories") require(row.getString("type") in listOf("INCOME", "EXPENSE"))
        if (table == "transactions") {
            val type = row.getString("type")
            require(type in listOf("INCOME", "EXPENSE", "TRANSFER", "REFUND", "ADJUSTMENT"))
            require(row.getLong("amount") != 0L && (type == "ADJUSTMENT" || row.getLong("amount") > 0))
            require(row.getString("status") in listOf("AUTO_RECORDED", "USER_CONFIRMED", "NEEDS_REVIEW", "REJECTED"))
            require(row.getString("origin") in listOf("MANUAL", "NOTIFICATION"))
            if (row.getString("origin") == "MANUAL") require(row.getString("status") == "USER_CONFIRMED")
            if (type == "TRANSFER") require(!row.isNull("to_account_id") && row.getLong("to_account_id") != row.getLong("account_id"))
            else require(row.isNull("to_account_id"))
        }
        if (table == "notification_events") {
            require(row.getString("classification") in listOf("TRANSACTION_SUCCESS", "TRANSACTION_FAILED", "PROMO", "UNKNOWN"))
            require(row.getString("parse_status") in listOf("PARSED", "PARSE_FAILED", "IGNORED"))
        }
        if (table == "recurring_transactions") {
            require(row.getString("name").isNotBlank())
            require(row.getString("type") in listOf("INCOME", "EXPENSE"))
            require(row.getString("frequency") in listOf("WEEKLY", "MONTHLY", "YEARLY"))
            require(row.getLong("day_rule") in 1L..31L)
            if (row.getString("frequency") == "WEEKLY") require(row.getLong("day_rule") <= 7)
            require(row.getLong("next_due_at") >= 0)
            require(row.getLong("remind_before_days") in 0L..30L)
            require(row.isNull("amount") || row.getLong("amount") > 0)
        }
        if (table == "budgets") {
            val month = row.getLong("month")
            require(month / 100 in 1L..9999L && month % 100 in 1L..12L)
            require(row.getLong("limit_amount") > 0 && row.getLong("warn_threshold_percent") in 1L..100L)
        }
    }

    private suspend fun <T> perform(operation: suspend () -> WriteResult<T>): WriteResult<T> = withContext(Dispatchers.IO) {
        try {
            operation()
        } catch (_: GeneralSecurityException) {
            WriteResult.Failure.INVALID_INPUT
        } catch (_: IllegalArgumentException) {
            WriteResult.Failure.INVALID_INPUT
        } catch (_: ArithmeticException) {
            WriteResult.Failure.INVALID_INPUT
        } catch (_: JSONException) {
            WriteResult.Failure.INVALID_INPUT
        } catch (_: SQLiteConstraintException) {
            WriteResult.Failure.INVALID_INPUT
        } catch (_: SQLiteException) {
            WriteResult.Failure.STORAGE_ERROR
        }
    }
}
