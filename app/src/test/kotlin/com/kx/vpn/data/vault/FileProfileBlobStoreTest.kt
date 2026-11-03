package com.kx.vpn.data.vault

import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class FileProfileBlobStoreTest {
    private fun withRoot(test: (File) -> Unit) {
        val root = Files.createTempDirectory("kx-profile-delete-test-").toFile()
        try { test(root) } finally { root.deleteRecursively() }
    }

    @Test fun deleteRemovesOnlyTheRequestedProfileAndItsTemporaryFile() = withRoot { root ->
        val store = FileProfileBlobStore(root)
        store.write("first", byteArrayOf(1, 2))
        store.write("second", byteArrayOf(3, 4))
        File(root, "first.tmp").writeText("temporary")
        store.delete("first")
        assertFalse(File(root, "first.kxprofile").exists())
        assertFalse(File(root, "first.tmp").exists())
        assertEquals(listOf("second"), store.list())
        assertTrue(store.read("second")!!.contentEquals(byteArrayOf(3, 4)))
    }

    @Test fun missingProfileDeletionIsIdempotent() = withRoot { root ->
        val store = FileProfileBlobStore(root)
        store.delete("missing"); store.delete("missing")
        assertTrue(root.isDirectory)
    }

    @Test fun invalidIdsCannotReadWriteOrDeleteOutsideTheStore() = withRoot { root ->
        val directory = File(root, "profiles")
        val store = FileProfileBlobStore(directory)
        val sentinel = File(root, "outside.kxprofile").apply { writeText("keep") }
        for (id in listOf("../outside", "../../outside", "bad/name", "", "a".repeat(65))) {
            val actions: List<() -> Unit> = listOf(
                { store.read(id); Unit },
                { store.write(id, byteArrayOf(1)) },
                { store.delete(id) },
            )
            actions.forEach { action ->
                var rejected = false
                try { action() } catch (_: IllegalArgumentException) { rejected = true }
                assertTrue("Invalid id must be rejected before filesystem access", rejected)
            }
        }
        assertEquals("keep", sentinel.readText())
    }

    @Test fun clearAllPreservesUnrelatedFilesAndDirectories() = withRoot { root ->
        val store = FileProfileBlobStore(root)
        store.write("default", byteArrayOf(1))
        File(root, "default.tmp").writeText("temporary")
        val unrelated = File(root, "notes.txt").apply { writeText("keep notes") }
        val folder = File(root, "other-folder").apply { mkdir() }
        val nested = File(folder, "keep.txt").apply { writeText("keep nested") }
        store.clearAll()
        assertTrue(store.list().isEmpty())
        assertFalse(File(root, "default.tmp").exists())
        assertEquals("keep notes", unrelated.readText())
        assertEquals("keep nested", nested.readText())
    }

    @Test fun directoryMasqueradingAsProfileIsNotRecursivelyDeletedOrReportedSuccessful() = withRoot { root ->
        val store = FileProfileBlobStore(root)
        val blocked = File(root, "blocked.kxprofile").apply { mkdir() }
        val sentinel = File(blocked, "keep.txt").apply { writeText("keep") }
        var failed = false
        try { store.delete("blocked") } catch (_: IOException) { failed = true }
        assertTrue(failed)
        assertEquals("keep", sentinel.readText())
        failed = false
        try { store.clearAll() } catch (_: IOException) { failed = true }
        assertTrue(failed)
        assertEquals("keep", sentinel.readText())
    }
    @Test fun encryptedBlobAtExactEnvelopeLimitRemainsReadable() = withRoot { root ->
        // KXV1 header (4) + max plaintext (65536) + GCM nonce (12) + tag (16).
        val bytes = ByteArray(65568) { (it % 251).toByte() }
        val store = FileProfileBlobStore(root)
        store.write("boundary", bytes)
        assertTrue(store.read("boundary")!!.contentEquals(bytes))
    }

    @Test fun oversizedEncryptedBlobIsRejectedWithoutDeletingIt() = withRoot { root ->
        val file = File(root, "oversize.kxprofile")
        file.writeBytes(ByteArray(65569))
        val store = FileProfileBlobStore(root)
        assertNull(store.read("oversize"))
        assertEquals(65569L, file.length())
    }

    @Test fun largeSparseBlobDoesNotBecomeAnInMemoryProfile() = withRoot { root ->
        val file = File(root, "sparse.kxprofile")
        java.io.RandomAccessFile(file, "rw").use { it.setLength(8L * 1024 * 1024) }
        assertNull(FileProfileBlobStore(root).read("sparse"))
        assertEquals(8L * 1024 * 1024, file.length())
    }

    @Test fun writeOverLimitIsRejectedBeforeReplacingExistingProfile() = withRoot { root ->
        val store = FileProfileBlobStore(root)
        val original = byteArrayOf(1, 2, 3)
        store.write("existing", original)
        var rejected = false
        try { store.write("existing", ByteArray(65569)) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertTrue(store.read("existing")!!.contentEquals(original))
        assertFalse(File(root, "existing.tmp").exists())
    }
    @Test fun growingInputIsRejectedAfterAtMostOneByteOverTheLimit() {
        var consumed = 0
        val stream = object : java.io.InputStream() {
            override fun read(): Int { consumed++; return 7 }
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                assertTrue("Reader must never request beyond limit+1", length <= 33 - consumed)
                java.util.Arrays.fill(bytes, offset, offset + length, 7.toByte())
                consumed += length
                return length
            }
        }
        assertNull(readBoundedProfileBlob(stream, 32))
        assertEquals(33, consumed)
    }

    @Test fun zeroLengthBulkReadsMakeProgressThroughSingleByteFallback() {
        var singleReads = 0
        val stream = object : java.io.InputStream() {
            override fun read() = if (singleReads++ < 3) 5 else -1
            override fun read(bytes: ByteArray, offset: Int, length: Int) = 0
        }
        assertTrue(readBoundedProfileBlob(stream, 3)!!.contentEquals(byteArrayOf(5, 5, 5)))
        assertEquals(4, singleReads)
    }

    @Test fun emptyInputAndValidShortInputPreserveTheirBytes() {
        assertTrue(readBoundedProfileBlob(byteArrayOf().inputStream(), 8)!!.isEmpty())
        assertTrue(readBoundedProfileBlob(byteArrayOf(1, 2, 3).inputStream(), 8)!!
            .contentEquals(byteArrayOf(1, 2, 3)))
    }

    @Test fun invalidLimitsFailBeforeReadingOrCreatingAStoreDirectory() = withRoot { root ->
        for (limit in listOf(0, -1, Int.MAX_VALUE)) {
            val destination = File(root, "profiles-$limit")
            var rejected = false
            try { FileProfileBlobStore(destination, limit) }
            catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
            assertFalse(destination.exists())
            rejected = false
            val stream = object : java.io.InputStream() {
                override fun read(): Int = throw AssertionError("Invalid limit must not start IO")
            }
            try { readBoundedProfileBlob(stream, limit) }
            catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
        }
    }
    @Test fun failedAtomicPublicationPreservesPriorBytesAndRemovesOnlyTemporaryBlob() = withRoot { root ->
        val target = File(root, "saved.kxprofile").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val temp = File(root, "saved.tmp")
        val unrelated = File(root, "unrelated.txt").apply { writeText("keep") }
        var failed = false
        try {
            writeProfileBlobAtomically(temp, target, byteArrayOf(9, 8)) { _, _ ->
                throw java.nio.file.AtomicMoveNotSupportedException("source", "target", "test-only")
            }
        } catch (_: IOException) { failed = true }
        assertTrue(failed)
        assertTrue(target.readBytes().contentEquals(byteArrayOf(1, 2, 3)))
        assertFalse(temp.exists())
        assertEquals("keep", unrelated.readText())
    }

    @Test fun repeatedAtomicReplacementPublishesTheCompleteNewBlob() = withRoot { root ->
        val store = FileProfileBlobStore(root)
        store.write("saved", byteArrayOf(1, 2, 3))
        val replacement = ByteArray(65568) { (it % 241).toByte() }
        store.write("saved", replacement)
        assertTrue(store.read("saved")!!.contentEquals(replacement))
        assertFalse(File(root, "saved.tmp").exists())
    }

    @Test fun failedTemporaryWriteDoesNotModifyExistingProfile() = withRoot { root ->
        val target = File(root, "saved.kxprofile").apply { writeText("original") }
        val temp = File(root, "saved.tmp").apply { mkdir() }
        val sentinel = File(temp, "keep.txt").apply { writeText("keep") }
        var published = false
        var failed = false
        try {
            writeProfileBlobAtomically(temp, target, byteArrayOf(4)) { _, _ -> published = true }
        } catch (_: IOException) { failed = true }
        assertTrue(failed)
        assertFalse(published)
        assertEquals("original", target.readText())
        assertEquals("keep", sentinel.readText())
    }
}
