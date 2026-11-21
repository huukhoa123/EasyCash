package com.easycash.data

import com.easycash.domain.Category
import com.easycash.domain.CategoryType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCategoryRepositoryTest {
    private val category = Category(0, "Ăn uống", CategoryType.EXPENSE, "food", "expense", false)

    @Test
    fun `reject blank category fields`() {
        assertTrue(RoomCategoryRepository.valid(category))
        assertFalse(RoomCategoryRepository.valid(category.copy(name = " \n")))
        assertFalse(RoomCategoryRepository.valid(category.copy(iconKey = "")))
        assertFalse(RoomCategoryRepository.valid(category.copy(colorKey = " ")))
    }
}
