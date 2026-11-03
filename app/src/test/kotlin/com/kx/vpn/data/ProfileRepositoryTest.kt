package com.kx.vpn.data

import com.kx.vpn.data.vault.AeadCipher
import com.kx.vpn.data.vault.ProfileBlobStore
import com.kx.vpn.data.vault.ProfileVault
import com.kx.vpn.data.vault.VaultResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The profile repository now holds several profiles and one choice.
 *
 * The cipher here is a stand-in that merely reverses the bytes. That is not dodging the
 * security test — `ProfileVaultTest` covers the real AES-GCM envelope, the tamper
 * rejection and the no-plaintext-fallback rule. What is under test below is which profile
 * the repository considers active, and when it refuses to change that.
 */
class ProfileRepositoryTest {

    private class ReversingCipher : AeadCipher {
        var available = true

        override fun encrypt(plaintext: ByteArray): ByteArray? =
            if (available) plaintext.reversed().toByteArray() else null

        override fun decrypt(ciphertext: ByteArray): ByteArray? =
            if (available) ciphertext.reversed().toByteArray() else null

        override val isAvailable: Boolean get() = available

        override fun destroyKey() {
            available = false
        }
    }

    private class InMemoryStore : ProfileBlobStore {
        private val blobs = mutableMapOf<String, ByteArray>()
        override fun write(id: String, bytes: ByteArray) { blobs[id] = bytes }
        override fun read(id: String): ByteArray? = blobs[id]
        override fun delete(id: String) { blobs.remove(id) }
        override fun list(): List<String> = blobs.keys.sorted()
        override fun clearAll() { blobs.clear() }
    }

    private fun repository() = ProfileRepository(
        ProfileVault(cipher = ReversingCipher(), store = InMemoryStore()),
    )

    private val configA = """
        [Interface]
        PrivateKey = aaaa

        [Peer]
        PublicKey = bbbb
        Endpoint = 203.0.113.1:51820
        AllowedIPs = 0.0.0.0/0
    """.trimIndent()

    private val configB = configA.replace("203.0.113.1", "203.0.113.2")

    @Test
    fun theFirstSavedProfileBecomesActive() {
        val profiles = repository()
        assertTrue(profiles.save("p1", configA) is VaultResult.Success)
        assertEquals("p1", profiles.activeProfileId())
        assertEquals(configA, profiles.activeConfigText())
    }

    @Test
    fun savingASecondProfileDoesNotStealTheChoice() {
        val profiles = repository()
        profiles.save("p1", configA)
        profiles.save("p2", configB)

        // Importing a server is not the same as switching to it — a running tunnel must
        // not be moved underneath the user by an unrelated action.
        assertEquals("p1", profiles.activeProfileId())
        assertEquals(configA, profiles.activeConfigText())
        assertTrue(profiles.hasStoredProfile("p2"))
    }

    @Test
    fun reSavingTheActiveProfileKeepsItActiveWithTheNewText() {
        val profiles = repository()
        profiles.save("p1", configA)
        profiles.save("p1", configB)

        assertEquals("p1", profiles.activeProfileId())
        assertEquals(configB, profiles.activeConfigText())
    }

    @Test
    fun activatingAnUnknownProfileFailsAndChangesNothing() {
        val profiles = repository()
        profiles.save("p1", configA)

        assertFalse(profiles.activate("ghost"))
        assertEquals("p1", profiles.activeProfileId())
    }

    @Test
    fun activationReadsTheStoredConfiguration() {
        val profiles = repository()
        profiles.save("p1", configA)
        profiles.save("p2", configB)

        assertTrue(profiles.activate("p2"))
        assertEquals(configB, profiles.activeConfigText())
    }

    @Test
    fun removingTheActiveProfileFallsBackToAnother() {
        val profiles = repository()
        profiles.save("p1", configA)
        profiles.save("p2", configB)

        profiles.remove("p1")

        assertEquals(listOf("p2"), profiles.knownProfileIds())
        assertEquals("p2", profiles.activeProfileId())
        assertEquals(configB, profiles.activeConfigText())
    }

    @Test
    fun removingTheLastProfileLeavesTheAppUnconfigured() {
        val profiles = repository()
        profiles.save("p1", configA)

        profiles.remove("p1")

        assertNull(profiles.activeProfileId())
        assertFalse(profiles.hasProfile())
    }

    /**
     * A profile that will not decrypt is not an active profile. The repository must say
     * so rather than reporting a configuration it cannot produce.
     */
    /**
     * A profile that will not decrypt cannot be selected. The repository refuses rather
     * than pointing the tunnel at a configuration it cannot produce.
     */
    @Test
    fun anUndecryptableProfileCannotBeActivated() {
        val cipher = ReversingCipher()
        val profiles = ProfileRepository(ProfileVault(cipher = cipher, store = InMemoryStore()))
        profiles.save("p1", configA)
        profiles.save("p2", configB)
        cipher.destroyKey()

        assertFalse("a profile that will not decrypt must not become active", profiles.activate("p2"))
        assertEquals("p1", profiles.activeProfileId())
    }

    /**
     * Forgetting a profile removes its plaintext from memory too. The cached
     * configuration exists so the tunnel does not have to re-decrypt on every command,
     * and a user who deleted the profile must not leave it behind there.
     */
    @Test
    fun removingAProfileDropsItsCachedTextAndFallsBackToAnother() {
        val cipher = ReversingCipher()
        val profiles = ProfileRepository(ProfileVault(cipher = cipher, store = InMemoryStore()))
        profiles.save("p1", configA)
        profiles.save("p2", configB)
        profiles.activate("p1")
        assertEquals(configA, profiles.activeConfigText())

        profiles.remove("p1")

        assertEquals("p2", profiles.activeProfileId())
        // Only p2's text is reachable now; p1 is gone from the store and from memory.
        assertNull(profiles.configTextFor("p1"))
        assertEquals(configB, profiles.activeConfigText())

        // With the key destroyed, nothing left on disk can be read, so the fallback
        // refuses to claim a profile that will not decrypt.
        cipher.destroyKey()
        profiles.remove("p2")
        assertNull(profiles.activeProfileId())
        assertFalse(profiles.hasProfile())
    }

    @Test
    fun aFailedWriteLeavesNoCacheBehind() {
        val cipher = ReversingCipher(
        ).apply { available = false }
        val profiles = ProfileRepository(ProfileVault(cipher = cipher, store = InMemoryStore()))

        assertFalse(profiles.save("p1", configA) is VaultResult.Success)
        assertNull(profiles.activeProfileId())
        assertFalse(profiles.hasProfile())
    }

    /** The configuration text is the private key material; its string form must not leak it. */
    @Test
    fun theVaultNeverExposesConfigurationThroughToString() {
        val profiles = repository()
        val saved = profiles.save("p1", configA) as VaultResult.Success

        val rendered = saved.profile.toString()
        assertFalse(rendered.contains("aaaa"))
        assertTrue(rendered.contains("redacted"))
    }
}
