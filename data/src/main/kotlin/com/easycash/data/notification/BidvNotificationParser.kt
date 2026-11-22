package com.easycash.data.notification

import com.easycash.domain.TransactionType
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

sealed interface BidvParseResult {
    data object Ignored : BidvParseResult
    data object Unknown : BidvParseResult
    data class Parsed(val amount: Long, val type: TransactionType, val occurredAtMillis: Long, val dedupKey: String) : BidvParseResult
}

object BidvNotificationParser {
    const val VERSION = 2
    const val PACKAGE_NAME = "com.vnpay.bidv"
    private val sensitive = Regex("otp|mã xác thực|mã xác minh|mã bảo mật|mật khẩu|password|verification|one.time", RegexOption.IGNORE_CASE)
    private val failed = Regex("thất bại|không thành công|khuyến mãi", RegexOption.IGNORE_CASE)
    private val money = Regex("([+-])([1-9][0-9]{0,2}(?:,[0-9]{3})*|[1-9][0-9]*) VND")
    private val dateFormat = DateTimeFormatter.ofPattern("HH:mm dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun containsSensitiveText(text: String) = sensitive.containsMatchIn(text)

    fun parse(text: String, zone: ZoneId): BidvParseResult {
        if (sensitive.containsMatchIn(text)) return BidvParseResult.Ignored
        if (failed.containsMatchIn(text)) return BidvParseResult.Ignored
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val legacy = lines.firstOrNull() == "BIDV xin thông báo tới Quý khách"
        if (!legacy && lines.firstOrNull()?.startsWith("Thời gian giao dịch:") != true) return BidvParseResult.Unknown
        fun field(label: String): String? {
            val matches = lines.filter { it.startsWith("$label:") }
            return matches.singleOrNull()?.substringAfter(':')?.trim()?.takeIf { it.isNotEmpty() }
        }
        val value = field("Số tiền GD")?.let { money.matchEntire(it) } ?: return BidvParseResult.Unknown
        val amount = value.groupValues[2].replace(",", "").toLongOrNull() ?: return BidvParseResult.Unknown
        val timestamp = field("Thời gian giao dịch") ?: return BidvParseResult.Unknown
        val occurredAt = try {
            LocalDateTime.parse(timestamp, dateFormat).atZone(zone).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            return BidvParseResult.Unknown
        }
        val reference = field("Mã giao dịch")
        val identity = if (reference != null) {
            if (!reference.matches(Regex("[A-Za-z0-9_-]{1,128}"))) return BidvParseResult.Unknown
            "$PACKAGE_NAME:$reference"
        } else {
            if (legacy || lines.any { it.startsWith("Mã giao dịch:") }) return BidvParseResult.Unknown
            val account = field("Tài khoản thanh toán") ?: return BidvParseResult.Unknown
            if (!account.matches(Regex("[0-9]{1,34}"))) return BidvParseResult.Unknown
            val balance = field("Số dư cuối") ?: return BidvParseResult.Unknown
            if (!balance.matches(Regex("-?(?:0|[1-9][0-9]{0,2}(?:,[0-9]{3})*|[1-9][0-9]*) VND"))) return BidvParseResult.Unknown
            val noteIndex = lines.indexOfFirst { it.startsWith("Nội dung giao dịch:") }
            if (noteIndex < 0) return BidvParseResult.Unknown
            val note = (listOf(lines[noteIndex].substringAfter(':').trim()) + lines.drop(noteIndex + 1))
                .joinToString(" ").replace(Regex("\\s+"), " ").trim()
            if (note.isEmpty()) return BidvParseResult.Unknown
            // No reference in observed push format; balance distinguishes same-minute payments.
            "$PACKAGE_NAME:push:$account:$timestamp:${value.value}:$balance:$note"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
        val key = digest.joinToString("") { "%02x".format(it.toInt() and 255) }
        return BidvParseResult.Parsed(amount,
            if (value.groupValues[1] == "-") TransactionType.EXPENSE else TransactionType.INCOME,
            occurredAt, key)
    }
}
