package com.kx.vpn.data.vault

import java.io.File

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
) : ProfileBlobStore {

    init {
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IllegalStateException("could not create the profile directory")
        }
    }

    override fun write(id: String, bytes: ByteArray) {
        val target = fileFor(id)
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
        return runCatching { file.readBytes() }.getOrNull()
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

    private companion object {
        const val EXTENSION = ".kxprofile"
        val ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}
