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

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != BidvNotificationParser.PACKAGE_NAME) return
        scope.launch {
            try {
                val processor = EntryPointAccessors.fromApplication(applicationContext, NotificationProcessorEntryPoint::class.java)
                    .notificationProcessor()
                processor.process(sbn.packageName, sbn.postTime, ZoneId.systemDefault()) {
                    notificationText(sbn.notification)
                }
            } catch (_: SQLiteException) {
                // Không ghi log lỗi SQLite có thể chứa dữ liệu thông báo.
            }
        }
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
    val fields = listOf(
        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty(),
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString().orEmpty(),
        text,
        expanded,
    ) + extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).orEmpty().map { it.toString() }
    if (fields.any { BidvNotificationParser.containsSensitiveText(it) }) return "OTP"
    return expanded.ifBlank { text }
}
