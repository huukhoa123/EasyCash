package com.easycash.ui.util

import com.easycash.domain.Category
import com.easycash.domain.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryMatcherTest {

    private val categories = listOf(
        Category(1L, "Ăn uống", CategoryType.EXPENSE, "food", "orange", true),
        Category(2L, "Di chuyển", CategoryType.EXPENSE, "transit", "blue", true),
        Category(3L, "Mua sắm", CategoryType.EXPENSE, "shopping", "purple", true),
        Category(4L, "Hoá đơn", CategoryType.EXPENSE, "bill", "red", true),
        Category(5L, "Sức khoẻ", CategoryType.EXPENSE, "health", "green", true),
        Category(6L, "Giải trí", CategoryType.EXPENSE, "entertainment", "cyan", true),
        Category(7L, "Lương", CategoryType.INCOME, "salary", "emerald", true),
    )

    @Test
    fun `suggestCategory matches direct category name`() {
        val match = CategoryMatcher.suggestCategory("Đi mua sắm cuối tuần", categories)
        assertEquals(3L, match?.id)
        assertEquals("Mua sắm", match?.name)
    }

    @Test
    fun `suggestCategory matches food and beverage keywords`() {
        val match1 = CategoryMatcher.suggestCategory("Cà phê Highlands sáng", categories)
        assertEquals("Ăn uống", match1?.name)

        val match2 = CategoryMatcher.suggestCategory("Cơm tấm sườn bì chả", categories)
        assertEquals("Ăn uống", match2?.name)

        val match3 = CategoryMatcher.suggestCategory("Trà sữa Phúc Long", categories)
        assertEquals("Ăn uống", match3?.name)
    }

    @Test
    fun `suggestCategory matches transit keywords`() {
        val match1 = CategoryMatcher.suggestCategory("Đổ xăng xe máy", categories)
        assertEquals("Di chuyển", match1?.name)

        val match2 = CategoryMatcher.suggestCategory("Đi Grab về nhà", categories)
        assertEquals("Di chuyển", match2?.name)

        val match3 = CategoryMatcher.suggestCategory("Vé máy bay Vietjet", categories)
        assertEquals("Di chuyển", match3?.name)
    }

    @Test
    fun `suggestCategory matches bill and shopping keywords`() {
        val match1 = CategoryMatcher.suggestCategory("Thanh toán tiền điện EVN", categories)
        assertEquals("Hoá đơn", match1?.name)

        val match2 = CategoryMatcher.suggestCategory("Mua đồ Shopee", categories)
        assertEquals("Mua sắm", match2?.name)
    }

    @Test
    fun `suggestCategory matches salary keywords`() {
        val match = CategoryMatcher.suggestCategory("Tạm ứng lương tháng này", categories)
        assertEquals("Lương", match?.name)
    }

    @Test
    fun `suggestCategory returns null when no match or empty`() {
        assertNull(CategoryMatcher.suggestCategory("", categories))
        assertNull(CategoryMatcher.suggestCategory("    ", categories))
        assertNull(CategoryMatcher.suggestCategory("Giao dịch abcxyz123", categories))
        assertNull(CategoryMatcher.suggestCategory("Ăn uống", emptyList()))
    }
}
