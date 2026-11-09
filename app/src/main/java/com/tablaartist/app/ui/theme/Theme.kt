package com.tablaartist.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = CopperAccent,
    onPrimary = Color(0xFF14110F),
    primaryContainer = SyahiSurfaceVariant,
    onPrimaryContainer = TextPrimary,
    secondary = SamGold,
    onSecondary = Color(0xFF14110F),
    background = SyahiBackground,
    onBackground = TextPrimary,
    surface = SyahiSurface,
    onSurface = TextPrimary,
    surfaceVariant = SyahiSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = SyahiBorder
)

@Composable
fun TablaArtistTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else darkColorScheme(
        primary = CopperAccent,
        onPrimary = Color(0xFF14110F),
        background = SyahiBackground,
        surface = SyahiSurface
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
