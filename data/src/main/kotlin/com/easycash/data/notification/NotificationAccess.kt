package com.easycash.data.notification

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

fun observeNotificationAccess(context: Context) = callbackFlow {
    val resolver = context.contentResolver
    val setting = "enabled_notification_listeners"
    val component = ComponentName(context.packageName, "com.easycash.data.notification.EasyCashNotificationListener")
    fun emitCurrent() {
        val enabled = Settings.Secure.getString(resolver, setting).orEmpty().split(':')
            .mapNotNull { ComponentName.unflattenFromString(it) }
        trySend(component in enabled)
    }
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = emitCurrent()
    }
    resolver.registerContentObserver(Settings.Secure.getUriFor(setting), false, observer)
    emitCurrent()
    awaitClose { resolver.unregisterContentObserver(observer) }
}.distinctUntilChanged()
