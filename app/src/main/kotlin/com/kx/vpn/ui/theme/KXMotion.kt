package com.kx.vpn.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import androidx.compose.runtime.Immutable

/**
 * Motion policy for the brand animation.
 *
 * The reference artwork implies continuous orbital movement. Continuous movement is
 * exactly what `prefers-reduced-motion` exists to stop, so animation is opt-out and
 * the opt-out is read from the real system setting, not a hard-coded flag.
 */
@Immutable
data class KXMotionSpec(
    /** Total brand intro before the launch gate may advance. */
    val introMillis: Int,
    /** Orbital node loop period. */
    val orbitPeriodMillis: Int,
    /** Halo breathing period. */
    val haloPeriodMillis: Int,
    val reduced: Boolean,
) {
    companion object {
        val Full = KXMotionSpec(
            introMillis = 900,
            orbitPeriodMillis = 18_000,
            haloPeriodMillis = 6_000,
            reduced = false,
        )

        /**
         * Reduced-motion fallback: nothing loops, nothing breathes, the composition
         * is still the full brand artwork, just static.
         */
        val Reduced = KXMotionSpec(
            introMillis = 0,
            orbitPeriodMillis = 0,
            haloPeriodMillis = 0,
            reduced = true,
        )
    }
}

/**
 * Pure decision function. `Settings.Global.ANIMATOR_DURATION_SCALE` of 0 means the
 * user turned system animation off; anything above 0 keeps the brand motion.
 * Extracted so it is unit-testable without an Android context.
 */
fun shouldAnimateBrand(animatorDurationScale: Float): Boolean =
    !animatorDurationScale.isNaN() && animatorDurationScale > 0f

@Composable
fun rememberKXMotionSpec(): KXMotionSpec {
    val context = LocalContext.current
    val scale = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }.getOrDefault(1f)
    return if (shouldAnimateBrand(scale)) KXMotionSpec.Full else KXMotionSpec.Reduced
}

val LocalKXMotion: ProvidableCompositionLocal<KXMotionSpec> =
    staticCompositionLocalOf { KXMotionSpec.Full }
