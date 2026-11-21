package com.easycash.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyUtilsTest {
    @Test
    fun `formats standard vnd currency correctly`() {
        assertEquals("1.250.000 ₫", CurrencyUtils.formatVnd(1_250_000L))
        assertEquals("0 ₫", CurrencyUtils.formatVnd(0L))
        assertEquals("10.000 ₫", CurrencyUtils.formatVnd(10_000L))
    }

    @Test
    fun `formats vnd with positive and negative sign`() {
        assertEquals("+50.000 ₫", CurrencyUtils.formatVndWithSign(50_000L, isExpense = false))
        assertEquals("−50.000 ₫", CurrencyUtils.formatVndWithSign(50_000L, isExpense = true))
    }
}
