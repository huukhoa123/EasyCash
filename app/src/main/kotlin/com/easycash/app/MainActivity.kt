package com.easycash.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.easycash.ui.MainScreen
import com.easycash.ui.theme.EasyCashTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("easycash_settings", Context.MODE_PRIVATE)
        val initialDarkTheme = prefs.getBoolean("is_dark_theme", false) // Light is default per UI_REFRESH_LIGHT.md
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(initialDarkTheme) }
            DisposableEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { isDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { isDarkTheme },
                )
                onDispose {}
            }
            EasyCashTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeToggle = { enabled ->
                        isDarkTheme = enabled
                        prefs.edit().putBoolean("is_dark_theme", enabled).apply()
                    },
                )
            }
        }
    }
}
