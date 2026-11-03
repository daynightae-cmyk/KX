package com.kx.vpn.domain.tunnel

/** Backend byte-counter sample, not a public IP, handshake or speed-test result. */
data class TrafficCounterSample(val received: Long, val sent: Long, val elapsedMillis: Long)
data class TrafficRate(val receivedBytesPerSecond: Double, val sentBytesPerSecond: Double)

/** Null means insufficient/stale/reset data, distinct from a genuinely measured zero. */
fun deriveTrafficRate(previous: TrafficCounterSample?, current: TrafficCounterSample): TrafficRate? {
    if (previous == null || previous.elapsedMillis < 0 || current.elapsedMillis < 0) return null
    if (previous.received < 0 || previous.sent < 0 || current.received < previous.received ||
        current.sent < previous.sent) return null
    val interval = current.elapsedMillis - previous.elapsedMillis
    // Don't call a long background average the current throughput.
    if (interval !in 1..5_000L) return null
    return TrafficRate(
        (current.received - previous.received).toDouble() * 1_000.0 / interval,
        (current.sent - previous.sent).toDouble() * 1_000.0 / interval,
    )
}

fun formatTrafficRate(bytesPerSecond: Double): String {
    require(bytesPerSecond.isFinite() && bytesPerSecond >= 0) { "Invalid traffic rate" }
    return when {
        bytesPerSecond < 1024 -> "${bytesPerSecond.toLong()} B/s"
        bytesPerSecond < 1024 * 1024 -> "${(bytesPerSecond / 1024).toLong()} KiB/s"
        else -> "${(bytesPerSecond / (1024 * 1024)).toLong()} MiB/s"
    }
}
