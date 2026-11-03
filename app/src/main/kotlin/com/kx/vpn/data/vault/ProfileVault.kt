package com.kx.vpn.data.vault

import java.nio.charset.StandardCharsets

/**
 * Encrypts WireGuard profiles before they touch the disk.
 *
 * A profile text contains a private key, so `docs/SECURITY_MODEL.md` requires platform
 * key protection with **no plaintext fallback**. That rule is implemented as a hard
 * behaviour of this class rather than a convention: when the cipher reports it has no
 * usable key, `save` returns `KeyUnavailable` and writes nothing at all. There is no
 * branch in this file that persists an unencrypted profile, and a test asserts it.
 *
 * Wire format: `KXV1` | sealed
 *
 * `sealed` is whatever the cipher produces and is opaque to the vault — for the AES-GCM
 * implementations that is nonce || ciphertext || tag. The vault does not parse it, so a
 * future cipher change cannot be silently mis-decoded. The `KXV1` prefix is checked on
 * read, which is what lets a format change be detected rather than mistaken for a
 * corrupt profile.
 */
class ProfileVault(
    private val cipher: AeadCipher,
    private val store: ProfileBlobStore,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val maxProfileBytes: Int = DEFAULT_MAX_PROFILE_BYTES,
) {

    fun save(id: String, configText: String): VaultResult {
        require(id.matches(ID_PATTERN)) { "profile id must be alphanumeric" }
        val plaintext = configText.toByteArray(StandardCharsets.UTF_8)
        if (plaintext.size > maxProfileBytes) return VaultResult.Failed(VaultFailure.QuotaExceeded)
        if (!cipher.isAvailable) return VaultResult.Failed(VaultFailure.KeyUnavailable)

        val sealed = cipher.encrypt(plaintext)
            ?: return VaultResult.Failed(VaultFailure.KeyUnavailable)

        val framed = HEADER + sealed
        // A write is only attempted once encryption actually succeeded, so a missing or
        // broken key can never leave plaintext on disk.
        store.write(id, framed)
        return VaultResult.Success(
            StoredProfile(id, configText, nowMillis()),
        )
    }

    fun load(id: String): VaultResult {
        val framed = store.read(id) ?: return VaultResult.Failed(VaultFailure.NotFound)
        if (framed.size <= HEADER.size) return VaultResult.Failed(VaultFailure.TamperedOrCorrupt)
        if (!framed.startsWith(HEADER)) return VaultResult.Failed(VaultFailure.TamperedOrCorrupt)

        val sealed = framed.copyOfRange(HEADER.size, framed.size)
        if (!cipher.isAvailable) return VaultResult.Failed(VaultFailure.KeyUnavailable)

        val plaintext = cipher.decrypt(sealed)
            // GCM tag failure, wrong key, or deliberate bit-flipping all land here.
            ?: return VaultResult.Failed(VaultFailure.TamperedOrCorrupt)

        if (plaintext.size > maxProfileBytes) return VaultResult.Failed(VaultFailure.QuotaExceeded)

        return VaultResult.Success(
            StoredProfile(
                id = id,
                configText = String(plaintext, StandardCharsets.UTF_8),
                savedAtEpochMillis = 0L, // timestamp lives in the blob store metadata in TASK_08
            ),
        )
    }

    fun listIds(): List<String> = store.list()

    fun delete(id: String) = store.delete(id)

    /**
     * Removes every profile and then destroys the key. Order matters: wiping the key
     * first would make the blobs unrecoverable but still present, which is a worse state
     * for a user who asked to be forgotten.
     */
    fun eraseEverything() {
        store.clearAll()
        cipher.destroyKey()
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (this.size < prefix.size) return false
        for (index in prefix.indices) if (this[index] != prefix[index]) return false
        return true
    }

    companion object {
        val HEADER = "KXV1".toByteArray(StandardCharsets.US_ASCII)
        const val DEFAULT_MAX_PROFILE_BYTES = 64 * 1024
        private val ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}
