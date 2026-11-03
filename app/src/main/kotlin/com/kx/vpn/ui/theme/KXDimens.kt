package com.kx.vpn.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing, sizing and shape scale. Everything the later screens need is expressed
 * here once so TASK_01..TASK_10 do not invent their own magic numbers.
 */
object KXDimens {

    /** Contract minimum touch target (docs/ENGINEERING_CONTRACT.md "UX contract"). */
    val MinTouchTarget: Dp = 48.dp

    val SpaceXxs: Dp = 2.dp
    val SpaceXs: Dp = 4.dp
    val SpaceSm: Dp = 8.dp
    val SpaceMd: Dp = 16.dp
    val SpaceLg: Dp = 24.dp
    val SpaceXl: Dp = 32.dp
    val SpaceXxl: Dp = 48.dp

    /**
     * Measured off the reference artwork, not chosen: `tools/measure_reference.py` puts the
     * card edge at 16.6dp from the left and 16.2dp from the right on
     * `04-connected-session.png`, and 14–18dp on `02-home-disconnected.png`. It was 24dp,
     * which made every card on every screen narrower than the design it is meant to match.
     */
    val ScreenPadding: Dp = 16.dp

    /** Brand emblem sizes. Splash uses [OrbHero]; list rows use [OrbInline]. */
    val OrbHero: Dp = 232.dp
    val OrbLarge: Dp = 168.dp
    val OrbMedium: Dp = 96.dp
    val OrbInline: Dp = 40.dp

    val WordmarkHero: Dp = 44.dp

    val CardCorner: Dp = 28.dp
    val CardInnerCorner: Dp = 20.dp
    val ButtonCorner: Dp = 999.dp

    val GlassStrokeWidth: Dp = 1.25.dp
    val RimStrokeWidth: Dp = 2.dp

    val IconActionSize: Dp = 48.dp
}
