package com.kx.vpn.data

import com.kx.vpn.data.vault.ProfileVault
import com.kx.vpn.data.vault.VaultResult

/**
 * The application's single profile slot.
 *
 * There is deliberately no list of profiles yet — profile management is TASK_09/10
 * territory — but the concept of "the configuration the user chose" exists from
 * TASK_02 because the tunnel needs something to connect with.
 *
 * The decrypted configuration is cached in memory only while the app is running. It is
 * never written anywhere in the clear, and it is never logged.
 */
class ProfileRepository(
    private val vault: ProfileVault,
) {

    @Volatile
    private var cachedConfigText: String? = null

    @Volatile
    private var cachedProfileId: String? = null

    /** The configuration the tunnel should use, or null when there is none. */
    fun activeConfigText(): String? {
        cachedConfigText?.let { return it }
        val id = cachedProfileId ?: return null
        val loaded = vault.load(id)
        return (loaded as? VaultResult.Success)?.profile?.configText.also { text ->
            if (text != null) cachedConfigText = text
        }
    }

    fun hasProfile(): Boolean = activeConfigText() != null

    /**
     * Stores a configuration under the platform keystore key. The write only happens
     * after encryption succeeds, so a failed encryption leaves nothing behind.
     */
    fun save(id: String, configText: String): VaultResult {
        val result = vault.save(id, configText)
        if (result is VaultResult.Success) {
            cachedProfileId = id
            cachedConfigText = configText
        }
        return result
    }

    fun clear() {
        cachedProfileId?.let { vault.delete(it) }
        cachedProfileId = null
        cachedConfigText = null
    }
}
