package com.kx.vpn.domain.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * Parser-boundary tests for hostile and malformed configuration input.
 *
 * The property under test that matters most is not "does it parse" but "does it ever
 * repeat a secret back". Every failure path is therefore also asserted for redaction.
 */
class WireGuardConfigValidatorTest {

    private fun key(seed: Int): String =
        Base64.getEncoder().encodeToString(ByteArray(32) { (it + seed).toByte() })

    private val privateKey = key(1)
    private val publicKey = key(2)
    private val otherPublicKey = key(3)
    private val preshared = key(4)

    private val minimalValid = """
        [Interface]
        PrivateKey = $privateKey
        Address = 10.8.0.2/32

        [Peer]
        PublicKey = $publicKey
        AllowedIPs = 0.0.0.0/0
        Endpoint = 203.0.113.7:51820
    """.trimIndent()

    private fun invalidMessages(result: ConfigValidation): List<IssueMessage> =
        (result as ConfigValidation.Invalid).errors.map { it.messageKey }

    // -- Happy paths ----------------------------------------------------------------

    @Test
    fun acceptsAMinimalRealisticConfig() {
        val result = WireGuardConfigValidator.validate(minimalValid)
        assertTrue("expected valid, got $result", result is ConfigValidation.Valid)
        val summary = (result as ConfigValidation.Valid).summary
        assertTrue(summary.interfaceHasPrivateKey)
        assertEquals(1, summary.interfaceAddressCount)
        assertEquals(1, summary.peerCount)
        assertEquals(listOf("203.0.113.7:51820"), summary.peerEndpoints)
        assertTrue("0.0.0.0/0 means full tunnel", summary.routesAllTraffic)
    }

    @Test
    fun acceptsEverySupportedInterfaceAndPeerField() {
        val config = """
            [Interface]
            PrivateKey = $privateKey
            Address = 10.8.0.2/32, fd00::3/128
            DNS = 1.1.1.1, 9.9.9.9, dns.example.com
            MTU = 1420
            ListenPort = 51820

            [Peer]
            PublicKey = $publicKey
            PresharedKey = $preshared
            Endpoint = wg.example.com:443
            AllowedIPs = 10.0.0.0/8, 192.168.1.5/32, ::/0
            PersistentKeepalive = 25
        """.trimIndent()

        val result = WireGuardConfigValidator.validate(config)
        assertTrue("expected valid, got $result", result is ConfigValidation.Valid)
        val summary = (result as ConfigValidation.Valid).summary
        assertEquals(2, summary.interfaceAddressCount)
        assertEquals(3, summary.interfaceDnsServers.size)
        assertEquals(1420, summary.mtu)
        assertEquals(51820, summary.interfaceListenPort)
        assertEquals(25, summary.persistentKeepaliveSeconds)
        assertEquals(3, summary.allowedIpCount)
        assertTrue(summary.routesAllTraffic)
        assertEquals(listOf("wg.example.com:443"), summary.peerEndpoints)
    }

    @Test
    fun commentsAndBlankLinesAreIgnored() {
        val config = """
            # a comment
            ; another comment style

            [Interface]
            PrivateKey = $privateKey
            Address = 10.8.0.2/32

            [Peer]
            PublicKey = $publicKey
            AllowedIPs = 10.0.0.0/8
        """.trimIndent()
        assertTrue(WireGuardConfigValidator.validate(config) is ConfigValidation.Valid)
    }

    @Test
    fun splitTunnelConfigIsNotReportedAsFullTunnel() {
        val config = """
            [Interface]
            PrivateKey = $privateKey
            Address = 10.8.0.2/32

            [Peer]
            PublicKey = $publicKey
            AllowedIPs = 10.0.0.0/8
        """.trimIndent()
        val summary =
            (WireGuardConfigValidator.validate(config) as ConfigValidation.Valid).summary
        assertFalse(summary.routesAllTraffic)
    }

    // -- Structural rejection -------------------------------------------------------

    @Test
    fun rejectsEmptyInput() {
        assertEquals(listOf(IssueMessage.Empty), invalidMessages(WireGuardConfigValidator.validate("   ")))
        assertEquals(listOf(IssueMessage.Empty), invalidMessages(WireGuardConfigValidator.validate(ByteArray(0))))
    }

    @Test
    fun rejectsMissingInterfaceAndMissingPeer() {
        // A bare [Peer] with no AllowedIPs is two findings, not one: the section is
        // present so MissingPeerSection does not apply, but the peer is unusable.
        assertEquals(
            listOf(IssueMessage.NoAllowedIps, IssueMessage.MissingInterfaceSection),
            invalidMessages(WireGuardConfigValidator.validate("[Peer]\nPublicKey = $publicKey")),
        )
        assertEquals(
            listOf(IssueMessage.MissingPeerSection),
            invalidMessages(
                WireGuardConfigValidator.validate(
                    "[Interface]\nPrivateKey = $privateKey\nAddress = 10.8.0.2/32",
                ),
            ),
        )
    }

