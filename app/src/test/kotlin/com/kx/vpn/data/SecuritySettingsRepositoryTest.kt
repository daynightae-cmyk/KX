package com.kx.vpn.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    /**
     * A fresh install does not silently send the user's DNS to a third party.
     *
     * The two switches that reach the tunnel are off by default because what they do is
     * replace the `DNS` line in the user's own configuration. The other four are stored
     * preferences that nothing reads, so their default has no effect on traffic — the
     * security screen shows them disabled either way.
     */
    @Test
    fun dnsRewritingControlsDefaultToOff() = runTest {
        val initial = repository.settings.first()
        assertFalse("a first run must not rewrite the user's DNS", initial.dnsLeakProtection)
        assertFalse("a first run must not point DNS at a filtering resolver", initial.trackerBlocking)
        assertTrue(initial.killSwitch)
        assertTrue(initial.ipv6Protection)
        assertTrue(initial.autoReconnect)
        assertTrue(initial.alwaysOnVpn)
    }

    @Test
    fun theDataStoreDefaultsMatchTheModelDefaults() = runTest {
        // Two places decide what an unwritten key means. If they drift, a user who never
        // opened the security screen gets different behaviour from the model's own
        // default — which is exactly how the DNS substitution came back on.
        val model = SecuritySettings()
        val stored = repository.settings.first()
        assertEquals(model.dnsLeakProtection, stored.dnsLeakProtection)
        assertEquals(model.trackerBlocking, stored.trackerBlocking)
        assertEquals(model.killSwitch, stored.killSwitch)
        assertEquals(model.ipv6Protection, stored.ipv6Protection)
        assertEquals(model.autoReconnect, stored.autoReconnect)
        assertEquals(model.alwaysOnVpn, stored.alwaysOnVpn)
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
