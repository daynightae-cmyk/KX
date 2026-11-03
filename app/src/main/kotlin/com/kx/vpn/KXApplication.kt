package com.kx.vpn

import android.app.Application
import com.kx.vpn.data.LaunchStateRepository
import com.kx.vpn.data.createLaunchStateRepository

/**
 * Application-scoped service locator, kept deliberately small.
 *
 * TASK_00 has one dependency to own. When the VPN engine lands in TASK_02 this is the
 * seam it attaches to, so introducing a full DI framework now would be speculative.
 */
class KXApplication : Application() {
    val sessionRepository: com.kx.vpn.data.session.WireGuardSessionRepository by lazy {
        com.kx.vpn.data.session.WireGuardSessionRepository(this)
    }


    val launchStateRepository: LaunchStateRepository by lazy {
        createLaunchStateRepository(this)
    }
}