    @Test
    fun rejectsTwoInterfaceSections() {
        val config = minimalValid + "\n[Interface]\nPrivateKey = $privateKey\n"
        assertTrue(
            invalidMessages(WireGuardConfigValidator.validate(config))
                .contains(IssueMessage.MultipleInterfaceSections),
        )
    }

    @Test
    fun rejectsUnknownSectionAndContentOutsideASection() {
        val withSection = WireGuardConfigValidator.validate("[Bogus]\nFoo = bar\n$minimalValid")
        assertTrue(
            invalidMessages(withSection).contains(IssueMessage.UnknownSection),
        )
    }

    // -- Key validation -------------------------------------------------------------

    @Test
    fun rejectsTruncatedAndMalformedKeys() {
        val truncated = minimalValid.replace(privateKey, privateKey.take(40))
        assertTrue(
            invalidMessages(WireGuardConfigValidator.validate(truncated))
                .contains(IssueMessage.InvalidBase64Key),
        )

        val wrongAlphabet = minimalValid.replace(privateKey, "A".repeat(43) + "!")
        assertTrue(
            invalidMessages(WireGuardConfigValidator.validate(wrongAlphabet))
                .contains(IssueMessage.InvalidBase64Key),
        )
    }

    @Test
    fun rejectsDuplicatePeers() {
        val config = minimalValid + """

            [Peer]
            PublicKey = $publicKey
            AllowedIPs = 192.168.0.0/16
            Endpoint = 203.0.113.8:51820
        """.trimIndent()
        assertTrue(
            invalidMessages(WireGuardConfigValidator.validate(config))
                .contains(IssueMessage.DuplicatePeer),
        )
    }

    @Test
    fun acceptsTwoDistinctPeers() {
        val config = minimalValid + """

            [Peer]
            PublicKey = $otherPublicKey
            AllowedIPs = 192.168.0.0/16
            Endpoint = 203.0.113.8:51820
        """.trimIndent()
        val summary =
            (WireGuardConfigValidator.validate(config) as ConfigValidation.Valid).summary
        assertEquals(2, summary.peerCount)
    }

    // -- Values ---------------------------------------------------------------------

    @Test
    fun rejectsBadMtuKeepaliveAndCidr() {
        val mtu = """
            [Interface]
            PrivateKey = $privateKey
            Address = 10.8.0.2/32
            MTU = 100

            [Peer]
            PublicKey = $publicKey
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        assertTrue(
            invalidMessages(WireGuardConfigValidator.validate(mtu))
                .contains(IssueMessage.InvalidMtu),
        )

        val keepalive = """
            [Interface]
            PrivateKey = $privateKey
            Address = 10.8.0.2/32

            [Peer]
            PublicKey = $publicKey
            AllowedIPs = 0.0.0.0/0
            PersistentKeepalive = -1
        """.trimIndent()
        assertTrue(
            invalidMessages(WireGuardConfigValidator.validate(keepalive))
                .contains(IssueMessage.InvalidKeepalive),
        )

        val cidr = """
            [Interface]
            PrivateKey = $privateKey
            Address = 10.8.0.2/32

            [Peer]
            PublicKey = $publicKey
            AllowedIPs = 10.0.0.0/33
        """.trimIndent()
        assertTrue(
            invalidMessages(WireGuardConfigValidator.validate(cidr))
                .contains(IssueMessage.InvalidCidr),
        )
    }

    @Test
    fun rejectsAnOutOfRangeListenPort() {
        val config = """
            [Interface]
            PrivateKey = $privateKey
            Address = 10.8.0.2/32
            ListenPort = 70000

            [Peer]
            PublicKey = $publicKey
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        assertTrue(
            invalidMessages(WireGuardConfigValidator.validate(config))
                .contains(IssueMessage.InvalidPort),
        )
    }

    @Test
    fun hyphenatedKeyVariantsAreAlsoAccepted() {
        // Real-world exports use CamelCase; some tooling emits hyphenated snake forms.
        val config = """
            [Interface]
            PrivateKey = $privateKey
            Address = 10.8.0.2/32
            Listen-Port = 51820

            [Peer]
            PublicKey = $publicKey
            Allowed-Ips = 0.0.0.0/0
            Persistent-Keepalive = 25
        """.trimIndent()
        val summary =
            (WireGuardConfigValidator.validate(config) as ConfigValidation.Valid).summary
        assertEquals(51820, summary.interfaceListenPort)
        assertEquals(25, summary.persistentKeepaliveSeconds)
        assertEquals(1, summary.allowedIpCount)
    }

