package com.kx.vpn.domain.servers

import java.text.Normalizer
import java.util.Locale

/**
 * A measurement taken against an endpoint.
 *
 * `latencyMillis` is null when the endpoint could not be reached. There is no "load"
 * or "capacity" field anywhere in this model, because nothing in this app can measure
 * those honestly — the reference artwork shows "Load 32%", and reproducing that number
 * would be inventing it.
 */
data class EndpointMeasurement(
    val reachable: Boolean,
    val latencyMillis: Long?,
    val measuredAtEpochMillis: Long,
) {
    companion object {
        /** A measurement older than this is shown as stale rather than as current. */
        const val STALE_AFTER_MILLIS: Long = 5 * 60 * 1000L

        fun unreachable(now: Long) =
            EndpointMeasurement(reachable = false, latencyMillis = null, measuredAtEpochMillis = now)

        fun success(latencyMillis: Long, now: Long) =
            EndpointMeasurement(reachable = true, latencyMillis = latencyMillis, measuredAtEpochMillis = now)
    }

    fun isFresh(now: Long): Boolean = now - measuredAtEpochMillis <= STALE_AFTER_MILLIS

    /** The latency to display, or null when there is nothing truthful to display. */
    fun displayLatency(now: Long): Long? =
        if (reachable && latencyMillis != null && isFresh(now)) latencyMillis else null
}

/** The filters the reference screen offers. */
enum class ServerFilter {
    All,
    Fastest,
    Streaming,
    Gaming;

    /**
     * A filter that needs metadata no configuration carries is disabled rather than
     * shown as if it worked. "Streaming" and "Gaming" need a capability declaration
     * that no profile provides, so they stay disabled until one does.
     */
    fun isEnabled(hasCapability: Boolean): Boolean = when (this) {
        All, Fastest -> true
        Streaming, Gaming -> hasCapability
    }
}

/** An endpoint as the list renders it, with everything known about it. */
data class ServerRow(
    val profile: EndpointProfile,
    val measurement: EndpointMeasurement?,
    val isFavorite: Boolean,
    val isRecommended: Boolean,
    /** True when the metadata needed by a capability filter is present. */
    val supportsStreaming: Boolean = false,
    val supportsGaming: Boolean = false,
)

/**
 * Country names come from the platform's own locale database rather than a table
 * bundled in the app. That keeps the searchable strings correct in every language the
 * device supports, including Arabic, without shipping a country-name list that would
 * have to be maintained and could drift.
 */
interface CountryNameResolver {
    fun displayName(countryCode: String, locale: Locale): String
}

/** The filters, sorting and search rules for the server list. */
object ServerCatalogue {

    const val FASTEST_LIMIT = 3

    /**
     * Builds the rows.
     *
     * "Recommended" is the single endpoint with the lowest fresh measured latency.
     * It is not a marketing label: with no measurement there is no recommendation,
     * and the badge simply does not appear.
     */
    fun buildRows(
        profiles: List<EndpointProfile>,
        measurements: Map<String, EndpointMeasurement>,
        favorites: Set<String>,
        now: Long,
        countryNames: CountryNameResolver? = null,
        locale: Locale = Locale.getDefault(),
    ): List<ServerRow> {
        val rows = profiles.map { profile ->
            ServerRow(
                profile = profile,
                measurement = measurements[profile.id],
                isFavorite = profile.id in favorites,
                isRecommended = false,
                supportsStreaming = false,
                supportsGaming = false,
            )
        }

        val recommended = rows
            .filter { it.measurement?.displayLatency(now) != null }
            .minByOrNull { it.measurement?.displayLatency(now) ?: Long.MAX_VALUE }
            ?.profile
            ?.id

        return rows.map { row ->
            row.copy(isRecommended = row.profile.id == recommended)
        }
    }

    /**
     * Applies a filter.
     *
     * A disabled filter is a no-op rather than an empty list: tapping a chip that
     * cannot work must not make the user's list vanish.
     *
     * [now] is the same instant the rows were built for, so "Fastest" cannot keep a
     * measurement that the rows themselves already refuse to display.
     */
    fun filter(rows: List<ServerRow>, filter: ServerFilter, now: Long): List<ServerRow> =
        when (filter) {
        ServerFilter.All -> rows
        ServerFilter.Fastest -> rows
            .filter { it.measurement?.displayLatency(now) != null }
            .sortedBy { it.measurement?.latencyMillis ?: Long.MAX_VALUE }
            .take(FASTEST_LIMIT)

        // A capability filter that no endpoint declares cannot discriminate between
        // them, so it is not applied at all. Returning an empty list would destroy the
        // user's servers because of a metadata gap that is not their fault.
        ServerFilter.Streaming -> rows.filter { it.supportsStreaming }
            .ifEmpty { rows }
        ServerFilter.Gaming -> rows.filter { it.supportsGaming }
            .ifEmpty { rows }
    }

    /**
     * The display order: favourites first, then by fresh measured latency, then
     * everything unmeasured, then unreachable endpoints.
     */
    fun sort(rows: List<ServerRow>, now: Long): List<ServerRow> = rows.sortedWith(
        compareBy<ServerRow> { if (it.isFavorite) 0 else 1 }
            .thenBy { row ->
                val latency = row.measurement?.displayLatency(now)
                if (latency == null) 1 else 0
            }
            .thenBy { row -> row.measurement?.displayLatency(now) ?: Long.MAX_VALUE }
            .thenBy { row -> row.profile.displayName.lowercase(Locale.ROOT) },
    )

    /**
     * Locale-neutral search.
     *
     * The query and the haystack are both folded: case removed and diacritics
     * stripped, so "sao paulo" matches "São Paulo" and an Arabic query matches an
     * Arabic country name. Matching is against the user's own label, the country code
     * and the platform's country names in the current locale — never against a
     * guessed list.
     */
    fun search(
        rows: List<ServerRow>,
        query: String,
        countryNames: CountryNameResolver?,
        locale: Locale = Locale.getDefault(),
    ): List<ServerRow> {
        val needle = fold(query)
        if (needle.isEmpty()) return rows
        return rows.filter { row ->
            val haystack = buildList {
                add(row.profile.displayName)
                add(row.profile.host)
                row.profile.countryCode?.let { add(it) }
                row.profile.countryCode?.let { code ->
                    countryNames?.let { resolver ->
                        add(resolver.displayName(code, locale))
                        add(resolver.displayName(code, Locale.ENGLISH))
                        add(resolver.displayName(code, Locale.forLanguageTag("ar")))
                    }
                }
            }
            haystack.any { fold(it).contains(needle) }
        }
    }

    /**
     * Case- and diacritic-insensitive folding. `Normalizer` is used so that "e" and
     * "é" fold together, which is what makes the search work when the user types
     * without accents.
     */
    internal fun fold(value: String): String {
        val decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        return buildString {
            for (char in decomposed) {
                if (Character.getType(char) != Character.NON_SPACING_MARK.toInt()) {
                    append(char.lowercaseChar())
                }
            }
        }.trim()
    }
}
