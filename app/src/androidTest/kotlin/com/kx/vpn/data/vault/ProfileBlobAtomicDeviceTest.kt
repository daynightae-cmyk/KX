package com.kx.vpn.data.vault

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Nonsecret, isolated filesystem fixture; does not connect a VPN or alter user profiles. */
@RunWith(AndroidJUnit4::class)
class ProfileBlobAtomicDeviceTest {
    @Test fun actualAndroidFilesystemSupportsCompleteAtomicProfileReplacement() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = Files.createTempDirectory(context.cacheDir.toPath(), "kx-atomic-test-").toFile()
        try {
            val store = FileProfileBlobStore(directory)
            store.write("fixture", byteArrayOf(1, 2, 3))
            val replacement = ByteArray(FileProfileBlobStore.DEFAULT_MAX_BLOB_BYTES) {
                (it % 239).toByte()
            }
            store.write("fixture", replacement)
            assertTrue(store.read("fixture")!!.contentEquals(replacement))
            assertFalse(File(directory, "fixture.tmp").exists())
        } finally {
            // Only the unique directory created by this test is removed.
            directory.deleteRecursively()
        }
    }
}
