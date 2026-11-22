package com.easycash.data.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.database.sqlite.SQLiteException
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import java.time.ZoneId

class EasyCashNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            while (isActive) {
                try {
                    EntryPointAccessors.fromApplication(applicationContext, NotificationProcessorEntryPoint::class.java)
                        .notificationProcessor().cleanup()
                } catch (_: SQLiteException) {
                    // Thử lại ở chu kỳ sau, không log dữ liệu SQLite.
                }
                delay(24L * 60 * 60 * 1000)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        publishListenerDiagnostic(this, "Bộ đọc đã kết nối. Đang kiểm tra thông báo BIDV còn hiện.")
        activeNotifications.orEmpty().forEach { onNotificationPosted(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != BidvNotificationParser.PACKAGE_NAME) return
        scope.launch {
            try {
                val processor = EntryPointAccessors.fromApplication(applicationContext, NotificationProcessorEntryPoint::class.java)
                    .notificationProcessor()
                val outcome = processor.process(sbn.packageName, sbn.postTime, ZoneId.systemDefault()) {
                    notificationText(sbn.notification)
                }
                outcome.diagnosticMessage()?.let { publishListenerDiagnostic(this@EasyCashNotificationListener, it) }
            } catch (_: SQLiteException) {
                publishListenerDiagnostic(this@EasyCashNotificationListener, "Bộ đọc gặp lỗi lưu trữ. Không có nội dung ngân hàng trong chẩn đoán.")
            }
        }
    }

    override fun onListenerDisconnected() {
        publishListenerDiagnostic(this, "Bộ đọc đã mất kết nối Android. Tắt/bật lại quyền đọc thông báo.")
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

internal fun notificationText(notification: Notification): String {
    val extras = notification.extras
    val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
    val expanded = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
    val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).orEmpty().map { it.toString() }
    val fields = listOf(
        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty(),
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString().orEmpty(),
        text,
        expanded,
    ) + lines
    if (fields.any { BidvNotificationParser.containsSensitiveText(it) }) return "OTP"
    return expanded.ifBlank { lines.filter { it.isNotBlank() }.joinToString("\n").ifBlank { text } }
}
