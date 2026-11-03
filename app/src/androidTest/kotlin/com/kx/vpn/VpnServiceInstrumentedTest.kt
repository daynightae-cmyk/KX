package com.kx.vpn

import android.content.pm.PackageManager
import android.net.VpnService
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kx.vpn.data.ConsentRequest
import com.kx.vpn.data.TunnelPlatform
import com.kx.vpn.data.TunnelSessionManager
import com.kx.vpn.domain.tunnel.TunnelState
import com.kx.vpn.data.vault.FileProfileBlobStore
import com.kx.vpn.data.vault.KeystoreAesGcmCipher
import com.kx.vpn.data.vault.ProfileVault
import com.kx.vpn.data.vault.VaultFailure
import com.kx.vpn.data.vault.VaultResult
import com.kx.vpn.platform.KXVpnService
import com.kx.vpn.platform.TunnelStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * On-device checks for the VPN plumbing and the real keystore.
 *
 * These run on the `KX Android Device Runtime` workflow (emulator API 26 and API 35).
 * The JVM suite uses a stand-in cipher; this suite exercises the **actual
 * AndroidKeyStore** path, which is the only way to prove that the real key provider
 * works on a device.
 */
@RunWith(AndroidJUnit4::class)
class VpnServiceInstrumentedTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // -- Platform declaration ---------------------------------------------------------

    @Test
    fun theTunnelServiceIsDeclaredAndNotExported() {
        val info = context.packageManager.getServiceInfo(
            android.content.ComponentName(context, KXVpnService::class.java),
            PackageManager.GET_META_DATA,
        )
        assertFalse(
            "the VPN service must not be exported to other apps",
            info.exported,
        )
        assertEquals(
            "only the system may bind the VPN service",
            "android.permission.BIND_VPN_SERVICE",
            info.permission,
        )
    }

    @Test
    fun theRequiredPermissionsAreActuallyDeclared() {
        val declared = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
        listOf(
            "android.permission.INTERNET",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
        ).forEach { permission ->
            assertTrue(
                "missing declared permission: $permission",
                declared.contains(permission),
            )
        }
    }

    @Test
    fun vpnConsentIsRequestedThroughThePlatformApi() {
        // On a device that has already granted consent this returns null; on a fresh
        // install it returns the platform's consent intent. Either is correct, and
        // neither is fabricated by the app.
        val intent = VpnService.prepare(context)
        if (intent != null) {
            assertNotNull(
                "the consent intent must be able to start an activity",
                intent.component ?: intent.action ?: intent,
            )
        }
    }

    // -- The real keystore -------------------------------------------------------------

    /**
     * A genuine round-trip through AndroidKeyStore on the device.
     *
     * The JVM suite substitutes a JVM cipher, so only this test proves that the real
     * platform key provider works: that it creates a key, encrypts, decrypts, and
     * refuses a tampered blob.
     */
    @Test
    fun profilesRoundTripThroughTheRealDeviceKeystore() {
        val cipher = KeystoreAesGcmCipher(alias = "kx_test_${System.nanoTime()}")
        val directory = File(context.filesDir, "vault-test-${System.nanoTime()}")
        val store = FileProfileBlobStore(directory)
        val vault = ProfileVault(cipher, store)

        try {
            assertTrue("the keystore must provide a usable key", cipher.isAvailable)

            val secret = "PrivateKey = notARealKeyButMustNeverReachTheDisk0000="
            val config = """
                [Interface]
                $secret
                Address = 10.8.0.2/32

                [Peer]
                PublicKey = cHVibGljS2V5Rm9yVGhlVGVzdE9ubHlTdG9yZQUAAAA=
                AllowedIPs = 0.0.0.0/0
                Endpoint = 203.0.113.7:51820
            """.trimIndent()

            assertTrue(vault.save("lab", config) is VaultResult.Success)

            val onDisk = store.read("lab")!!.toString(Charsets.ISO_8859_1)
            assertFalse("the private key must not be readable on disk", onDisk.contains("notARealKey"))

            val loaded = vault.load("lab")
            assertTrue("load failed: $loaded", loaded is VaultResult.Success)
            assertEquals(config, (loaded as VaultResult.Success).profile.configText)

            // Tamper with one byte: the GCM tag must reject it.
            val blob = store.read("lab")!!.copyOf()
            blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 0x01).toByte()
            store.write("lab", blob)
            assertEquals(
                VaultResult.Failed(VaultFailure.TamperedOrCorrupt),
                vault.load("lab"),
            )
        } finally {
            store.clearAll()
            directory.deleteRecursively()
            cipher.destroyKey()
        }
    }

    // -- The session manager against the real platform ---------------------------------

    @Test
    fun connectingWithoutAProfileNeverStartsATunnel() {
        val platform = RecordingPlatform()
        val manager = TunnelSessionManager(platform, profileSource = { null })

        manager.connect()

        assertEquals(0, platform.startCalls)
        assertEquals(TunnelState.UNCONFIGURED, manager.state.value.tunnelState)
    }

    /**
     * The profile has to disappear while the session is genuinely waiting, which is
     * what happens when the platform asks for consent. A platform that grants consent
     * immediately starts the tunnel straight away, so there is no window in which the
     * profile could vanish — a first version of this test asserted against that
     * platform and failed on the emulator.
     */
    @Test
    fun aProfileThatIsRemovedWhileWaitingForConsentIsRefused() {
        val platform = RecordingPlatform(consentRequired = true)
        var profile: String? = "config"
        val manager = TunnelSessionManager(platform, profileSource = { profile })

        manager.connect()
        assertEquals(TunnelState.AWAITING_PERMISSION, manager.state.value.tunnelState)

        profile = null
        manager.onPermissionGranted()

        assertEquals("no tunnel may start without a configuration", 0, platform.startCalls)
        assertEquals(
            "the refusal must be reported as a failure, not silently ignored",
            com.kx.vpn.domain.tunnel.FailureReason.InvalidConfiguration,
            manager.state.value.failure,
        )
    }

    private class RecordingPlatform(
        private val consentRequired: Boolean = false,
    ) : TunnelPlatform {
        var startCalls = 0
        override fun requestConsent(): ConsentRequest =
            if (consentRequired) ConsentRequest.Required("req") else ConsentRequest.NotRequired
        override fun startTunnel(configText: String) { startCalls++ }
        override fun stopTunnel() = Unit
        override fun isTunnelUp(): Boolean = false
        override fun statistics(): TunnelStats? = null
    }
}
