package com.kx.vpn.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** One profile, encrypted with a non-exportable AndroidKeyStore AES key; no fallback. */
class EncryptedTunnelStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "owned-tunnel.enc"))

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }

    fun exists(): Boolean = file.baseFile.exists()

    @Synchronized fun write(bytes: ByteArray) {
        require(bytes.size in 1..65536)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(bytes)
        val output = file.startWrite()
        try {
            output.write(1)
            output.write(cipher.iv.size)
            output.write(cipher.iv)
            output.write(encrypted)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        } finally { encrypted.fill(0) }
    }

    @Synchronized fun read(): ByteArray {
        val input = file.openRead()
        val payload = input.use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(4096)
            while (true) {
                val count = stream.read(chunk)
                if (count < 0) break
                require(output.size() + count <= 66000)
                output.write(chunk, 0, count)
            }
            output.toByteArray()
        }
        try {
            require(payload.size >= 30 && payload[0].toInt() == 1 && payload[1].toInt() == 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload, 2, 12))
            return cipher.doFinal(payload, 14, payload.size - 14)
        } finally { payload.fill(0) }
    }

    @Synchronized fun delete() = file.delete()

    private companion object { const val ALIAS = "kx-owned-wireguard-v1" }
}
