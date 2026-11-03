package com.kx.vpn.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Session duration and throughput, from monotonic readings only.
 *
 * The claims worth testing are the negative ones: that a rewound clock, a reset counter
 * or a single sample produce **no number** rather than a plausible-looking one.
 */
class SessionTelemetryTest {

    private fun sample(at: Long, rx: Long, tx: Long) = MonotonicSample(at, rx, tx)

    // -- Duration -------------------------------------------------------------------------

    @Test
    fun thereIsNoDurationBeforeTheTunnelIsUp() {
        assertNull(SessionTelemetry.elapsedMillis(null, 10_000L))
    }

    @Test
    fun durationComesFromTheMonotonicDifference() {
        assertEquals(
            45_000L,
            SessionTelemetry.elapsedMillis(startedAtElapsedRealtime = 10_000L, nowElapsedRealtime = 55_000L),
        )
    }

    /**
     * A monotonic source that goes backwards — which is what a reboot looks like to a
     * stored start instant — must not produce a session measured in negative time.
     */
    @Test
    fun aRewoundMonotonicSourceYieldsNoDurationRatherThanANegativeOne() {
        assertNull(SessionTelemetry.elapsedMillis(startedAtElapsedRealtime = 60_000L, nowElapsedRealtime = 10_000L))
    }

    @Test
    fun aZeroLengthSessionIsStillADuration() {
        assertEquals(0L, SessionTelemetry.elapsedMillis(5_000L, 5_000L))
    }

    // -- Throughput -----------------------------------------------------------------------

    @Test
    fun theFirstSampleEstablishesABaseAndReportsNoRate() {
        val (base, throughput) = SessionTelemetry.nextThroughput(previous = null, current = sample(1_000L, 500L, 200L))
        assertEquals(1_000L, base.elapsedRealtimeMillis)
        assertNull(throughput.downloadBytesPerSecond)
        assertNull(throughput.uploadBytesPerSecond)
        assertFalse(throughput.isMeasured)
        assertEquals(0L, throughput.windowMillis)
    }

    @Test
    fun twoSamplesGiveBytesPerSecondOverTheRealWindow() {
        val (_, throughput) = SessionTelemetry.nextThroughput(
            previous = sample(1_000L, 1_000L, 500L),
            current = sample(3_000L, 3_000L, 1_000L),
        )
        // 2000 bytes down and 500 up across a 2000 ms window: 1000 B/s and 250 B/s.
        assertEquals(1000.0, throughput.downloadBytesPerSecond!!, 1e-9)
        assertEquals(250.0, throughput.uploadBytesPerSecond!!, 1e-9)
        assertEquals(2_000L, throughput.windowMillis)
        assertTrue(throughput.isMeasured)
    }

    /**
     * Reconnecting resets the WireGuard statistics, so the counters drop while the clock
     * advances. Turning that into a negative speed would be worse than saying nothing.
     */
    @Test
    fun countersThatGoBackwardsProduceNoRate() {
        val (base, throughput) = SessionTelemetry.nextThroughput(
            previous = sample(5_000L, 10_000L, 4_000L),
            current = sample(7_000L, 200L, 50L),
        )
        assertNull(throughput.downloadBytesPerSecond)
        assertNull(throughput.uploadBytesPerSecond)
        assertFalse(throughput.isMeasured)
        // The new reading still becomes the base, so the next window can be measured.
        assertEquals(7_000L, base.elapsedRealtimeMillis)
        assertEquals(200L, base.rxBytes)
    }

    @Test
    fun aWindowThatDidNotAdvanceProduceNoRate() {
        val (_, throughput) = SessionTelemetry.nextThroughput(
            previous = sample(5_000L, 1_000L, 1_000L),
            current = sample(5_000L, 2_000L, 2_000L),
        )
        assertNull(throughput.downloadBytesPerSecond)
        assertEquals(0L, throughput.windowMillis)
    }

    @Test
    fun anIdleWindowIsARealMeasurementOfZero() {
        val (_, throughput) = SessionTelemetry.nextThroughput(
            previous = sample(1_000L, 9_000L, 9_000L),
            current = sample(3_000L, 9_000L, 9_000L),
        )
        // Zero bytes over a real window is a genuine reading, not an invalid one.
        assertEquals(0.0, throughput.downloadBytesPerSecond!!, 1e-9)
        assertTrue(throughput.isMeasured)
    }

    @Test
    fun aSampleRejectsImpossibleInputs() {
        var rejected = false
        try {
            MonotonicSample(elapsedRealtimeMillis = -1L, rxBytes = 0L, txBytes = 0L)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("a negative monotonic reading is not a sample", rejected)
    }

    // -- Formatting -----------------------------------------------------------------------

    @Test
    fun durationsFormatAsHoursMinutesSeconds() {
        assertEquals("00:00:00", SessionTelemetry.formatDuration(0L))
        assertEquals("00:42:18", SessionTelemetry.formatDuration((42 * 60 + 18) * 1_000L))
        assertEquals("01:00:00", SessionTelemetry.formatDuration(3_600_000L))
        assertEquals("100:00:00", SessionTelemetry.formatDuration(360_000_000L))
    }

    @Test
    fun anUnknownDurationIsMarkedNotMeasured() {
        assertEquals("--", SessionTelemetry.formatDuration(null))
    }

    @Test
    fun byteCountsUseBinaryUnitsAndNeverLieAboutThem() {
        assertEquals("512 B", SessionTelemetry.formatBytes(512L))
        assertEquals("1.0 KiB", SessionTelemetry.formatBytes(1024L))
        assertEquals("1.5 MiB", SessionTelemetry.formatBytes(1_572_864L))
        assertEquals("1.00 GiB", SessionTelemetry.formatBytes(1_073_741_824L))
        assertEquals("--", SessionTelemetry.formatBytes(-1L))
    }

    @Test
    fun ratesFormatInBitsAndRefuseToGuess() {
        assertEquals("--", SessionTelemetry.formatBitsPerSecond(null))
        // 1 byte/s is 8 bit/s; 1 MiB/s is 8.4 Mbit/s. The unit is bits, and it says so.
        assertEquals("8 bit/s", SessionTelemetry.formatBitsPerSecond(1.0))
        assertEquals("8.4 Mbit/s", SessionTelemetry.formatBitsPerSecond(1024.0 * 1024.0))
    }

    @Test
    fun aMeasuredThroughputAlwaysCarriesWhenItWasTaken() {
        val (_, throughput) = SessionTelemetry.nextThroughput(
            previous = sample(1_000L, 0L, 0L),
            current = sample(2_000L, 1_000L, 0L),
        )
        assertNotNull(throughput.measuredAtElapsedRealtime)
        assertEquals(2_000L, throughput.measuredAtElapsedRealtime)
    }
}
