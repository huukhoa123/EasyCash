package com.easycash.data.notification

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService

internal object ListenerConnection {
    @Volatile var connected = false
}

fun recoverNotificationListener(context: Context): Boolean = requestListenerRecovery(context)

internal fun requestListenerRecovery(
    context: Context,
    request: (ComponentName) -> Unit = NotificationListenerService::requestRebind,
): Boolean {
    if (ListenerConnection.connected) return false
    val component = ComponentName(context, EasyCashNotificationListener::class.java)
    val allowed = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        .orEmpty().split(':').mapNotNull(ComponentName::unflattenFromString).contains(component)
    if (!allowed) return false
    return try {
        request(component)
        true
    } catch (_: SecurityException) {
        false
    }
}
