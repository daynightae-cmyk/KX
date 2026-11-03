package com.knoux.kx.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Violet = Color(0xFF6537E8)
internal val Cyan = Color(0xFF5BCFFD)
internal val Pearl = Color(0xFFE7EFFF)
internal val Midnight = Color(0xFF0B102C)

private val Day = lightColorScheme(
    primary = Violet, onPrimary = Color.White,
    background = Color(0xFFF3F0FF), onBackground = Color(0xFF171B4C),
    surface = Color(0xFFF9F7FF), onSurface = Color(0xFF171B4C),
    onSurfaceVariant = Color(0xFF4F5068), outline = Color(0xFF79758B),
)
private val Night = darkColorScheme(
    primary = Color(0xFFC5B4FF), onPrimary = Color(0xFF29145D),
    background = Midnight, onBackground = Pearl,
    surface = Color(0xFF171B3A), onSurface = Pearl,
    onSurfaceVariant = Color(0xFFC2C4DD), outline = Color(0xFF9692AF),
)

@Composable
fun KxTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Night else Day, content = content)
}
