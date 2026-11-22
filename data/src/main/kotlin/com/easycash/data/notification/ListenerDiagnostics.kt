package com.easycash.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

// Temporary on-device diagnostics for BIDV acceptance; remove after real-device verification.
internal fun publishListenerDiagnostic(context: Context, message: String) {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (!manager.areNotificationsEnabled()) return
    val channel = "bidv_diagnostics"
    manager.createNotificationChannel(NotificationChannel(channel, "Kiểm tra nhận BIDV", NotificationManager.IMPORTANCE_LOW))
    val notification = Notification.Builder(context, channel)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("EasyCash — kiểm tra BIDV")
        .setContentText(message)
        .setStyle(Notification.BigTextStyle().bigText(message))
        .setOnlyAlertOnce(true)
        .setVisibility(Notification.VISIBILITY_SECRET)
        .build()
    try {
        manager.notify(7303, notification)
    } catch (_: SecurityException) {
        // Sending permission may be revoked while the listener is processing.
    }
}

internal fun NotificationOutcome.diagnosticMessage(): String? = when (this) {
    NotificationOutcome.IGNORED -> null
    NotificationOutcome.SOURCE_DISABLED -> "Đã nhận sự kiện BIDV; nguồn BIDV trong EasyCash đang tắt."
    NotificationOutcome.UNKNOWN_FORMAT -> "Đã nhận BIDV; nội dung Android cung cấp chưa khớp định dạng."
    NotificationOutcome.ALREADY_RECORDED -> "BIDV này đã được ghi hoặc đã được xử lý trước đó."
    NotificationOutcome.NO_ACCOUNT -> "Đã đọc BIDV; chưa gán tài khoản trong EasyCash."
    NotificationOutcome.ACCOUNT_UNAVAILABLE -> "Đã đọc BIDV; tài khoản đã lưu trữ hoặc không còn tồn tại."
    NotificationOutcome.WRITE_FAILED -> "Đã đọc BIDV; không ghi được giao dịch vào dữ liệu."
    NotificationOutcome.RECORDED -> "Đã ghi BIDV. Mở Giao dịch → Chờ xem lại."
}
