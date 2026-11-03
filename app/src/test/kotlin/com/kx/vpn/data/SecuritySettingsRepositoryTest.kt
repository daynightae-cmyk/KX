package com.kx.vpn.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecuritySettingsRepositoryTest {

    private lateinit var repository: PreferencesSecuritySettingsRepository

    @Before
    fun setUp() {
        repository = PreferencesSecuritySettingsRepository(InMemoryPreferencesDataStore())
    }

    @Test
    fun defaultsToAllSecurityControlsActive() = runTest {
        val initial = repository.settings.first()
        assertTrue(initial.killSwitch)
        assertTrue(initial.dnsLeakProtection)
        assertTrue(initial.ipv6Protection)
        assertTrue(initial.autoReconnect)
        assertTrue(initial.trackerBlocking)
        assertTrue(initial.alwaysOnVpn)
    }

    @Test
    fun togglingKillSwitchPersists() = runTest {
        repository.setKillSwitch(false)
        assertFalse(repository.settings.first().killSwitch)

        repository.setKillSwitch(true)
        assertTrue(repository.settings.first().killSwitch)
    }

    @Test
    fun togglingDnsLeakProtectionPersists() = runTest {
        repository.setDnsLeakProtection(false)
        assertFalse(repository.settings.first().dnsLeakProtection)
    }

    @Test
    fun togglingTrackerBlockingPersists() = runTest {
        repository.setTrackerBlocking(false)
        assertFalse(repository.settings.first().trackerBlocking)
    }
}
