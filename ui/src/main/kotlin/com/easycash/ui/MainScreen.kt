package com.easycash.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.easycash.ui.components.AppIcon
import com.easycash.ui.components.Glyph
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.easycash.ui.accounts.AccountsScreen
import com.easycash.ui.accounts.AccountsViewModel
import com.easycash.ui.home.HomeScreen
import com.easycash.ui.home.HomeViewModel
import com.easycash.ui.navigation.NavDestination
import com.easycash.ui.reports.ReportsScreen
import com.easycash.ui.reports.ReportsViewModel
import com.easycash.ui.settings.SettingsScreen
import com.easycash.ui.settings.SettingsViewModel
import com.easycash.ui.theme.EasyCashTheme
import com.easycash.ui.transactions.TransactionsScreen
import com.easycash.ui.transactions.TransactionsViewModel

@Composable
fun MainScreen(
    isDarkTheme: Boolean = true,
    onDarkThemeToggle: (Boolean) -> Unit = {},
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                modifier = Modifier.height(88.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                NavDestination.bottomNavItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            AppIcon(
                                glyph = when (item) {
                                    NavDestination.Home -> Glyph.Home
                                    NavDestination.Transactions -> Glyph.Transactions
                                    NavDestination.Reports -> Glyph.Reports
                                    NavDestination.Accounts -> Glyph.Wallet
                                    NavDestination.Settings -> Glyph.Settings
                                },
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(NavDestination.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToTransactions = {
                        navController.navigate(NavDestination.Transactions.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAccounts = {
                        navController.navigate(NavDestination.Accounts.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(NavDestination.Settings.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(NavDestination.Transactions.route) {
                val txViewModel: TransactionsViewModel = hiltViewModel()
                TransactionsScreen(viewModel = txViewModel)
            }
            composable(NavDestination.Reports.route) {
                val reportsViewModel: ReportsViewModel = hiltViewModel()
                ReportsScreen(viewModel = reportsViewModel)
            }
            composable(NavDestination.Accounts.route) {
                val accountsViewModel: AccountsViewModel = hiltViewModel()
                AccountsScreen(viewModel = accountsViewModel)
            }
            composable(NavDestination.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeToggle = onDarkThemeToggle,
                    viewModel = settingsViewModel,
                )
            }
        }
    }
}

@Preview(name = "MainScreen Dark", showBackground = true)
@Composable
private fun MainScreenDarkPreview() {
    EasyCashTheme(darkTheme = true) {
        MainScreen(isDarkTheme = true)
    }
}

@Preview(name = "MainScreen Light", showBackground = true)
@Composable
private fun MainScreenLightPreview() {
    EasyCashTheme(darkTheme = false) {
        MainScreen(isDarkTheme = false)
    }
}
