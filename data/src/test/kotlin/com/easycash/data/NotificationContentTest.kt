package com.easycash.data

import android.app.Notification
import android.os.Bundle
import com.easycash.data.notification.*
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class NotificationContentTest {
    private val fixture = javaClass.getResource("/notifications/bidv/debit.txt")!!.readText()

    @Test
    fun `sensitive short text cannot hide behind expanded transaction`() {
        for (key in listOf(Notification.EXTRA_TITLE, Notification.EXTRA_TEXT, Notification.EXTRA_SUB_TEXT, Notification.EXTRA_SUMMARY_TEXT)) {
            val notification = Notification().apply {
                extras = Bundle().apply {
                    putCharSequence(Notification.EXTRA_BIG_TEXT, fixture)
                    putCharSequence(key, "Mã xác thực giả 123456")
                }
            }
            assertEquals(BidvParseResult.Ignored, BidvNotificationParser.parse(notificationText(notification), ZoneId.of("UTC")))
        }
        val lines = Notification().apply {
            extras = Bundle().apply {
                putCharSequence(Notification.EXTRA_BIG_TEXT, fixture)
                putCharSequenceArray(Notification.EXTRA_TEXT_LINES, arrayOf("OTP giả 123456"))
            }
        }
        assertEquals("OTP", notificationText(lines))
    }

    @Test
    fun `blank expanded text falls back to short body`() {
        val notification = Notification().apply {
            extras = Bundle().apply {
                putCharSequence(Notification.EXTRA_BIG_TEXT, " ")
                putCharSequence(Notification.EXTRA_TEXT, fixture)
            }
        }
        assertEquals(fixture, notificationText(notification))
    }
}
