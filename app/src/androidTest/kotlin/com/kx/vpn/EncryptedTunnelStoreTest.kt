package com.kx.vpn

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kx.vpn.data.session.EncryptedTunnelStore
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedTunnelStoreTest {
    @Test fun maintainedWireGuardNativeLibraryLoadsWithoutStartingATunnel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backend = com.wireguard.android.backend.GoBackend(context)
        assertTrue(backend.version.isNotBlank())
        assertTrue(backend.runningTunnelNames.isEmpty())
    }

    @Test fun androidKeystoreRoundTripAndTamperRejection() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.noBackupFilesDir, "owned-tunnel.enc")
        // Test devices only. Preserve an existing encrypted profile if present.
        check(!file.exists()) { "Refusing to replace an existing user profile in this test" }
        val store = EncryptedTunnelStore(context)
        val fixture = "PUBLIC-NONSECRET-TEST-FIXTURE".toByteArray()
        try {
            store.write(fixture)
            assertTrue(store.exists())
            assertArrayEquals(fixture, store.read())
            assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains("PUBLIC-NONSECRET"))
            val envelope = file.readBytes()
            envelope[envelope.lastIndex] = (envelope.last().toInt() xor 1).toByte()
            file.writeBytes(envelope)
            var rejected = false
            try { store.read() } catch (_: Exception) { rejected = true }
            assertTrue("Authenticated encryption must reject modified ciphertext", rejected)
        } finally { store.delete() }
        assertFalse(store.exists())
    }
}
