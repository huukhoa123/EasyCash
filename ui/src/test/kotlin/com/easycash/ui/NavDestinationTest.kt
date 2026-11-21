package com.easycash.ui

import com.easycash.ui.navigation.NavDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NavDestinationTest {
    @Test
    fun `bottom navigation items contains exactly five required tabs`() {
        val items = NavDestination.bottomNavItems
        assertEquals(5, items.size)
        assertEquals(
            listOf("home", "transactions", "reports", "accounts", "settings"),
            items.map { it.route },
        )
    }

    @Test
    fun `all bottom navigation items have non-empty titles`() {
        NavDestination.bottomNavItems.forEach { destination ->
            assertTrue(destination.title.isNotBlank())
        }
    }
}
