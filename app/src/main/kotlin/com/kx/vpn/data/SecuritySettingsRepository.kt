package com.kx.vpn.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persistence model for the Security Center controls.
 *
 * Defaults to true per the reference design (05-security-center.png) where all
 * security protections are active out-of-the-box.
 */
data class SecuritySettings(
    val killSwitch: Boolean = true,
    val dnsLeakProtection: Boolean = true,
    val ipv6Protection: Boolean = true,
    val autoReconnect: Boolean = true,
    val trackerBlocking: Boolean = true,
    val alwaysOnVpn: Boolean = true,
)

interface SecuritySettingsRepository {
    val settings: Flow<SecuritySettings>
    suspend fun setKillSwitch(enabled: Boolean)
    suspend fun setDnsLeakProtection(enabled: Boolean)
    suspend fun setIpv6Protection(enabled: Boolean)
    suspend fun setAutoReconnect(enabled: Boolean)
    suspend fun setTrackerBlocking(enabled: Boolean)
    suspend fun setAlwaysOnVpn(enabled: Boolean)
}

class PreferencesSecuritySettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SecuritySettingsRepository {

    private object Keys {
        val KILL_SWITCH = booleanPreferencesKey("security_kill_switch")
        val DNS_LEAK = booleanPreferencesKey("security_dns_leak")
        val IPV6 = booleanPreferencesKey("security_ipv6")
        val AUTO_RECONNECT = booleanPreferencesKey("security_auto_reconnect")
        val TRACKER_BLOCKING = booleanPreferencesKey("security_tracker_blocking")
        val ALWAYS_ON = booleanPreferencesKey("security_always_on")
    }

    override val settings: Flow<SecuritySettings> = dataStore.data.map { prefs ->
        SecuritySettings(
            killSwitch = prefs[Keys.KILL_SWITCH] ?: true,
            dnsLeakProtection = prefs[Keys.DNS_LEAK] ?: true,
            ipv6Protection = prefs[Keys.IPV6] ?: true,
            autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: true,
            trackerBlocking = prefs[Keys.TRACKER_BLOCKING] ?: true,
            alwaysOnVpn = prefs[Keys.ALWAYS_ON] ?: true,
        )
    }

    override suspend fun setKillSwitch(enabled: Boolean) {
        dataStore.edit { it[Keys.KILL_SWITCH] = enabled }
    }

    override suspend fun setDnsLeakProtection(enabled: Boolean) {
        dataStore.edit { it[Keys.DNS_LEAK] = enabled }
    }

    override suspend fun setIpv6Protection(enabled: Boolean) {
        dataStore.edit { it[Keys.IPV6] = enabled }
    }

    override suspend fun setAutoReconnect(enabled: Boolean) {
        dataStore.edit { it[Keys.AUTO_RECONNECT] = enabled }
    }

    override suspend fun setTrackerBlocking(enabled: Boolean) {
        dataStore.edit { it[Keys.TRACKER_BLOCKING] = enabled }
    }

    override suspend fun setAlwaysOnVpn(enabled: Boolean) {
        dataStore.edit { it[Keys.ALWAYS_ON] = enabled }
    }
}
