package com.kx.vpn.domain.servers

/**
 * One endpoint the user actually owns.
 *
 * The catalogue is built **only** from configurations the user supplied. There is no
 * bundled fleet and no scraped public list: `docs/tasks/TASK_03` forbids embedding
 * untrusted public server lists, and a bundled list of "free exits" would also be a
 * promise the product cannot keep.
 *
 * `countryCode` is user-supplied metadata (an ISO 3166-1 alpha-2 code). It is optional
 * because a configuration may simply not declare a country, and inventing one would be
 * fabrication. The flag is derived from it at render time by the platform's own emoji
 * support, so no third-party flag artwork is bundled.
 */
data class EndpointProfile(
    /** Stable identifier, derived from the profile id. */
    val id: String,
    /** The label the user gave this endpoint. Never a country the app guessed. */
    val displayName: String,
    /** Optional ISO 3166-1 alpha-2 country code, or null when the user did not say. */
    val countryCode: String?,
    /** Host of the WireGuard peer endpoint. Validated before it is ever used. */
    val host: String,
    val port: Int,
    /** The id of the encrypted profile that holds the real configuration. */
    val profileId: String,
) {
    /** True when the user supplied country metadata this app may show. */
    val hasCountry: Boolean get() = countryCode != null

    /** The flag glyph, derived from the country code, or null when there is none. */
    val flag: String?
        get() = countryCode
            ?.takeIf { it.length == 2 && it.all { c -> c.isLetter() } }
            ?.uppercase()
            ?.let { code ->
                // Regional indicator symbols: 'A' is 0x1F1E6.
                code.map { Character.toChars(0x1F1E6 + (it - 'A')) }
                    .joinToString("") { String(it) }
            }
}

/** Why an endpoint was rejected. Never carries the offending value. */
enum class EndpointRejection {
    EmptyLabel,
    LabelTooLong,
    InvalidHost,
    InvalidPort,
    InvalidCountryCode,
    DuplicateId,
}

/**
 * Validation for endpoint metadata.
 *
 * The host is the one field that ends up in a socket address, so it is validated
 * strictly: only a DNS name or a literal IP, with no characters that could inject
 * anything into an address string or a URL. The checks are deliberately conservative —
 * an endpoint that fails them is refused rather than sanitised, because a sanitised
 * host could silently point somewhere the user did not choose.
 */
object EndpointValidator {

    const val MAX_LABEL_LENGTH = 64
    const val MAX_HOST_LENGTH = 253

    private val HOSTNAME_PATTERN = Regex("^[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$")
    private val COUNTRY_PATTERN = Regex("^[A-Za-z]{2}$")
    private val PROFILE_ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")

    fun validate(profile: EndpointProfile): List<EndpointRejection> {
        val problems = mutableListOf<EndpointRejection>()

        if (profile.displayName.isBlank()) problems += EndpointRejection.EmptyLabel
        if (profile.displayName.length > MAX_LABEL_LENGTH) {
            problems += EndpointRejection.LabelTooLong
        }
        if (!isValidHost(profile.host)) problems += EndpointRejection.InvalidHost
        if (profile.port !in 1..65535) problems += EndpointRejection.InvalidPort
        if (profile.countryCode != null && !COUNTRY_PATTERN.matches(profile.countryCode)) {
            problems += EndpointRejection.InvalidCountryCode
        }
        if (!PROFILE_ID_PATTERN.matches(profile.id)) problems += EndpointRejection.DuplicateId
        if (!PROFILE_ID_PATTERN.matches(profile.profileId)) {
            problems += EndpointRejection.DuplicateId
        }
        return problems
    }

    fun isValidHost(host: String): Boolean {
        val candidate = host.trim()
        if (candidate.isEmpty() || candidate.length > MAX_HOST_LENGTH) return false
        // A literal IPv4 address.
        if (candidate.all { it.isDigit() || it == '.' }) {
            val octets = candidate.split('.')
            if (octets.size != 4) return false
            return octets.all { octet ->
                octet.isNotEmpty() && octet.length <= 3 && octet.all(Char::isDigit) &&
                    octet.toInt() in 0..255 && !(octet.length > 1 && octet.startsWith('0'))
            }
        }
        // A literal IPv6 address: hex groups with at most one "::".
        if (candidate.contains(':')) {
            if (candidate.count { it == ':' } > 8) return false
            if (Regex("::").findAll(candidate).count() > 1) return false
            val groups = candidate.split(':')
            if (groups.size < 2) return false
            var emptyRuns = 0
            for (group in groups) {
                when {
                    group.isEmpty() -> emptyRuns++
                    group.length > 4 || !group.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' } ->
                        return false
                }
            }
            return emptyRuns <= 2
        }
        // A DNS name.
        if (!HOSTNAME_PATTERN.matches(candidate)) return false
        val labels = candidate.split('.')
        if (labels.any { it.length > 63 }) return false
        return labels.last().length >= 2 || labels.size == 1
    }
}
