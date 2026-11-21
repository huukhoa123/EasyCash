package com.easycash.data

import android.content.ComponentName
import android.provider.Settings
import com.easycash.data.notification.observeNotificationAccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class NotificationAccessTest {
    @Test
    fun `permission requires exact EasyCash component and reflects revocation`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val resolver = context.contentResolver
        val setting = "enabled_notification_listeners"
        val original = Settings.Secure.getString(resolver, setting)
        try {
            Settings.Secure.putString(resolver, setting, "another.app/.Listener")
            assertFalse(observeNotificationAccess(context).first())
            val component = ComponentName(context.packageName, "com.easycash.data.notification.EasyCashNotificationListener")
            Settings.Secure.putString(resolver, setting, "another.app/.Listener:${component.flattenToString()}")
            assertTrue(observeNotificationAccess(context).first())
            Settings.Secure.putString(resolver, setting, "")
            assertFalse(observeNotificationAccess(context).first())
        } finally {
            Settings.Secure.putString(resolver, setting, original)
        }
    }
}
