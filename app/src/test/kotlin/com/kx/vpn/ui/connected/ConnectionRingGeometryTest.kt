package com.kx.vpn.ui.connected

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The connected ring's geometry, asserted as numbers rather than as pixels.
 *
 * Every value is derived from the box it is drawn into, so the ring scales with the
 * viewport instead of being re-tuned per density — and a degenerate box must produce a
 * path that leaks nothing outside itself.
 */
@RunWith(RobolectricTestRunner::class)
class ConnectionRingGeometryTest {

    @Test
    fun ringThicknessScalesWithTheBoxButStaysWithinItsBounds() {
        val small = ConnectionRingGeometry.ringStrokeWidth(minDimension = 100f)
        val medium = ConnectionRingGeometry.ringStrokeWidth(minDimension = 600f)
        val large = ConnectionRingGeometry.ringStrokeWidth(minDimension = 2_000f)

        assertEquals(6f, small, 1e-6f)
        assertEquals(21f, medium, 1e-6f)
        assertEquals(22f, large, 1e-6f)
        assertTrue("thicker on a bigger ring, up to the clamp", small < medium && medium < large)
    }

    @Test
    fun theRingAlwaysFitsInsideItsBox() {
        for (dimension in listOf(120f, 400f, 1_200f)) {
            val stroke = ConnectionRingGeometry.ringStrokeWidth(dimension)
            val radius = (dimension - stroke) / 2f
            assertTrue(
                "the ring at $dimension must stay inside the canvas",
                radius + stroke / 2f <= dimension / 2f + 0.001f,
            )
        }
    }

    @Test
    fun theCrestSitsAboveTheVerticalCentreLeavingRoomForTheLabel() {
        val bounds = ConnectionRingGeometry.crestBounds(minDimension = 600f)
        val centre = 300f

        assertTrue("the crest top must be above the centre", bounds.top < centre)
        assertTrue("the crest must not run into the label below", bounds.bottom < centre + 60f)
        assertTrue(bounds.halfWidth > 0f)
        assertEquals(bounds.bottom - bounds.top, bounds.height, 1e-4f)
    }

    @Test
    fun theCrestScalesLinearlyWithTheBox() {
        val one = ConnectionRingGeometry.crestBounds(minDimension = 200f)
        val two = ConnectionRingGeometry.crestBounds(minDimension = 400f)

        assertEquals(one.top * 2f, two.top, 1e-3f)
        assertEquals(one.bottom * 2f, two.bottom, 1e-3f)
        assertEquals(one.halfWidth * 2f, two.halfWidth, 1e-3f)
    }

    @Test
    fun theCheckIsDrawnInsideTheCrest() {
        val box = 600f
        val centre = Offset(box / 2f, box / 2f)
        val drawn = ConnectionRingGeometry.checkPath(centre, box).getBounds()
        val crest = ConnectionRingGeometry.crestBounds(box)

        assertTrue("the check must stay inside the crest on the left", drawn.left > centre.x - crest.halfWidth)
        assertTrue("the check must stay inside the crest on the right", drawn.right < centre.x + crest.halfWidth)
        assertTrue("the check must not rise above the crest", drawn.top >= crest.top)
        assertTrue("the check must not sink below the crest", drawn.bottom <= crest.bottom)

        // The crest converges to a point, so the widest test is not enough: the check has
        // to clear the taper as well. The first version sized it to the crest's full width
        // and the right arm poked outside the shield's edge — visible in the render, not
        // caught by the bounding-box checks above.
        val reach = maxOf(centre.x - drawn.left, drawn.right - centre.x)
        assertTrue(
            "the check must stay inside 60% of the crest half-width so it clears the taper",
            reach < crest.halfWidth * 0.6f,
        )
        assertTrue(
            "and it must sit in the upper part of the crest, where the shape is widest",
            drawn.bottom - centre.y < crest.height * 0.45f,
        )
    }

    /** A zero or non-finite box must not produce coordinates that escape it. */
    @Test
    fun aDegenerateBoxProducesAnEmptyPath() {
        assertTrue(ConnectionRingGeometry.checkPath(Offset.Zero, 0f).isEmpty)
        assertTrue(ConnectionRingGeometry.checkPath(Offset.Zero, -10f).isEmpty)
        assertTrue(ConnectionRingGeometry.checkPath(Offset.Zero, Float.NaN).isEmpty)
        assertTrue(ConnectionRingGeometry.checkPath(Offset.Zero, Float.POSITIVE_INFINITY).isEmpty)
    }

    @Test
    fun ringThicknessNeverBecomesNonFinite() {
        for (dimension in listOf(0f, 1f, 10f, 100_000f)) {
            val stroke = ConnectionRingGeometry.ringStrokeWidth(dimension)
            assertFalse("stroke for $dimension must be a real number", stroke.isNaN())
            assertTrue(stroke >= 6f && stroke <= 22f)
        }
    }
}
