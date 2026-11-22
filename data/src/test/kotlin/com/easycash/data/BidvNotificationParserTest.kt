package com.easycash.data

import com.easycash.data.notification.BidvNotificationParser
import com.easycash.data.notification.BidvParseResult
import com.easycash.domain.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BidvNotificationParserTest {
    private val fixture = javaClass.getResource("/notifications/bidv/debit.txt")!!.readText()
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun `parse redacted debit fixture and derive stable reference hash`() {
        val result = BidvNotificationParser.parse(fixture, zone) as BidvParseResult.Parsed
        assertEquals(10000L, result.amount)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(Instant.parse("2026-11-20T04:30:00Z").toEpochMilli(), result.occurredAtMillis)
        assertEquals(64, result.dedupKey.length)
        assertEquals(result.dedupKey, (BidvNotificationParser.parse(fixture.replace("90,000", "80,000"), zone) as BidvParseResult.Parsed).dedupKey)
        assertFalse(result.dedupKey.contains("TEST"))
    }

    @Test
    fun `observed push without preamble or reference parses and deduplicates wrapped content`() {
        val push = javaClass.getResource("/notifications/bidv/push-debit.txt")!!.readText()
        val result = BidvNotificationParser.parse(push, zone) as BidvParseResult.Parsed
        assertEquals(10000L, result.amount)
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(Instant.parse("2026-11-22T15:45:00Z").toEpochMilli(), result.occurredAtMillis)
        val unwrapped = push.replace("MB-TKThe\n", "MB-TKThe ").replace("NGUOI THU\n", "NGUOI THU ")
        assertEquals(result.dedupKey, (BidvNotificationParser.parse(unwrapped, zone) as BidvParseResult.Parsed).dedupKey)
        assertNotEquals(result.dedupKey, (BidvNotificationParser.parse(push.replace("90,000", "80,000"), zone) as BidvParseResult.Parsed).dedupKey)
        assertEquals(TransactionType.INCOME, (BidvNotificationParser.parse(push.replace("-10,000", "+10,000"), zone) as BidvParseResult.Parsed).type)
        for (invalid in listOf(push.substringBefore("Số tiền GD"), push.substringBefore("Nội dung giao dịch"), push.replace("0000000001", "…"))) {
            assertEquals(BidvParseResult.Unknown, BidvNotificationParser.parse(invalid, zone))
        }
        assertEquals(BidvParseResult.Ignored, BidvNotificationParser.parse("OTP giả 123456\n$push", zone))
    }

    @Test
    fun `reject sensitive failed truncated invalid and overflowing notifications`() {
        assertEquals(BidvParseResult.Ignored, BidvNotificationParser.parse("OTP giả 123456\n$fixture", zone))
        assertEquals(BidvParseResult.Ignored, BidvNotificationParser.parse("$fixture\nGiao dịch thất bại", zone))
        for (text in listOf(fixture.substringBefore("Mã giao dịch"), fixture.replace("-10,000", "-10,00"),
            fixture.replace("20/11/2026", "31/02/2026"), fixture.replace("-10,000", "-9223372036854775808"),
            fixture.replace("-10,000", "0"), "$fixture\nSố tiền GD: -20,000 VND")) {
            assertEquals(BidvParseResult.Unknown, BidvNotificationParser.parse(text, zone))
        }
    }
}
