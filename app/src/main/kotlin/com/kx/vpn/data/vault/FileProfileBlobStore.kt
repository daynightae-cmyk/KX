package com.kx.vpn.data.vault

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.ByteArrayOutputStream

/**
 * Stores encrypted profile blobs in the app's private directory.
 *
 * `filesDir` is private to the app on every supported Android version, so another app
 * cannot read these files. The contents are still encrypted with the keystore key:
 * private storage stops other apps, and encryption stops a backup or an offline
 * extraction of the device.
 */
class FileProfileBlobStore(
    private val directory: File,
    private val maxBlobBytes: Int = DEFAULT_MAX_BLOB_BYTES,
) : ProfileBlobStore {

    init {
        require(maxBlobBytes in 1..MAX_SUPPORTED_BLOB_BYTES) { "Invalid encrypted profile size limit" }
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IllegalStateException("could not create the profile directory")
        }
    }

    override fun write(id: String, bytes: ByteArray) {
        val target = fileFor(id)
        require(bytes.size <= maxBlobBytes) { "Encrypted profile exceeds storage limit" }
        // Write to a temporary file and rename, so a crash mid-write cannot leave a
        // half-written blob that would later be reported as a corrupt profile.
        val temp = File(directory, "$id.tmp")
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            target.writeBytes(bytes)
            temp.delete()
        }
    }

    override fun read(id: String): ByteArray? {
        val file = fileFor(id)
        if (!file.isFile) return null
        return try {
            // Length is only a fast rejection. Enforce the same ceiling during IO,
            // because a file can grow after this check; never readBytes() first.
            if (file.length() > maxBlobBytes) return null
            file.inputStream().use { readBoundedProfileBlob(it, maxBlobBytes) }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    override fun delete(id: String) {
        val target = fileFor(id) // Validate before either mutation.
        deleteOwnedFile(target)
        deleteOwnedFile(File(directory, "$id.tmp"))
    }

    override fun list(): List<String> = runCatching {
        directory.listFiles { file -> file.isFile && file.name.endsWith(EXTENSION) }
            ?.map { it.name.removeSuffix(EXTENSION) }
            ?.sorted()
            .orEmpty()
    }.getOrDefault(emptyList())

    override fun clearAll() {
        val files = directory.listFiles()
            ?: throw java.io.IOException("Profile directory cannot be inspected")
        files.filter { file ->
            val id = when {
                file.name.endsWith(EXTENSION) -> file.name.removeSuffix(EXTENSION)
                file.name.endsWith(".tmp") -> file.name.removeSuffix(".tmp")
                else -> return@filter false
            }
            ID_PATTERN.matches(id)
        }.forEach(::deleteOwnedFile)
    }

    private fun deleteOwnedFile(file: File) {
        if (!file.exists()) return
        if (!file.isFile || !file.delete() || file.exists()) {
            throw java.io.IOException("Profile removal unconfirmed")
        }
    }

    private fun fileFor(id: String): File {
        require(ID_PATTERN.matches(id)) { "Invalid profile id" }
        return File(directory, "$id$EXTENSION")
    }

    companion object {
        // Current KXV1 frame + 64KiB plaintext + Android AES-GCM nonce/tag.
        // A future envelope change must explicitly revise this storage contract.
        const val DEFAULT_MAX_BLOB_BYTES = 4 + 64 * 1024 + 12 + 16
        private const val MAX_SUPPORTED_BLOB_BYTES = 1024 * 1024
        private const val EXTENSION = ".kxprofile"
        private val ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}

/** Reads at most limit + 1 bytes; oversized input is rejected without mutation. */
internal fun readBoundedProfileBlob(stream: InputStream, limit: Int): ByteArray? {
    require(limit in 1..1024 * 1024) { "Invalid encrypted profile size limit" }
    val output = ByteArrayOutputStream(minOf(limit, 8192))
    val buffer = ByteArray(minOf(limit + 1, 8192))
    var total = 0
    while (true) {
        val count = stream.read(buffer, 0, minOf(buffer.size, limit - total + 1))
        if (count < 0) return output.toByteArray()
        if (count == 0) {
            // InputStream implementations should progress, but a zero-returning
            // wrapper must not cause an infinite loop or an empty-success result.
            val next = stream.read()
            if (next < 0) return output.toByteArray()
            if (++total > limit) return null
            output.write(next)
        } else {
            total += count
            if (total > limit) return null
            output.write(buffer, 0, count)
        }
    }
}
