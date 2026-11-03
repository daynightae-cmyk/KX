package com.kx.vpn.data.vault

import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}
