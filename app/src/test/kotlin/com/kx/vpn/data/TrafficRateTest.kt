package com.kx.vpn.data

import com.kx.vpn.domain.tunnel.*
import org.junit.Assert.*
import org.junit.Test

class TrafficRateTest {
    @Test fun firstSampleIsUnknownNotZero() {
        assertNull(deriveTrafficRate(null, TrafficCounterSample(100, 50, 1_000)))
    }
    @Test fun ratesUseMeasuredIntervalAndIndependentDirections() {
        assertEquals(TrafficRate(200.0, 40.0), deriveTrafficRate(
            TrafficCounterSample(100, 50, 1_000), TrafficCounterSample(500, 130, 3_000)))
    }
    @Test fun realUnchangedCountersMeanMeasuredZero() {
        assertEquals(TrafficRate(0.0, 0.0), deriveTrafficRate(
            TrafficCounterSample(100, 50, 1_000), TrafficCounterSample(100, 50, 2_000)))
    }
    @Test fun resetNegativeRepeatedBackwardAndStaleSamplesAreUnknown() {
        val before = TrafficCounterSample(100, 50, 1_000)
        listOf(TrafficCounterSample(99, 60, 2_000), TrafficCounterSample(110, 49, 2_000),
            TrafficCounterSample(110, 60, 1_000), TrafficCounterSample(110, 60, 999),
            TrafficCounterSample(110, 60, 6_001), TrafficCounterSample(-1, 60, 2_000)).forEach {
            assertNull(deriveTrafficRate(before, it))
        }
    }
    @Test fun largestCountersDoNotOverflowIntegerMultiplication() {
        val rate = deriveTrafficRate(TrafficCounterSample(0, 0, 0),
            TrafficCounterSample(Long.MAX_VALUE, Long.MAX_VALUE, 1))!!
        assertTrue(rate.receivedBytesPerSecond.isFinite())
        assertTrue(rate.receivedBytesPerSecond > 0)
    }
    @Test fun formattingUsesBinaryUnitsWithoutCallingItMegabitsOrSpeedTest() {
        assertEquals("0 B/s", formatTrafficRate(0.0))
        assertEquals("1 KiB/s", formatTrafficRate(1024.0))
        assertEquals("1 MiB/s", formatTrafficRate(1048576.0))
        for (value in listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            var rejected = false
            try { formatTrafficRate(value) } catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
        }
    }
}
