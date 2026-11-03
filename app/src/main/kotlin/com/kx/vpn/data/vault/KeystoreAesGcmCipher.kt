package com.kx.vpn.data.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM keyed by a non-exportable AndroidKeyStore key.
 *
 * This is the "platform primitives, no plaintext fallback" requirement from
 * `docs/SECURITY_MODEL.md`. The key is generated inside the keystore with
 * `setRandomizedEncryptionRequired(true)`, so it cannot be read out and the nonce is
 * produced by the platform rather than by this code.
 *
 * Every failure path returns null or `isAvailable = false` rather than throwing, because
 * the caller's contract is "no plaintext was written" — and a thrown exception from a
 * keystore that is locked, wiped or unavailable would push a careless caller toward a
 * fallback. There is no fallback here to fall through to.
 *
 * Nothing in this class logs a key, a nonce or a plaintext.
 */
class KeystoreAesGcmCipher(
    private val alias: String = KEY_ALIAS,
) : AeadCipher {

    private fun keyStore(): KeyStore? = runCatching {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }.getOrNull()

    private fun existingKey(): SecretKey? = runCatching {
        keyStore()?.getKey(alias, null) as? SecretKey
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey? {
        existingKey()?.let { return it }
        return runCatching {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    // AndroidKeyStore keys are non-exportable by default; there is no
                    // builder flag for it, and adding one would not compile.
                    .build(),
            )
            generator.generateKey()
        }.getOrNull()
    }

    override val isAvailable: Boolean
        get() = getOrCreateKey() != null

    override fun encrypt(plaintext: ByteArray): ByteArray? {
        val key = getOrCreateKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            // The platform supplies the nonce; it must travel with the ciphertext.
            cipher.iv + cipher.doFinal(plaintext)
        }.getOrNull()
    }

    override fun decrypt(ciphertext: ByteArray): ByteArray? {
        val key = existingKey() ?: return null
        if (ciphertext.size <= NONCE_BYTES) return null
        return runCatching {
            val nonce = ciphertext.copyOfRange(0, NONCE_BYTES)
            val body = ciphertext.copyOfRange(NONCE_BYTES, ciphertext.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            cipher.doFinal(body)
        }.getOrNull() // a tampered tag, a wiped key or a rotated key all land here
    }

    override fun destroyKey() {
        runCatching { keyStore()?.deleteEntry(alias) }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "kx_profile_master"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
    }
}
