package com.kx.vpn.domain.session

import java.util.Locale

/**
 * Session duration and throughput, computed only from monotonic readings.
 *
 * Nothing in this file reads a clock. The caller supplies readings taken from a
 * monotonic source, which is what makes the boundary cases testable and what keeps a
 * wall-clock change — daylight saving, an NTP correction, the user editing the date —
 * from moving a session timer or producing a negative speed.
 */

/** One observation of the tunnel, all fields from the same monotonic instant. */
data class MonotonicSample(
    val elapsedRealtimeMillis: Long,
    val rxBytes: Long,
    val txBytes: Long,
) {
    init {
        require(elapsedRealtimeMillis >= 0L) { "a monotonic reading cannot be negative" }
        require(rxBytes >= 0L && txBytes >= 0L) { "byte counters cannot be negative" }
    }
}

/**
 * Observed throughput over one sampling window.
 *
 * A null rate means "this window cannot be trusted", not zero: the counters went
 * backwards, the reading did not advance, or the tunnel restarted underneath the
 * sample. Reporting zero would tell the user their connection is idle when the real
 * answer is that the measurement was invalid.
 */
data class Throughput(
    val downloadBytesPerSecond: Double?,
    val uploadBytesPerSecond: Double?,
    val windowMillis: Long,
    val measuredAtElapsedRealtime: Long,
) {
    val isMeasured: Boolean get() = downloadBytesPerSecond != null || uploadBytesPerSecond != null
}

object SessionTelemetry {

    /**
     * How long the session has been up, in milliseconds.
     *
     * Returns null before the tunnel is up and never returns a negative value, so a
     * monotonic source that is rewound by a reboot shows "no duration" rather than a
     * session that started in the future.
     */
    fun elapsedMillis(startedAtElapsedRealtime: Long?, nowElapsedRealtime: Long): Long? {
        val started = startedAtElapsedRealtime ?: return null
        val elapsed = nowElapsedRealtime - started
        return if (elapsed < 0L) null else elapsed
    }

    /**
     * The throughput between two samples, and the sample to keep for the next window.
     *
     * The returned [Throughput] is always the *previous* window's measurement, because a
     * rate needs two points. The new sample is returned so the caller can store it as the
     * base for the next window without recomputing anything.
     */
    fun nextThroughput(previous: MonotonicSample?, current: MonotonicSample): Pair<MonotonicSample, Throughput> {
        if (previous == null) {
            return current to Throughput(
                downloadBytesPerSecond = null,
                uploadBytesPerSecond = null,
                windowMillis = 0L,
                measuredAtElapsedRealtime = current.elapsedRealtimeMillis,
            )
        }
        val windowMillis = current.elapsedRealtimeMillis - previous.elapsedRealtimeMillis
        val rxDelta = current.rxBytes - previous.rxBytes
        val txDelta = current.txBytes - previous.txBytes

        // A window that did not advance, or counters that went backwards, is an invalid
        // measurement. The WireGuard statistics are per-tunnel, so a reconnect resets them
        // to a smaller number — that is exactly the case this refuses to turn into a rate.
        if (windowMillis <= 0L || rxDelta < 0L || txDelta < 0L) {
            return current to Throughput(
                downloadBytesPerSecond = null,
                uploadBytesPerSecond = null,
                windowMillis = if (windowMillis > 0L) windowMillis else 0L,
                measuredAtElapsedRealtime = current.elapsedRealtimeMillis,
            )
        }

        val seconds = windowMillis.toDouble() / 1000.0
        return current to Throughput(
            downloadBytesPerSecond = rxDelta / seconds,
            uploadBytesPerSecond = txDelta / seconds,
            windowMillis = windowMillis,
            measuredAtElapsedRealtime = current.elapsedRealtimeMillis,
        )
    }

    /**
     * Formats a byte count for display.
     *
     * Deliberately binary units (KiB/MiB/GiB) with the long form spelled out, because a
     * "MB" that means 1,048,576 bytes is a small untruth repeated often.
     *
     * `Locale.US` is pinned rather than inherited: these are technical readouts that sit
     * next to ASCII digits and unit abbreviations, and letting the device locale choose the
     * decimal separator would make "1,0 KiB" appear inside an English label and make the
     * formatting untestable without a locale rule.
     */
    fun formatBytes(bytes: Long): String = when {
        bytes < 0L -> "--"
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KiB", bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L ->
            String.format(Locale.US, "%.1f MiB", bytes / (1024.0 * 1024.0))

        else -> String.format(Locale.US, "%.2f GiB", bytes / (1024.0 * 1024.0 * 1024.0))
    }

    /**
     * Formats a rate as bits per second, which is the unit the reference's "Mbps" tile
     * means. Only ever called with a rate that actually came from two real samples.
     */
    fun formatBitsPerSecond(bytesPerSecond: Double?): String {
        val bits = bytesPerSecond?.times(8.0) ?: return "--"
        return when {
            bits < 1_000.0 -> String.format(Locale.US, "%.0f bit/s", bits)
            bits < 1_000_000.0 -> String.format(Locale.US, "%.1f kbit/s", bits / 1_000.0)
            bits < 1_000_000_000.0 -> String.format(Locale.US, "%.1f Mbit/s", bits / 1_000_000.0)
            else -> String.format(Locale.US, "%.2f Gbit/s", bits / 1_000_000_000.0)
        }
    }

    /**
     * The same rate split into the number and its unit.
     *
     * A three-across tile at 360dp cannot fit "11.5 Mbit/s" on one line at display size,
     * and clipping it to "11.5" drops the unit — which is the part that makes the number
     * mean something. Showing the unit at caption size beside a larger figure keeps both.
     */
    fun rateParts(bytesPerSecond: Double?): Pair<String, String> {
        val bits = bytesPerSecond?.times(8.0) ?: return "--" to ""
        return when {
            bits < 1_000.0 -> String.format(Locale.US, "%.0f", bits) to "bps"
            bits < 1_000_000.0 -> String.format(Locale.US, "%.1f", bits / 1_000.0) to "kbps"
            bits < 1_000_000_000.0 -> String.format(Locale.US, "%.1f", bits / 1_000_000.0) to "Mbps"
            else -> String.format(Locale.US, "%.2f", bits / 1_000_000_000.0) to "Gbps"
        }
    }

    /**
     * Formats an elapsed duration as HH:MM:SS.
     *
     * Null renders as the same `--` marker the rest of the app uses for "not measured",
     * so a session that has not started cannot look like a session of zero seconds.
     */
    fun formatDuration(millis: Long?): String {
        if (millis == null) return "--"
        val totalSeconds = millis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
