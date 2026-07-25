package com.anodyne.app.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme {
    Light,
    Dark,
    OLED
}

enum class AppPrimaryColor(val displayName: String, val lightColor: Color, val darkColor: Color) {
    Purple("Purple", Color(0xFF6650a4), Color(0xFFD0BCFF)),
    Blue("Blue", Color(0xFF004AAD), Color(0xFF5DE0E6)),
    Teal("Teal", Color(0xFF00695C), Color(0xFF80CBC4)),
    Green("Green", Color(0xFF2E7D32), Color(0xFFA5D6A7)),
    Orange("Orange", Color(0xFFE65100), Color(0xFFFFCC80)),
    Pink("Pink", Color(0xFFC2185B), Color(0xFFF48FB1)),
    Red("Red", Color(0xFFC62828), Color(0xFFEF9A9A)),
    Indigo("Indigo", Color(0xFF283593), Color(0xFF9FA8DA)),
    Monochrome("Monochrome", Color(0xFF222222), Color(0xFFE0E0E0))
}

object ThemeState {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "app_theme"
    private const val KEY_PRIMARY_COLOR = "primary_color"
    
    private var _currentTheme by mutableStateOf(AppTheme.Dark)
    var currentTheme: AppTheme
        get() = _currentTheme
        set(value) {
            _currentTheme = value
        }
    
    private var _currentPrimaryColor by mutableStateOf(AppPrimaryColor.Blue)
    var currentPrimaryColor: AppPrimaryColor
        get() = _currentPrimaryColor
        set(value) {
            _currentPrimaryColor = value
        }
    
    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(KEY_THEME, AppTheme.Dark.name) ?: AppTheme.Dark.name
        _currentTheme = try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.Dark
        }
        val colorName = prefs.getString(KEY_PRIMARY_COLOR, AppPrimaryColor.Blue.name) ?: AppPrimaryColor.Blue.name
        _currentPrimaryColor = try {
            AppPrimaryColor.valueOf(colorName)
        } catch (e: IllegalArgumentException) {
            AppPrimaryColor.Blue
        }
    }
    
    fun setTheme(context: Context, theme: AppTheme) {
        _currentTheme = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }
    
    fun setPrimaryColor(context: Context, color: AppPrimaryColor) {
        _currentPrimaryColor = color
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PRIMARY_COLOR, color.name).apply()
    }
    
    fun getPrimaryColor(context: Context): AppPrimaryColor {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorName = prefs.getString(KEY_PRIMARY_COLOR, AppPrimaryColor.Blue.name) ?: AppPrimaryColor.Blue.name
        return try {
            AppPrimaryColor.valueOf(colorName)
        } catch (e: IllegalArgumentException) {
            AppPrimaryColor.Blue
        }
    }
    
    fun getTheme(context: Context): AppTheme {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(KEY_THEME, AppTheme.Dark.name) ?: AppTheme.Dark.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.Dark
        }
    }
    
    fun isDarkTheme(): Boolean {
        return currentTheme == AppTheme.Dark || currentTheme == AppTheme.OLED
    }
    
    fun isOLEDTheme(): Boolean {
        return currentTheme == AppTheme.OLED
    }
}

@Composable
fun rememberThemeState(): AppTheme {
    val context = LocalContext.current
    return remember {
        mutableStateOf(ThemeState.getTheme(context))
    }.value
}
