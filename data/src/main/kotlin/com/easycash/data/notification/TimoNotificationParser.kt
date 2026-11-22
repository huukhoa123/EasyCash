package com.easycash.data.notification

import com.easycash.domain.TransactionType
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

object TimoNotificationParser {
    const val PACKAGE_NAME = "io.lifestyle.plus"
    const val VERSION = 1
    private const val money = "(?:[1-9][0-9]{0,2}(?:\\.[0-9]{3})+|[1-9][0-9]*)"
    private val credit = Regex("Số dư tài khoản vừa tăng ($money) vào ([0-9]{2}/[0-9]{2}/[0-9]{4} [0-9]{2}:[0-9]{2}) VN Số dư hiện tại: (0|$money) VND Mô tả: (.+) Số tài khoản: ([xX*]+[0-9]{4})")
    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT)

    fun parse(text: String): BidvParseResult {
        if (BidvNotificationParser.containsSensitiveText(text)) return BidvParseResult.Ignored
        val normalized = text.replace(Regex("\\s+"), " ").trim()
        val match = credit.matchEntire(normalized) ?: return BidvParseResult.Unknown
        val amount = match.groupValues[1].replace(".", "").toLongOrNull() ?: return BidvParseResult.Unknown
        if (match.groupValues[3].replace(".", "").toLongOrNull() == null) return BidvParseResult.Unknown
        val occurredAt = try {
            LocalDateTime.parse(match.groupValues[2], dateFormat).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            return BidvParseResult.Unknown
        }
        // No reference in the observed credit push; identical fields are treated as duplicate.
        val key = MessageDigest.getInstance("SHA-256").digest("$PACKAGE_NAME:$normalized".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return BidvParseResult.Parsed(amount, TransactionType.INCOME, occurredAt, key)
    }
}
