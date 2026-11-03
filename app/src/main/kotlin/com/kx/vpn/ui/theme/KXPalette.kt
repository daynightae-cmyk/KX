package com.kx.vpn.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand tokens for KX GLASS ORBIT.
 *
 * The values marked `sampled` were read directly off
 * `assets/reference/00-brand-logo.png` (1254x1254, SHA-256
 * c0b92dd84d60f6f0938bff177d7066a2f74a3eab59ba282438abb76da5f8681e)
 * with System.Drawing pixel probes. Where they disagree with the suggested
 * palette in `docs/DESIGN_SYSTEM.md`, the measured value wins and the delta is
 * recorded in `docs/evidence/TASK_00.md`.
 */
object KXPalette {

    // ---- Measured from the reference artwork -------------------------------------
    /** Orb interior lavender, probe (300,520). */
    val OrbLavender = Color(0xFFB6A5FC)

    /** Orb interior glow band, probe (627,700). */
    val OrbGlow = Color(0xFFD7D4FE)

    /** Orb upper rim highlight, probe (627,105). */
    val OrbRimCyan = Color(0xFFC2DAFD)

    /** Shield body violet, probe (520,330). Delta vs suggested #6537E8. */
    val ShieldViolet = Color(0xFF5932D3)

    /** Shield shadowed body, probe (640,600). */
    val ShieldDeep = Color(0xFF2F19BA)

    /** Iridescent ribbon fold, probe (560,420). */
    val RibbonViolet = Color(0xFF6C52FB)

    /** Lower flowing ribbon, probe (250,760). */
    val RibbonBlue = Color(0xFF98A9FB)

    /** Upper-left orbit node, probe (322,375). Suggested cyan was #5BCFFD. */
    val NodeBlue = Color(0xFF6FB3FC)

    /** Lower-left orbit node, probe (352,640). Suggested orchid was #BE69EF. */
    val NodeOrchid = Color(0xFFD169FC)

    /** Right orbit node, probe (1010,558). */
    val NodePearl = Color(0xFF7BA4FB)

    /** Darkest wordmark stroke, probe (400,860). */
    val WordmarkInk = Color(0xFF140A82)

    /** Artwork backdrop, probes (10,10) and (1240,1240). */
    val ArtBackdrop = Color(0xFF000000)

    // ---- Derived surface tokens --------------------------------------------------
    /** Night app surface. Suggested `midnight` in DESIGN_SYSTEM.md. */
    val Midnight = Color(0xFF0B102C)
    val Ink = Color(0xFF171B4C)
    val Violet = Color(0xFF6537E8)
    val Orchid = Color(0xFFBE69EF)
    val Cyan = Color(0xFF5BCFFD)
    val Pearl = Color(0xFFE7EFFF)

    /** Day app surface: pale lilac, per DESIGN_SYSTEM "pale lilac day". */
    val DaySurface = Color(0xFFF3F2FF)
    val DaySurfaceHigh = Color(0xFFFAFAFF)
    val DayScrim = Color(0xFFD9D4F7)

    /** Night text carried onto pale glass must never be pale glass itself. */
    val NightTextPrimary = Color(0xFFF2F1FF)
    val NightTextSecondary = Color(0xFFB9B6E4)
    val DayTextPrimary = Color(0xFF141338)
    val DayTextSecondary = Color(0xFF4C4886)

    /**
     * Status colour used by the launch gate only. TASK_00 has no tunnel engine, so
     * there is deliberately no "connected / protected" colour anywhere in this file.
     */
    val NeutralHint = Color(0xFF8E8AC0)
}
