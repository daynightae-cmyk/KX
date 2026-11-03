package com.kx.vpn.data.vault

/**
 * The minimal cipher contract the vault needs.
 *
 * Kept as an interface so the encryption *envelope* logic — framing, versioning, tamper
 * rejection, key-missing behaviour — is testable on the JVM, while production binds it
 * to a non-exportable Android Keystore key. Substituting the key source in tests is not
 * pretending: the framing and failure semantics are the part under test here.
 */
interface AeadCipher {
    /** @return self-describing ciphertext (nonce + body + tag), or null if the key is unusable. */
    fun encrypt(plaintext: ByteArray): ByteArray?

    /** @return plaintext, or null if authentication fails or the key is unusable. */
    fun decrypt(ciphertext: ByteArray): ByteArray?

    /** True only when a working key is actually available. */
    val isAvailable: Boolean

    /** Destroys the key. Irreversible by design; callers must warn the user first. */
    fun destroyKey()
}

/**
 * Where encrypted blobs live. Deliberately dumb: the vault owns all security decisions so
 * a storage swap cannot accidentally weaken them.
 */
interface ProfileBlobStore {
    fun write(id: String, bytes: ByteArray)
    fun read(id: String): ByteArray?
    fun delete(id: String)
    fun list(): List<String>
    fun clearAll()
}

/**
 * A stored profile, always after successful authenticated decryption.
 *
 * The generated `toString()` of a data class would print `configText`, which contains
 * the private key, into any log line or crash report that happens to include this
 * object. The override below replaces it with a redacted form — a data class gives
 * equality and copy for free, but its string form is a liability here.
 */
data class StoredProfile(
    val id: String,
    /** The WireGuard configuration text, including the private key. Never logged. */
    val configText: String,
    val savedAtEpochMillis: Long,
) {
    override fun toString(): String =
        "StoredProfile(id=$id, configText=<redacted ${configText.length} chars>, " +
            "savedAtEpochMillis=$savedAtEpochMillis)"
}

enum class VaultFailure {
    KeyUnavailable,
    TamperedOrCorrupt,
    NotFound,
    QuotaExceeded,
}

sealed interface VaultResult {
    data class Success(val profile: StoredProfile) : VaultResult
    data class Failed(val failure: VaultFailure) : VaultResult
}
