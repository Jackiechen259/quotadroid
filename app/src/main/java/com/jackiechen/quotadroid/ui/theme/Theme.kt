package com.jackiechen.quotadroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = GreenSecondary,
    tertiary = SandTertiary,
    background = LightSurface,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF002115),
    secondary = androidx.compose.ui.graphics.Color(0xFFB3CCBE),
    tertiary = androidx.compose.ui.graphics.Color(0xFFE7C08A),
    background = DarkSurface,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
)

@Composable
fun QuotaDroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
