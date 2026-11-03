package com.kx.vpn.domain.config

import com.kx.vpn.data.SecuritySettings
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnConfigModifierTest {

    private val sampleConfig = """
        [Interface]
        PrivateKey = aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        Address = 10.0.0.2/32

        [Peer]
        PublicKey = bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
        Endpoint = 198.51.100.1:51820
        AllowedIPs = 0.0.0.0/0
    """.trimIndent()

    @Test
    fun trackerBlockingInjectsPrivacyDns() {
        val modified = VpnConfigModifier.applySecuritySettings(
            sampleConfig,
            SecuritySettings(trackerBlocking = true),
        )
        assertTrue(modified.contains("DNS = 94.140.14.14, 94.140.15.15"))
    }

    @Test
    fun dnsLeakProtectionInjectsSecureDnsWhenTrackerBlockingOff() {
        val modified = VpnConfigModifier.applySecuritySettings(
            sampleConfig,
            SecuritySettings(trackerBlocking = false, dnsLeakProtection = true),
        )
        assertTrue(modified.contains("DNS = 1.1.1.1, 1.0.0.1"))
    }
}
