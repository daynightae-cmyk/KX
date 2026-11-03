package com.kx.vpn

import android.app.Application
import com.kx.vpn.data.LaunchStateRepository
import com.kx.vpn.data.ProfileRepository
import com.kx.vpn.data.TunnelSessionManager
import com.kx.vpn.data.createLaunchStateRepository
import com.kx.vpn.data.vault.FileProfileBlobStore
import com.kx.vpn.data.vault.KeystoreAesGcmCipher
import com.kx.vpn.data.vault.ProfileVault
import com.kx.vpn.platform.AndroidTunnelPlatform
import com.kx.vpn.platform.TunnelServiceRegistry
import java.io.File

/**
 * Application-scoped service locator, kept deliberately small.
 *
 * TASK_00 had one dependency to own. TASK_02 adds three more — the launch store, the
 * encrypted profile vault and the tunnel session — and they are all process-scoped for
 * the same reason: the VPN service lives in this process and has to be able to report
 * state back to the UI without a framework.
 */
class KXApplication : Application() {

    val launchStateRepository: LaunchStateRepository by lazy {
        createLaunchStateRepository(this)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            ProfileVault(
                cipher = KeystoreAesGcmCipher(),
                store = FileProfileBlobStore(File(filesDir, "profiles")),
            ),
        )
    }

    val tunnelSessionManager: TunnelSessionManager by lazy {
        TunnelSessionManager(
            platform = AndroidTunnelPlatform(this),
            profileSource = { profileRepository.activeConfigText() },
        )
    }

    override fun onCreate() {
        super.onCreate()
        // The service reports tunnel state changes back through the registry; the
        // manager turns them into state machine events.
        TunnelServiceRegistry.listener = { up ->
            tunnelSessionManager.onPlatformStateChanged(up)
        }
        tunnelSessionManager.refreshProfilePresence()
    }
}
