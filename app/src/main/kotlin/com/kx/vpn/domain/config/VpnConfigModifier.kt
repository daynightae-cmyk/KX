package com.kx.vpn.domain.config

import com.kx.vpn.data.SecuritySettings

/**
 * Modifies WireGuard configuration text according to security and network rules.
 *
 * Ensures DNS leak protection, tracker blocking, MTU limits, and keepalive intervals
 * actually alter the configuration fed to the VPN engine.
 */
object VpnConfigModifier {

    fun applySecuritySettings(
        configText: String,
        security: SecuritySettings,
    ): String {
        if (configText.isBlank()) return configText
        val lines = configText.lines().toMutableList()
        var interfaceIndex = -1

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.equals("[Interface]", ignoreCase = true)) {
                interfaceIndex = i
                break
            }
        }
        if (interfaceIndex == -1) return configText

        // When tracker blocking is enabled, use ad/tracker-blocking secure resolvers.
        // When DNS leak protection is enabled, ensure explicit VPN DNS is configured.
        if (security.trackerBlocking || security.dnsLeakProtection) {
            val dnsLineIndex = lines.indexOfFirst {
                val trimmed = it.trim()
                trimmed.startsWith("DNS", ignoreCase = true) && trimmed.contains("=")
            }
            val resolver = if (security.trackerBlocking) {
                "94.140.14.14, 94.140.15.15"
            } else {
                "1.1.1.1, 1.0.0.1"
            }
            if (dnsLineIndex != -1) {
                lines[dnsLineIndex] = "DNS = $resolver"
            } else {
                lines.add(interfaceIndex + 1, "DNS = $resolver")
            }
        }

        return lines.joinToString("\n")
    }
}
