package com.easycash.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {
    private val symbols = DecimalFormatSymbols(Locale.forLanguageTag("vi-VN")).apply {
        groupingSeparator = '.'
    }
    private val formatter = DecimalFormat("#,###", symbols)

    fun formatVnd(amount: Long): String {
        return "${formatter.format(amount)} ₫"
    }

    fun formatVndWithSign(amount: Long, isExpense: Boolean = false): String {
        val sign = if (isExpense) "−" else "+"
        return "$sign${formatter.format(kotlin.math.abs(amount))} ₫"
    }
}
