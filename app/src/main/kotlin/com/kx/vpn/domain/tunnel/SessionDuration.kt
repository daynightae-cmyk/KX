package com.kx.vpn.domain.tunnel

/** Byte/latency counters are unrelated; this only formats measured backend-UP time. */
fun formatSessionDuration(elapsedMillis: Long): String {
    require(elapsedMillis >= 0) { "Session duration must not be negative" }
    val seconds = elapsedMillis / 1_000
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remaining = seconds % 60
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${remaining.toString().padStart(2, '0')}"
}
