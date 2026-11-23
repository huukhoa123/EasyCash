package com.easycash.data

import android.content.ComponentName
import android.provider.Settings
import com.easycash.data.notification.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class NotificationListenerRecoveryTest {
    @Test
    fun `rebind only disconnected authorized component and respect revocation`() {
        val context = RuntimeEnvironment.getApplication()
        val component = ComponentName(context, EasyCashNotificationListener::class.java)
        val setting = "enabled_notification_listeners"
        val previous = Settings.Secure.getString(context.contentResolver, setting)
        val requests = mutableListOf<ComponentName>()
        try {
            ListenerConnection.connected = false
            Settings.Secure.putString(context.contentResolver, setting, "other.app/other.app.Listener")
            assertFalse(requestListenerRecovery(context) { requests.add(it) })
            Settings.Secure.putString(context.contentResolver, setting, component.flattenToString())
            assertTrue(requestListenerRecovery(context) { requests.add(it) })
            assertEquals(listOf(component), requests)
            ListenerConnection.connected = true
            assertFalse(requestListenerRecovery(context) { error("Do not disrupt connected listener") })
            ListenerConnection.connected = false
            assertFalse(requestListenerRecovery(context) { throw SecurityException("revoked during call") })
            Settings.Secure.putString(context.contentResolver, setting, "")
            assertFalse(requestListenerRecovery(context) { error("Must respect revoked permission") })
        } finally {
            ListenerConnection.connected = false
            Settings.Secure.putString(context.contentResolver, setting, previous)
        }
    }
}
