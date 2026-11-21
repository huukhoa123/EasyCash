package com.easycash.ui.navigation

sealed class NavDestination(val route: String, val title: String) {
    data object Home : NavDestination("home", "Tổng quan")
    data object Transactions : NavDestination("transactions", "Giao dịch")
    data object Reports : NavDestination("reports", "Báo cáo")
    data object Accounts : NavDestination("accounts", "Tài khoản")
    data object Settings : NavDestination("settings", "Cài đặt")

    companion object {
        val bottomNavItems = listOf(Home, Transactions, Reports, Accounts, Settings)
    }
}
