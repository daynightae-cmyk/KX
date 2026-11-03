package com.kx.vpn.ui.brand

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs

/**
 * Geometry invariants for the brand mark.
 *
 * These are the checks that catch the two failure modes that actually happen when an
 * artwork is translated into canvas code: coordinates that escape the drawing bounds,
 * and a mark that silently changes shape when the orb is rendered smaller.
 */
@RunWith(RobolectricTestRunner::class)
class KXOrbGeometryTest {

    private val canvasSize = 1000f
    private val center = Offset(500f, 500f)
    private val radius = 440f

    @Test
    fun circlePathIsCentredAndTouchesAllFourExtremes() {
        val bounds = circlePath(center, radius).getBounds()
        assertEquals(60f, bounds.left, 0.5f)
        assertEquals(60f, bounds.top, 0.5f)
        assertEquals(940f, bounds.right, 0.5f)
        assertEquals(940f, bounds.bottom, 0.5f)
    }

    @Test
    fun crestStaysInsideTheOrbAndPointsDownward() {
        val halfWidth = radius * 0.40f
        val top = center.y + radius * -0.72f
        val bottom = center.y + radius * 0.16f
        val bounds = crestPath(center, halfWidth, top, bottom).getBounds()

        assertTrue("shield must not exceed the orb horizontally", bounds.left > 60f)
        assertTrue("shield must not exceed the orb horizontally", bounds.right < 940f)
        assertTrue("shield tip must sit below its shoulders", bounds.bottom > center.y)
        assertTrue("shield must not run into the wordmark band", bounds.bottom < 800f)

        // The crest's first control handle sits above the anchor (that is what gives
        // the shoulders their round), so the bounding box overshoots `top` — but never
        // as far as the handle itself. Asserting equality here would encode a
        // misunderstanding of Bezier hulls, so the bound is asserted instead.
        assertTrue(
            "bbox top ${bounds.top} must lie between the handle overshoot and the anchor",
            bounds.top in (top - halfWidth * 0.10f)..top,
        )
    }

    @Test
    fun ribbonFoldsAreMirrorImagesOfEachOther() {
        val halfWidth = radius * 0.40f
        val top = center.y - radius * 0.72f
        val bottom = center.y + radius * 0.16f

        val forward = ribbonPath(center, halfWidth, top, bottom, leftToRight = true).getBounds()
        val back = ribbonPath(center, halfWidth, top, bottom, leftToRight = false).getBounds()

        // Mirror about the vertical axis: left/right swap, top/bottom match.
        val mirroredLeft = canvasSize - back.right
        val mirroredRight = canvasSize - back.left
        assertEquals(mirroredLeft, forward.left, 1f)
        assertEquals(mirroredRight, forward.right, 1f)
        assertEquals(back.top, forward.top, 1f)
        assertEquals(back.bottom, forward.bottom, 1f)
    }

    @Test
    fun fourPointStarIsSymmetricAboutItsCenter() {
        val star = fourPointStar(center, 80f, 20f).getBounds()
        assertEquals(420f, star.left, 0.5f)
        assertEquals(580f, star.right, 0.5f)
        assertEquals(420f, star.top, 0.5f)
        assertEquals(580f, star.bottom, 0.5f)
    }

    @Test
    fun ellipticalArcHalfDependsOnTheTopFlag() {
        val upper = ellipticalArcPath(center, 300f, 100f, top = true).getBounds()
        val lower = ellipticalArcPath(center, 300f, 100f, top = false).getBounds()
        assertTrue("upper half must sit above the centre", upper.bottom <= center.y + 1f)
        assertTrue("lower half must sit below the centre", lower.top >= center.y - 1f)
    }

    /**
     * Size invariance: scaling the canvas must scale the geometry, not reshape it.
     * This is what makes one composable valid at 232dp on the splash and 40dp inline.
     */
    @Test
    fun geometryScalesLinearlyWithTheCanvas() {
        val small = crestPath(Offset(100f, 100f), 40f, 60f, 120f).getBounds()
        val large = crestPath(Offset(200f, 200f), 80f, 120f, 240f).getBounds()

        val scale = 2f
        assertEquals(small.width * scale, large.width, 0.5f)
        assertEquals(small.height * scale, large.height, 0.5f)
    }

    @Test
    fun noPathProducesNonFiniteCoordinates() {
        val halfWidth = radius * 0.40f
        val paths = listOf(
            circlePath(center, radius),
            crestPath(center, halfWidth, center.y - radius, center.y + radius * 0.2f),
            ribbonPath(center, halfWidth, center.y - radius, center.y + radius * 0.2f, true),
            ellipticalArcPath(center, radius, radius * 0.3f, top = true),
            fourPointStar(center, 50f, 12f),
        )
        paths.forEach { path ->
            val bounds = path.getBounds()
            listOf(bounds.left, bounds.top, bounds.right, bounds.bottom).forEach { value ->
                assertTrue("path bounds must be finite", !value.isNaN() && !abs(value).isInfinite())
            }
        }
    }
}
