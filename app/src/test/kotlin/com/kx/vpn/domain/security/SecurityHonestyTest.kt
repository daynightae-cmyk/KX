package com.kx.vpn.domain.security

import com.kx.vpn.data.SecuritySettings
import com.kx.vpn.domain.config.VpnConfigModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The honesty rules the security centre lives by.
 *
 * These are structural rather than cosmetic: each one fails the build if a future change
 * quietly reintroduces a claim the app cannot support.
 *
 *  1. A control the build does not honour is reported as not enforced, and the screen has
 *     no way to render it as working.
 *  2. No string resource in the app — in either language — asserts that the user is
 *     protected or that their data is secured.
 *  3. The two switches that do reach the tunnel leave the user's own DNS alone until the
 *     user turns them on.
 */
class SecurityHonestyTest {

    // -- 1. The enforcement table -------------------------------------------------------

    @Test
    fun persistedOnlyControlsAreReportedAsNotEnforced() {
        listOf(
            SecurityControlKey.KILL_SWITCH,
            SecurityControlKey.IPV6_PROTECTION,
            SecurityControlKey.AUTO_RECONNECT,
        ).forEach { key ->
            assertEquals("$key is written to storage and read by nothing", Enforcement.NOT_ENFORCED, SecurityEnforcement.of(key))
            assertFalse("$key must not be tappable", SecurityEnforcement.isOperable(key))
        }
    }

    @Test
    fun dnsRewritingControlsRequireDisclosureBeforeTheyAreUsed() {
        listOf(
            SecurityControlKey.DNS_LEAK_PROTECTION,
            SecurityControlKey.TRACKER_BLOCKING,
        ).forEach { key ->
            assertEquals(
                "$key really does change the configuration, so it must state what it changes",
                Enforcement.ENFORCED_WITH_DISCLOSURE,
                SecurityEnforcement.of(key),
            )
            assertTrue(SecurityEnforcement.isOperable(key))
        }
    }

    @Test
    fun everyControlHasAnEntrySoNoneDefaultsToLookingEnforced() {
        // If a control is added and not classified, this fails instead of letting the
        // new switch render as though it worked.
        val classified = SecurityControlKey.entries.toSet()
        assertEquals(SecurityControlKey.entries.size, classified.size)
        classified.forEach { SecurityEnforcement.of(it) }
    }

    // -- 2. No protection language anywhere ---------------------------------------------

    /**
     * Phrases that would claim an outcome this build has never measured.
     *
     * "Protected", "secured", "safe" and their Arabic equivalents are exactly what
     * `docs/SECURITY_MODEL.md` forbids drawing on the strength of a tunnel interface
     * existing, and the reference artwork uses several of them as decoration.
     */
    private val bannedEnglishPhrases = listOf(
        "Fully Protected",
        "connection and data are secured",
        "You are protected",
        "You're fully protected",
    )

    private val bannedArabicPhrases = listOf(
        "محمي بالكامل",
        "مؤمنة بالكامل",
        "أنت محمي",
    )

    @Test
    fun noStringResourceClaimsProtection() {
        val english = File("src/main/res/values/strings.xml")
        val arabic = File("src/main/res/values-ar/strings.xml")
        assertTrue(
            "the test must read the real resources, not pass because a path was wrong",
            english.isFile && arabic.isFile,
        )

        listOf(english to bannedEnglishPhrases, arabic to bannedArabicPhrases)
            .forEach { (file, banned) ->
                val text = file.readText()
                banned.forEach { phrase ->
                    assertFalse(
                        "${file.path} must not contain \"$phrase\"",
                        text.contains(phrase, ignoreCase = true),
                    )
                }
            }
    }

    @Test
    fun theSecurityScreenHasNoProtectedStringLeftToReach() {
        // The keys the screen used to render are gone, not merely unused, so nothing can
        // point at them again by accident.
        val text = File("src/main/res/values/strings.xml").readText()
        assertFalse(text.contains("security_status_protected"))
        assertFalse(text.contains("security_subtitle_protected"))
    }

    // -- 3. The user's DNS survives untouched by default --------------------------------

    private val sampleConfig = """
        [Interface]
        PrivateKey = aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
        Address = 10.0.0.2/32
        DNS = 10.8.0.1

        [Peer]
        PublicKey = bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
        Endpoint = 198.51.100.1:51820
        AllowedIPs = 0.0.0.0/0
    """.trimIndent()

    @Test
    fun defaultSettingsDoNotRewriteTheUsersOwnDns() {
        val modified = VpnConfigModifier.applySecuritySettings(sampleConfig, SecuritySettings())

        assertEquals(
            "a first run must not send the user's DNS to a third party they never chose",
            sampleConfig,
            modified,
        )
        assertTrue(modified.contains("DNS = 10.8.0.1"))
    }

    @Test
    fun theRewritersAreNamedByTheSameConstantsTheScreenDiscloses() {
        // If the modifier's resolver ever changes, the disclosure label has to change
        // with it, or the screen is describing a substitution that no longer happens.
        val trackerOut = VpnConfigModifier.applySecuritySettings(
            sampleConfig,
            SecuritySettings(trackerBlocking = true, dnsLeakProtection = false),
        )
        val dnsOut = VpnConfigModifier.applySecuritySettings(
            sampleConfig,
            SecuritySettings(trackerBlocking = false, dnsLeakProtection = true),
        )
        assertTrue(trackerOut.contains("DNS = ${SecurityEnforcement.TRACKER_RESOLVERS}"))
        assertTrue(dnsOut.contains("DNS = ${SecurityEnforcement.DNS_LEAK_RESOLVERS}"))
    }
}
