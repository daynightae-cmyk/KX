package com.kx.vpn.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KXMotionTest {

    @Test
    fun positiveAnimatorScaleKeepsBrandMotion() {
        assertTrue(shouldAnimateBrand(1f))
        assertTrue(shouldAnimateBrand(0.5f))
        assertTrue(shouldAnimateBrand(5f))
    }

    @Test
    fun zeroAnimatorScaleIsReducedMotion() {
        assertFalse(shouldAnimateBrand(0f))
        // -0.0 compares equal to 0.0 in IEEE754; must not be treated as "animate".
        assertFalse(shouldAnimateBrand(-0.0f))
    }

    @Test
    fun negativeAnimatorScaleIsNotTreatedAsEnabled() {
        assertFalse(shouldAnimateBrand(-1f))
    }

    @Test
    fun nanIsTreatedAsUnknownAndThereforeReduced() {
        assertFalse(shouldAnimateBrand(Float.NaN))
    }

    @Test
    fun reducedSpecLoopsNothing() {
        val spec = KXMotionSpec.Reduced
        assertTrue(spec.reduced)
        assertEquals(0, spec.introMillis)
        assertEquals(0, spec.orbitPeriodMillis)
        assertEquals(0, spec.haloPeriodMillis)
    }

    @Test
    fun fullSpecHasBoundedNonZeroPeriods() {
        val spec = KXMotionSpec.Full
        assertFalse(spec.reduced)
        assertTrue("intro must be short enough not to stall launch", spec.introMillis in 1..2_000)
        assertTrue(spec.orbitPeriodMillis > spec.haloPeriodMillis)
    }
}
