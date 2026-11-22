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
    fun `diagnostic notification contains only supplied status and hides lock screen contents`() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        publishListenerDiagnostic(context, NotificationOutcome.NO_ACCOUNT.diagnosticMessage()!!)
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val notice = org.robolectric.Shadows.shadowOf(manager).getNotification(7303)
        assertEquals(Notification.VISIBILITY_SECRET, notice.visibility)
        assertEquals(NotificationOutcome.NO_ACCOUNT.diagnosticMessage(), notice.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(null, NotificationOutcome.IGNORED.diagnosticMessage())
    }

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
    fun `inbox lines are parsed instead of truncated short body`() {
        val notification = Notification().apply {
            extras = Bundle().apply {
                putCharSequence(Notification.EXTRA_TEXT, "Thời gian giao dịch: 22:45 22/11/2026")
                putCharSequenceArray(Notification.EXTRA_TEXT_LINES, fixture.lines().toTypedArray())
            }
        }
        assertEquals(fixture.lineSequence().filter { it.isNotBlank() }.toList(), notificationText(notification).lines())
        org.junit.Assert.assertTrue(
            BidvNotificationParser.parse(notificationText(notification), ZoneId.of("UTC")) is BidvParseResult.Parsed
        )
    }

    @Test
    fun `collapsed notification without amount remains unknown`() {
        val notification = Notification().apply {
            extras = Bundle().apply {
                putCharSequence(Notification.EXTRA_TITLE, "Thông báo BIDV")
                putCharSequence(Notification.EXTRA_TEXT, "Thời gian giao dịch: 22:45 22/11/2026\nTài khoản thanh toán: 0000000000…")
            }
        }
        assertEquals(BidvParseResult.Unknown, BidvNotificationParser.parse(notificationText(notification), ZoneId.of("UTC")))
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