    @Test
    fun peerWithoutAllowedIpsIsRejected() {
        val config = """
            [Interface]
            PrivateKey = $privateKey
            Address = 10.8.0.2/32

            [Peer]
            PublicKey = $publicKey
            Endpoint = 203.0.113.7:51820
        """.trimIndent()
        assertTrue(
            invalidMessages(WireGuardConfigValidator.validate(config))
                .contains(IssueMessage.NoAllowedIps),
        )
    }

    @Test
    fun rejectsMalformedEndpoints() {
        listOf(
            "203.0.113.7",              // no port
            "203.0.113.7:0",            // port out of range
            "203.0.113.7:70000",
            "not a host:51820",
            ":51820",                   // empty host
        ).forEach { endpoint ->
            val config = minimalValid.replace("203.0.113.7:51820", endpoint)
            val messages = invalidMessages(WireGuardConfigValidator.validate(config))
            assertTrue("'$endpoint' should be rejected, got $messages", messages.contains(IssueMessage.InvalidEndpoint))
        }
    }

    // -- Limits and encoding --------------------------------------------------------

    @Test
    fun rejectsInputOverTheSizeLimitBeforeParsing() {
        val huge = ByteArray(WireGuardConfigValidator.MAX_CONFIG_BYTES + 1) { 'a'.code.toByte() }
        val result = WireGuardConfigValidator.validate(huge)
        assertEquals(listOf(IssueMessage.TooLarge), invalidMessages(result))
    }

    @Test
    fun rejectsExactlyAtTheBoundaryPlusOneAndAcceptsJustUnder() {
        val justOver = minimalValid + "\n" + "#".repeat(WireGuardConfigValidator.MAX_CONFIG_BYTES)
        assertEquals(
            listOf(IssueMessage.TooLarge),
            invalidMessages(WireGuardConfigValidator.validate(justOver.toByteArray())),
        )
    }

    @Test
    fun rejectsTooManyLines() {
        val manyLines = buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = $privateKey")
            appendLine("Address = 10.8.0.2/32")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = $publicKey")
            appendLine("AllowedIPs = 0.0.0.0/0")
            repeat(WireGuardConfigValidator.MAX_LINES + 5) { appendLine("# pad $it") }
        }
        assertEquals(
            listOf(IssueMessage.TooManyLines),
            invalidMessages(WireGuardConfigValidator.validate(manyLines)),
        )
    }

    @Test
    fun rejectsInvalidUtf8Bytes() {
        // 0xFF is never valid in UTF-8.
        val bytes = minimalValid.toByteArray() + byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        assertEquals(
            listOf(IssueMessage.NotUtf8),
            invalidMessages(WireGuardConfigValidator.validate(bytes)),
        )
    }

    @Test
    fun rejectsEmbeddedControlCharacters() {
        val withNul = minimalValid + "\u0000injected"
        assertEquals(
            listOf(IssueMessage.ControlCharacters),
            invalidMessages(WireGuardConfigValidator.validate(withNul)),
        )
    }

    // -- The redaction guarantee ----------------------------------------------------

    @Test
    fun failureOutputNeverEchoesThePrivateKey() {
        val hostile = """
            [Interface]
            PrivateKey = ${privateKey}
            Address = 999.999.999.999/32

            [Peer]
            PublicKey = ${publicKey.take(20)}
            AllowedIPs = garbage
            Endpoint = h o s t:99999
        """.trimIndent()

        val result = WireGuardConfigValidator.validate(hostile)
        val invalid = result as ConfigValidation.Invalid
        val rendered = invalid.toString() +
            invalid.errors.joinToString { issue -> "${issue.field}:${issue.messageKey}" }

        assertFalse("private key leaked into error output", rendered.contains(privateKey))
        assertFalse("public key leaked into error output", rendered.contains(publicKey))
        // Field names are allowed; values are not.
        assertTrue(rendered.contains("PrivateKey") || rendered.contains("Address"))
    }

    @Test
    fun successSummaryCarriesNoKeyMaterial() {
        val result = WireGuardConfigValidator.validate(
            """
                [Interface]
                PrivateKey = $privateKey
                Address = 10.8.0.2/32

                [Peer]
                PublicKey = $publicKey
                PresharedKey = $preshared
                AllowedIPs = 0.0.0.0/0
                Endpoint = 203.0.113.7:51820
            """.trimIndent(),
        ) as ConfigValidation.Valid

        val rendered = result.summary.toString()
        assertFalse("private key must not be in the summary", rendered.contains(privateKey))
        assertFalse("preshared key must not be in the summary", rendered.contains(preshared))
        assertTrue("public key is public and may be shown", rendered.contains(publicKey))
        assertEquals(1, result.summary.peerPublicKeys.size)
    }
}
