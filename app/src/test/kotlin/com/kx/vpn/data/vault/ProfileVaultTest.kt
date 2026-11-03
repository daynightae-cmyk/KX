package com.kx.vpn.data.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Cipher
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.nio.charset.StandardCharsets

/**
 * Tests for the encryption envelope.
 *
 * A JVM AES-GCM cipher stands in for the AndroidKeyStore one. That substitution is
 * narrow and deliberate: what is under test here is the vault's framing, its refusal to
 * write when no key exists, its tamper rejection and its erase ordering — none of which
 * depend on where the key came from. The keystore path itself is exercised on a device
 * by the instrumented suite.
 */
class ProfileVaultTest {

    private class InMemoryBlobStore : ProfileBlobStore {
        val blobs = mutableMapOf<String, ByteArray>()
        var clearAllCalls = 0
        override fun write(id: String, bytes: ByteArray) { blobs[id] = bytes }
        override fun read(id: String): ByteArray? = blobs[id]
        override fun delete(id: String) { blobs.remove(id) }
        override fun list(): List<String> = blobs.keys.sorted()
        override fun clearAll() { clearAllCalls++; blobs.clear() }
    }

    private class JvmAesGcmCipher(
        var key: SecretKey? = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey(),
    ) : AeadCipher {
        var destroyCount = 0
        override val isAvailable: Boolean get() = key != null

        override fun encrypt(plaintext: ByteArray): ByteArray? {
            val secret = key ?: return null
            val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secret, GCMParameterSpec(128, nonce))
            return nonce + cipher.doFinal(plaintext)
        }

