package com.easycash.data

import com.easycash.data.notification.*
import com.easycash.domain.TransactionType
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class TimoNotificationParserTest {
    private val fixture = javaClass.getResource("/notifications/timo/credit.txt")!!.readText()

    @Test
    fun `observed credit parses with VN timezone and stable normalized dedup`() {
        val parsed = TimoNotificationParser.parse(fixture) as BidvParseResult.Parsed
        assertEquals(10000L, parsed.amount)
        assertEquals(TransactionType.INCOME, parsed.type)
        assertEquals(Instant.parse("2026-11-22T16:47:00Z").toEpochMilli(), parsed.occurredAtMillis)
        assertEquals(parsed.dedupKey, (TimoNotificationParser.parse(fixture.replace(Regex("\\s+"), " ")) as BidvParseResult.Parsed).dedupKey)
        assertNotEquals(parsed.dedupKey, (TimoNotificationParser.parse(fixture.replace("800.000", "810.000")) as BidvParseResult.Parsed).dedupKey)
        assertEquals(64, parsed.dedupKey.length)
    }

    @Test
    fun `reject incomplete invalid overflow debit and otp rather than guessing`() {
        for (invalid in listOf(fixture.substringBefore("Số dư hiện tại"), fixture.replace("10.000", "10.00"),
            fixture.replace("10.000", "9223372036854775808"), fixture.replace("22/11/2026", "31/02/2026"),
            fixture.replace("vừa tăng", "vừa giảm"), fixture.replace("10.000", "0"))) {
            assertEquals(BidvParseResult.Unknown, TimoNotificationParser.parse(invalid))
        }
        assertEquals(BidvParseResult.Ignored, TimoNotificationParser.parse("OTP giả 000000\n$fixture"))
        assertEquals(BidvParseResult.Ignored, TimoNotificationParser.parse("Mã xác minh giả 000000\n$fixture"))
    }
}
