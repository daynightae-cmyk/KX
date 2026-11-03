package com.kx.vpn.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kx.vpn.domain.servers.EndpointProfile
import com.kx.vpn.domain.servers.EndpointValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Everything the server screen needs: the user's own endpoints and their favourites. */
data class EndpointCatalogueState(
    val endpoints: List<EndpointProfile> = emptyList(),
    val favorites: Set<String> = emptySet(),
    /**
     * The endpoint the user last chose.
     *
     * This is a preference, not a claim: it says which configuration the app will use
     * next, and nothing about whether a tunnel is up.
     */
    val activeEndpointId: String? = null,
) {
    val isEmpty: Boolean get() = endpoints.isEmpty()
}

/**
 * Persists the endpoint catalogue and the user's favourites.
 *
 * Only **metadata** is stored here: a label, an optional country code, a host and a
 * port. The configuration itself — the part that holds the private key — lives in the
 * encrypted vault, and this repository never sees it.
 *
 * Favourites are a local preference. They are not uploaded and not shared; the
 * catalogue is the user's own list, not a provider's.
 */
class EndpointCatalogueRepository(
    private val preferences: DataStore<Preferences>,
) {

    val state: Flow<EndpointCatalogueState> = preferences.data
        .map { stored ->
            val endpoints = decodeEndpoints(stored[ENDPOINTS])
            val storedActive = stored[ACTIVE_ENDPOINT]?.takeIf { it.isNotEmpty() }
            EndpointCatalogueState(
                endpoints = endpoints,
                favorites = decodeFavorites(stored[FAVORITES]),
                // A preference naming an endpoint that is no longer in the catalogue is
                // dropped rather than resurrected: the choice has to point at something
                // the user actually has.
                activeEndpointId = storedActive?.takeIf { active ->
                    endpoints.any { it.id == active }
                },
            )
        }
        .catch { error ->
            // A corrupt store must not crash the list. An unreadable catalogue is an
            // empty catalogue, and the empty state offers the import action.
            if (error is IOException) {
                emit(EndpointCatalogueState())
            } else {
                throw error
            }
        }

    /** Adds or replaces an endpoint. Invalid metadata is rejected, not stored. */
    suspend fun upsert(profile: EndpointProfile): Boolean {
        if (EndpointValidator.validate(profile).isNotEmpty()) return false
        preferences.edit { stored ->
            val current = decodeEndpoints(stored[ENDPOINTS])
                .filterNot { it.id == profile.id }
            stored[ENDPOINTS] = encodeEndpoints(current + profile)
            // The first endpoint the user adds becomes the chosen one; after that the
            // choice is theirs and an import does not silently override it.
            if (stored[ACTIVE_ENDPOINT].isNullOrBlank()) stored[ACTIVE_ENDPOINT] = profile.id
        }
        return true
    }

    /**
     * Records the user's chosen endpoint.
     *
     * Refuses an id that is not in the catalogue, so a stale UI cannot persist a
     * selection for a server the user no longer has.
     */
    suspend fun setActiveEndpoint(endpointId: String): Boolean {
        var accepted = false
        preferences.edit { stored ->
            val known = decodeEndpoints(stored[ENDPOINTS]).any { it.id == endpointId }
            if (known) {
                stored[ACTIVE_ENDPOINT] = endpointId
                accepted = true
            }
        }
        return accepted
    }

    suspend fun remove(endpointId: String) {
        preferences.edit { stored ->
            val remaining = decodeEndpoints(stored[ENDPOINTS]).filterNot { it.id == endpointId }
            stored[ENDPOINTS] = encodeEndpoints(remaining)
            stored[FAVORITES] = encodeFavorites(
                decodeFavorites(stored[FAVORITES]) - endpointId,
            )
            if (stored[ACTIVE_ENDPOINT] == endpointId) {
                // Fall back to any endpoint still present, so removing a server never
                // leaves the app pointing at nothing.
                stored[ACTIVE_ENDPOINT] = remaining.firstOrNull()?.id.orEmpty()
            }
        }
    }

    /** Toggling a favourite is idempotent per call and persists immediately. */
    suspend fun toggleFavorite(endpointId: String) {
        preferences.edit { stored ->
            val current = decodeFavorites(stored[FAVORITES])
            stored[FAVORITES] = encodeFavorites(
                if (endpointId in current) current - endpointId else current + endpointId,
            )
        }
    }

    // -- encoding ---------------------------------------------------------------------
    //
    // Fields are URL-encoded and joined with a character that cannot appear in an
    // encoded field, so a label containing a newline or a separator cannot forge an
    // extra endpoint.

    private fun encodeEndpoints(endpoints: List<EndpointProfile>): String =
        endpoints.joinToString(RECORD_SEPARATOR) { profile ->
            listOf(
                profile.id,
                profile.displayName,
                profile.countryCode.orEmpty(),
                profile.host,
                profile.port.toString(),
                profile.profileId,
            ).joinToString(FIELD_SEPARATOR) { encode(it) }
        }

    private fun decodeEndpoints(raw: String?): List<EndpointProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(RECORD_SEPARATOR).mapNotNull { record ->
            val fields = record.split(FIELD_SEPARATOR)
            if (fields.size != 6) return@mapNotNull null
            val port = fields[4].toIntOrNull() ?: return@mapNotNull null
            EndpointProfile(
                id = decode(fields[0]),
                displayName = decode(fields[1]),
                countryCode = decode(fields[2]).takeIf { it.isNotEmpty() },
                host = decode(fields[3]),
                port = port,
                profileId = decode(fields[5]),
            )
        }
    }

    private fun encodeFavorites(ids: Set<String>): String =
        ids.sorted().joinToString(RECORD_SEPARATOR) { encode(it) }

    private fun decodeFavorites(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(RECORD_SEPARATOR).map { decode(it) }.filter { it.isNotEmpty() }.toSet()
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8.name()) }.getOrDefault("")

    private companion object {
        val ENDPOINTS = stringPreferencesKey("kx_endpoints")
        val FAVORITES = stringPreferencesKey("kx_favorites")
        val ACTIVE_ENDPOINT = stringPreferencesKey("kx_active_endpoint")
        const val FIELD_SEPARATOR = "\u001F"
        const val RECORD_SEPARATOR = "\u001E"
    }
}