        override fun decrypt(ciphertext: ByteArray): ByteArray? {
            val secret = key ?: return null
            if (ciphertext.size <= 12) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                secret,
                GCMParameterSpec(128, ciphertext.copyOfRange(0, 12)),
            )
            return runCatching { cipher.doFinal(ciphertext.copyOfRange(12, ciphertext.size)) }.getOrNull()
        }

        override fun destroyKey() { destroyCount++; key = null }
    }

    private val privateKeyLine = "PrivateKey = wAaH0k1r3dummys3cretmaterialAAAAAAAAAAAAAAAAAAA="
    private val config = buildString {
        appendLine("[Interface]")
        appendLine(privateKeyLine)
        appendLine("Address = 10.8.0.2/32")
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = H3mR3a1P3rPub1cK3yAAAAAAAAAAAAAAAAAAAAAAA=")
        appendLine("AllowedIPs = 0.0.0.0/0")
        appendLine("Endpoint = 203.0.113.7:51820")
    }

    private fun vault(
        cipher: AeadCipher = JvmAesGcmCipher(),
        store: InMemoryBlobStore = InMemoryBlobStore(),
        maxBytes: Int = ProfileVault.DEFAULT_MAX_PROFILE_BYTES,
    ) = Triple(ProfileVault(cipher, store, nowMillis = { 1_700_000_000_000L }, maxProfileBytes = maxBytes), cipher, store)

    // -- Round trip -------------------------------------------------------------------

    @Test
    fun savesAndLoadsTheSameConfig() {
        val (profileVault, _, store) = vault()
        val saved = profileVault.save("lab1", config)
        assertTrue("save failed: $saved", saved is VaultResult.Success)
        assertEquals(1, store.blobs.size)

        val loaded = profileVault.load("lab1") as VaultResult.Success
        assertEquals(config, loaded.profile.configText)
    }

    @Test
    fun eachSaveProducesADifferentCiphertextBecauseTheNonceIsRandom() {
        val (profileVault, _, store) = vault()
        profileVault.save("a", config)
        profileVault.save("b", config)
        assertFalse(
            "two profiles of the same config must not produce identical blobs",
            store.blobs["a"]!!.contentEquals(store.blobs["b"]),
        )
    }

    // -- The no-plaintext-fallback rule ------------------------------------------------

    @Test
    fun plaintextNeverReachesTheStore() {
        val (profileVault, _, store) = vault()
        profileVault.save("lab1", config)

        val raw = store.blobs.values.joinToString("") { String(it, StandardCharsets.ISO_8859_1) }
        assertFalse("private key material found on disk", raw.contains("PrivateKey"))
        assertFalse("private key value found on disk", raw.contains("wAaH0k1r3dummys3cretmaterial"))
        assertFalse("public key line found on disk", raw.contains("Endpoint = 203.0.113.7"))
    }

    @Test
    fun aMissingKeyRefusesToWriteAnythingAtAll() {
        val cipher = JvmAesGcmCipher(key = null)
        val store = InMemoryBlobStore()
        val profileVault = ProfileVault(cipher, store)

        val result = profileVault.save("lab1", config)

        assertEquals(VaultResult.Failed(VaultFailure.KeyUnavailable), result)
        assertTrue("a failed save must leave nothing behind", store.blobs.isEmpty())
    }

    @Test
    fun aCipherThatReturnsNullOnEncryptAlsoWritesNothing() {
        val nullCipher = object : AeadCipher {
            override val isAvailable = true // claims to be fine, then fails
            override fun encrypt(plaintext: ByteArray): ByteArray? = null
            override fun decrypt(ciphertext: ByteArray): ByteArray? = null
            override fun destroyKey() = Unit
        }
        val store = InMemoryBlobStore()
        val result = ProfileVault(nullCipher, store).save("lab1", config)

        assertEquals(VaultResult.Failed(VaultFailure.KeyUnavailable), result)
        assertTrue(store.blobs.isEmpty())
    }

    @Test
    fun loadingWithoutAKeyReportsKeyUnavailableNotACrash() {
        val cipher = JvmAesGcmCipher()
        val store = InMemoryBlobStore()
        ProfileVault(cipher, store).save("lab1", config)

        val locked = JvmAesGcmCipher(key = null)
        val result = ProfileVault(locked, store).load("lab1")
        assertEquals(VaultResult.Failed(VaultFailure.KeyUnavailable), result)
    }

    // -- Tamper rejection ---------------------------------------------------------------

    @Test
    fun aSingleFlippedBitIsRejectedAsTampered() {
        val (profileVault, _, store) = vault()
        profileVault.save("lab1", config)

        val blob = store.blobs["lab1"]!!.copyOf()
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 0x01).toByte()
        store.blobs["lab1"] = blob

        assertEquals(VaultResult.Failed(VaultFailure.TamperedOrCorrupt), profileVault.load("lab1"))
    }

    @Test
    fun aReencryptedBlobWithADifferentKeyIsRejected() {
        val (profileVault, _, store) = vault()
        profileVault.save("lab1", config)

        val otherKeyVault = ProfileVault(JvmAesGcmCipher(), store)
        assertEquals(VaultResult.Failed(VaultFailure.TamperedOrCorrupt), otherKeyVault.load("lab1"))
    }

    @Test
    fun aMissingVersionHeaderIsRejected() {
        val cipher = JvmAesGcmCipher()
        val store = InMemoryBlobStore()
        val profileVault = ProfileVault(cipher, store)
        profileVault.save("lab1", config)

        val sealed = store.blobs["lab1"]!!.copyOfRange(ProfileVault.HEADER.size, store.blobs["lab1"]!!.size)
        store.blobs["lab1"] = "XXXX".toByteArray() + sealed

        assertEquals(VaultResult.Failed(VaultFailure.TamperedOrCorrupt), profileVault.load("lab1"))
    }

    @Test
    fun truncatedAndEmptyBlobsAreRejectedNotCrashed() {
        val (profileVault, _, store) = vault()
        listOf(
            ByteArray(0),
            ByteArray(3) { 7 },
            ProfileVault.HEADER,
            ProfileVault.HEADER + ByteArray(1),
        ).forEach { junk ->
            store.blobs["j"] = junk
            val result = profileVault.load("j")
            assertEquals(VaultResult.Failed(VaultFailure.TamperedOrCorrupt), result)
        }
    }

    @Test
    fun aTruncatedSealedBodyCannotForgeADecrypt() {
        val (profileVault, _, store) = vault()
        profileVault.save("lab1", config)
        val blob = store.blobs["lab1"]!!
        store.blobs["lab1"] = blob.copyOfRange(0, blob.size / 2)
        assertEquals(VaultResult.Failed(VaultFailure.TamperedOrCorrupt), profileVault.load("lab1"))
    }

    // -- Limits and identity ------------------------------------------------------------

    @Test
    fun oversizedProfilesAreRefusedBeforeEncryption() {
        val (profileVault, _, store) = vault(maxBytes = 64)
        val result = profileVault.save("big", config)
        assertEquals(VaultResult.Failed(VaultFailure.QuotaExceeded), result)
        assertTrue(store.blobs.isEmpty())
    }

    @Test
    fun missingProfileReportsNotFound() {
        val (profileVault, _, _) = vault()
        assertEquals(VaultResult.Failed(VaultFailure.NotFound), profileVault.load("ghost"))
    }

    @Test
    fun idsAreConstrainedToASafeCharset() {
        val (profileVault, _, _) = vault()
        listOf("../escape", "with space", "a".repeat(65), "path/traversal", "").forEach { bad ->
            assertThrowsIllegalArgumentException(bad) { profileVault.save(bad, config) }
        }
        // Safe ids work.
        assertNotNull(profileVault.save("ok-id_1", config))
    }

    private fun assertThrowsIllegalArgumentException(id: String, block: () -> Unit) {
        val thrown = runCatching(block).exceptionOrNull()
        assertTrue("id '$id' should have been rejected, got $thrown", thrown is IllegalArgumentException)
    }

    // -- Erase semantics ----------------------------------------------------------------

    @Test
    fun eraseClearsBlobsBeforeDestroyingTheKey() {
        val cipher = JvmAesGcmCipher()
        val store = InMemoryBlobStore()
        val profileVault = ProfileVault(cipher, store)
        profileVault.save("lab1", config)

        val order = mutableListOf<String>()
        val recordingStore = object : ProfileBlobStore {
            override fun write(id: String, bytes: ByteArray) = store.write(id, bytes)
            override fun read(id: String) = store.read(id)
            override fun delete(id: String) = store.delete(id)
            override fun list() = store.list()
            override fun clearAll() { order += "blobs"; store.clearAll() }
        }
        val recordingCipher = object : AeadCipher {
            override val isAvailable get() = cipher.isAvailable
            override fun encrypt(p: ByteArray) = cipher.encrypt(p)
            override fun decrypt(c: ByteArray) = cipher.decrypt(c)
            override fun destroyKey() { order += "key"; cipher.destroyKey() }
        }

        ProfileVault(recordingCipher, recordingStore).eraseEverything()

        assertEquals("blobs must be wiped first", listOf("blobs", "key"), order)
        assertTrue(store.blobs.isEmpty())
        assertFalse(cipher.isAvailable)
    }

    @Test
    fun listIdsReflectsStoredProfilesOnly() {
        val (profileVault, _, _) = vault()
        profileVault.save("alpha", config)
        profileVault.save("beta", config)
        assertEquals(listOf("alpha", "beta"), profileVault.listIds())
        profileVault.delete("alpha")
        assertEquals(listOf("beta"), profileVault.listIds())
    }

    /**
     * The vault must not be the place a secret leaks. Its own toString and the result
     * types are checked so a future `data class` change cannot start printing config text
     * into a log.
     */
    @Test
    fun resultTypesDoNotPrintTheSecret() {
        val (profileVault, _, _) = vault()
        val result = profileVault.save("lab1", config) as VaultResult.Success
        val rendered = result.toString()
        assertFalse(
            "VaultResult.Success.toString() must not include the config text",
            rendered.contains("wAaH0k1r3dummys3cretmaterial"),
        )
        assertFalse(rendered.contains("PrivateKey ="))
    }
}
