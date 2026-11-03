package com.kx.vpn.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Brand type scale.
 *
 * FontFamily.SansSerif resolves to the platform sans, which carries both the Latin
 * and Arabic shaping fallbacks — required because TASK_00 ships EN and AR resources.
 * Sizes are generous and line heights loose so the 1.3x font-scale test does not
 * clip Arabic diacritics.
 */
object KXTypography {

    val Display = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.5.sp,
    )

    val Wordmark = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        // The reference sets K and X tight against each other; positive tracking
        // made the mark read as two separate letters.
        letterSpacing = (-0.5).sp,
    )

    val Title = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    )

    val Body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )

    val Caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    )

    val Button = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp,
    )
}

val KXMaterialTypography = Typography(
    displaySmall = KXTypography.Display,
    headlineSmall = KXTypography.Title,
    titleMedium = KXTypography.Title,
    bodyLarge = KXTypography.Body,
    bodyMedium = KXTypography.Body,
    labelLarge = KXTypography.Button,
    labelMedium = KXTypography.Caption,
)
