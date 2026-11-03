package com.kx.vpn.data

import com.kx.vpn.data.vault.ProfileVault
import com.kx.vpn.data.vault.VaultResult

/**
 * The encrypted profiles the user owns, and which one the tunnel uses.
 *
 * TASK_02 needed a single slot; TASK_03 needs a selection, so this holds one active id
 * over a vault that can already store many. The decrypted configuration is cached in
 * memory only, for the id it was loaded under, and is never written anywhere in the
 * clear or logged.
 *
 * The active id is deliberately *not* persisted here. It lives in the endpoint
 * catalogue, which stores metadata only — this class must not gain a reason to write
 * configuration-derived state to an unencrypted store.
 */
class ProfileRepository(
    private val vault: ProfileVault,
) {

    /** An id and the configuration that was decrypted for it, cached together. */
    private data class Cached(val profileId: String, val configText: String)

    @Volatile
    private var activeId: String? = null

    @Volatile
    private var cache: Cached? = null

    /** The profile the tunnel will use, or null when none is selected. */
    fun activeProfileId(): String? = activeId

    /** The configuration the tunnel should use, or null when there is none. */
    fun activeConfigText(): String? = activeId?.let(::configTextFor)

    fun hasProfile(): Boolean = activeConfigText() != null

    /** True when the vault actually holds an encrypted profile under [id]. */
    fun hasStoredProfile(id: String): Boolean = id in vault.listIds()

    /** Every profile id the vault holds. Ids only — never configuration text. */
    fun knownProfileIds(): List<String> = vault.listIds()

    /**
     * Decrypts the profile under [id], or returns null when it is missing or will not
     * decrypt. Reading a profile never makes it active.
     */
    fun configTextFor(id: String): String? {
        cache?.let { if (it.profileId == id) return it.configText }
        val loaded = vault.load(id) as? VaultResult.Success ?: return null
        // Cached only after a successful decrypt, so a corrupt blob is retried rather
        // than remembered as absent.
        cache = Cached(id, loaded.profile.configText)
        return loaded.profile.configText
    }

    /**
     * Points the tunnel at an existing profile.
     *
     * Returns false, and changes nothing, when the vault has no readable profile under
     * [id] — selecting an endpoint whose configuration is not on this device must fail
     * rather than leave the app pointing at a profile that does not exist.
     */
    fun activate(id: String): Boolean {
        if (configTextFor(id) == null) return false
        activeId = id
        return true
    }

    /**
     * Stores a configuration under the platform keystore key.
     *
     * It becomes the active profile only when nothing was active yet, or when it
     * replaced the profile that was already active. Importing a second server therefore
     * must not silently move a running tunnel — the user chooses it from the server list.
     * The write only happens after encryption succeeds, so a failed encryption leaves
     * nothing behind, and the in-memory cache is never updated for a write that failed.
     */
    fun save(id: String, configText: String): VaultResult {
        val result = vault.save(id, configText)
        if (result is VaultResult.Success) {
            cache = Cached(id, configText)
            val current = activeId
            // Also takes over when the pointer is dangling — a profile that will not
            // decrypt is not an active profile, and leaving it in place would strand the
            // app with a stored configuration it refuses to use.
            if (current == null || current == id || activeConfigText() == null) activeId = id
        }
        return result
    }

    /**
     * The tunnel-side address the configuration under [id] assigns, or null when it
     * declares none. Read from the user's own `Address` line — never guessed — and the
     * only address-shaped value this app is able to show honestly.
     */
    fun tunnelAddressFor(id: String): String? =
        configTextFor(id)?.let(com.kx.vpn.domain.servers.ProfileEndpointReader::tunnelAddress)

    /**
     * Forgets one profile.
     *
     * The decrypted text cached for that id is dropped with it: asking the app to forget
     * a configuration has to remove it from memory as well as from the disk. When the
     * removed profile was the active one, the active slot moves to another stored profile
     * if one exists, so deleting a server does not leave the app with no way to connect.
     */
    fun remove(id: String) {
        vault.delete(id)
        cache = cache?.takeUnless { it.profileId == id }
        if (activeId != id) return
        // The replacement is only real if it can actually be read back; otherwise the app
        // would claim a profile it cannot use.
        val replacement = vault.listIds().firstOrNull { configTextFor(it) != null }
        activeId = replacement
    }
}
