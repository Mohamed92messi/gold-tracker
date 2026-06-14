package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldSecondary,
    tertiary = GoldAccent,
    background = ObsidianBackground,
    surface = CardSurface,
    onPrimary = ObsidianBackground,
    onSecondary = GoldPrimary,
    onTertiary = GoldPrimary,
    onBackground = GoldAccent,
    onSurface = GoldAccent
)

private val LightColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldSecondary,
    tertiary = GoldAccent,
    background = ObsidianBackground,
    surface = CardSurface,
    onPrimary = ObsidianBackground,
    onSecondary = GoldPrimary,
    onTertiary = GoldPrimary,
    onBackground = GoldAccent,
    onSurface = GoldAccent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // We default to true for the rich premium look
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our bespoke gold theme branding strictly
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
