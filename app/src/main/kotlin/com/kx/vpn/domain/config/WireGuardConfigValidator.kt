package com.kx.vpn.domain.config

import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Strict, host-side validation of a user-supplied WireGuard configuration.
 *
 * This is the parser boundary `docs/SECURITY_MODEL.md` describes: parse only the
 * explicit supported format, impose size and encoding limits, validate keys strictly,
 * and return safe errors that never echo the offending value. A hostile or truncated
 * file must produce a bounded, local failure — no URL fetching, no script execution,
 * no unbounded allocation.
 *
 * TASK_01 uses this for the first-run "Import Config" entry so the control reports
 * something true about a file. The rich import UI and quarantine workflow belong to
 * TASK_09 and are not duplicated here.
 */
object WireGuardConfigValidator {

    /** A real WireGuard conf is a few hundred bytes. 64 KiB is generous and bounded. */
    const val MAX_CONFIG_BYTES: Int = 64 * 1024
    const val MAX_LINES: Int = 2_048

    /** WireGuard curve keys encode to exactly 44 base64 characters ("…" + "="). */
    private const val BASE64_KEY_LENGTH = 44
    private const val DECODED_KEY_LENGTH = 32

    private const val SECTION_INTERFACE = "Interface"
    private const val SECTION_PEER = "Peer"

    private val KEY_PATTERN = Regex("^[A-Za-z0-9+/]{43}=$")

    /**
     * The security boundary. Anything read from a file, QR payload or clipboard arrives
     * here as bytes, so the size limit is applied before any parsing and malformed
     * UTF-8 is genuinely detectable.
     */
    fun validate(bytes: ByteArray): ConfigValidation {
        if (bytes.isEmpty()) {
            return ConfigValidation.Invalid(listOf(error("document", IssueMessage.Empty)))
        }
        if (bytes.size > MAX_CONFIG_BYTES) {
            return ConfigValidation.Invalid(listOf(error("document", IssueMessage.TooLarge)))
        }
        val text = try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes))
                .toString()
        } catch (expected: CharacterCodingException) {
            return ConfigValidation.Invalid(listOf(error("document", IssueMessage.NotUtf8)))
        }
        return validateText(text)
    }

    /**
     * Convenience for callers that already hold text. The strict encoding check cannot
     * apply here — a `String` cannot contain malformed UTF-8 — so callers handling
     * untrusted bytes must use [validate] instead.
     */
    fun validate(raw: String): ConfigValidation = validateText(raw)

    private fun validateText(raw: String): ConfigValidation {
        if (raw.isBlank()) {
            return ConfigValidation.Invalid(listOf(error("document", IssueMessage.Empty)))
        }

        if (raw.any { it.code < 0x20 && it != '\t' && it != '\n' && it != '\r' }) {
            return ConfigValidation.Invalid(
                listOf(error("document", IssueMessage.ControlCharacters)),
            )
        }

        val lines = raw.split('\n')
        if (lines.size > MAX_LINES) {
            return ConfigValidation.Invalid(listOf(error("document", IssueMessage.TooManyLines)))
        }

        return parse(lines)
    }

    private fun parse(lines: List<String>): ConfigValidation {
        val errors = mutableListOf<ConfigIssue>()
        val warnings = mutableListOf<ConfigIssue>()

        var section: String? = null
        var interfaceSeen = false
        var interfaceCount = 0

        var interfacePrivateKey = false
        val interfaceAddresses = mutableListOf<String>()
        val interfaceDns = mutableListOf<String>()
        var listenPort: Int? = null
        var mtu: Int? = null

        var peerCount = 0
        val peerPublicKeys = mutableListOf<String>()
        val peerEndpoints = mutableListOf<String>()
        var allowedIpCount = 0
        var routesAllTraffic = false
        var keepalive: Int? = null
        var currentPeerHasAllowedIps = false
        var peerBlockHadContent = false

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue

            val sectionMatch = SECTION_REGEX.find(line)
            if (sectionMatch != null) {
                val name = sectionMatch.groupValues[1].trim()
                when (name.lowercase()) {
                    SECTION_INTERFACE.lowercase() -> {
                        section = SECTION_INTERFACE
                        interfaceCount++
                        if (interfaceSeen) {
                            errors += error("section", IssueMessage.MultipleInterfaceSections)
                        }
                        interfaceSeen = true
                    }

                    SECTION_PEER.lowercase() -> {
                        section = SECTION_PEER
                        if (peerBlockHadContent && !currentPeerHasAllowedIps) {
                            errors += error("AllowedIPs", IssueMessage.NoAllowedIps)
                        }
                        peerCount++
                        peerBlockHadContent = false
                        currentPeerHasAllowedIps = false
                    }

                    else -> {
                        section = null
                        errors += error("section", IssueMessage.UnknownSection)
                    }
                }
                continue
            }

            val assignment = line.split('=', limit = 2)
            if (assignment.size != 2) {
                errors += error("line", IssueMessage.MissingKey)
                continue
            }
            val key = assignment[0].trim()
            val value = assignment[1].trim()
            // Canonical WireGuard keys are CamelCase with no separator: AllowedIPs,
            // ListenPort, PersistentKeepalive. Comparing a lowercased raw key against a
            // hyphenated literal matched nothing, so every one of those fields was
            // silently ignored. Normalising both ways accepts the real spelling and the
            // common hyphenated variant without loosening anything else.
            val field = normaliseKey(key)
            // Values are never stored in an issue; only the key name is.
            //
            // This flag tracks "the *current peer block* has at least one assignment",
            // so it must only be set while inside [Peer]. Setting it for every
            // assignment line meant the [Interface] lines above turned it on, and the
            // first [Peer] header then reported the previous (non-existent) peer as
            // missing AllowedIPs — which rejected every valid config.
            if (section == SECTION_PEER) {
                peerBlockHadContent = true
            }

            when (section) {
                SECTION_INTERFACE -> when (field) {
                    "privatekey" -> {
                        if (!isWireGuardKey(value)) {
                            errors += error(key, IssueMessage.InvalidBase64Key)
                        } else {
                            interfacePrivateKey = true
                        }
                    }

                    "address" -> {
                        value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                            .forEach { entry ->
                                if (isCidrOrAddress(entry)) {
                                    interfaceAddresses += entry
                                } else {
                                    errors += error(key, IssueMessage.InvalidIpAddress)
                                }
                            }
                    }

                    "dns" -> value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                        .forEach { entry ->
                            if (isDnsServer(entry)) {
                                interfaceDns += entry
                            } else {
                                errors += error(key, IssueMessage.InvalidDnsServer)
                            }
                        }

                    "listenport" -> listenPort = parsePort(value, key, errors)
                    "mtu" -> mtu = parseMtu(value, key, errors)
                    else -> warnings += warn(key, IssueMessage.UnknownSection)
                }

                SECTION_PEER -> when (field) {
                    "publickey" -> {
                        if (!isWireGuardKey(value)) {
                            errors += error(key, IssueMessage.InvalidBase64Key)
                        } else if (peerPublicKeys.contains(value)) {
                            errors += error(key, IssueMessage.DuplicatePeer)
                        } else {
                            peerPublicKeys += value
                        }
                    }

                    "presharedkey" -> if (value.isNotEmpty() && !isWireGuardKey(value)) {
                        errors += error(key, IssueMessage.InvalidBase64Key)
                    }

                    "endpoint" -> {
                        val endpoint = parseEndpoint(value)
                        if (endpoint == null) {
                            errors += error(key, IssueMessage.InvalidEndpoint)
                        } else {
                            // Only host:port is retained, and a host is not secret;
                            // this is what the UI shows.
                            peerEndpoints += endpoint
                        }
                    }

                    "allowedips" -> {
                        val entries = value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                        if (entries.isEmpty()) {
                            errors += error(key, IssueMessage.NoAllowedIps)
                        }
                        entries.forEach { entry ->
                            if (isCidrOrAddress(entry)) {
                                allowedIpCount++
                                currentPeerHasAllowedIps = true
                                if (entry == "0.0.0.0/0" || entry == "::/0") {
                                    routesAllTraffic = true
                                }
                            } else {
                                errors += error(key, IssueMessage.InvalidCidr)
                            }
                        }
                    }

                    "persistentkeepalive" -> keepalive = parseKeepalive(value, key, errors)
                    else -> warnings += warn(key, IssueMessage.UnknownSection)
                }

                else -> Unit // Content outside any known section was already flagged.
            }
        }

        if (peerBlockHadContent && !currentPeerHasAllowedIps) {
            errors += error("AllowedIPs", IssueMessage.NoAllowedIps)
        }
        if (!interfaceSeen) {
            errors += error("section", IssueMessage.MissingInterfaceSection)
        }
        if (peerCount == 0) {
            errors += error("section", IssueMessage.MissingPeerSection)
        }

        if (errors.isNotEmpty()) return ConfigValidation.Invalid(errors)

        return ConfigValidation.Valid(
            summary = TunnelSummary(
                interfaceHasPrivateKey = interfacePrivateKey,
                interfaceAddressCount = interfaceAddresses.size,
                interfaceDnsServers = interfaceDns,
                interfaceListenPort = listenPort,
                peerCount = peerCount,
                peerPublicKeys = peerPublicKeys,
                peerEndpoints = peerEndpoints,
                allowedIpCount = allowedIpCount,
                routesAllTraffic = routesAllTraffic,
                mtu = mtu,
                persistentKeepaliveSeconds = keepalive,
            ),
            warnings = warnings,
        )
    }

    private fun normaliseKey(key: String): String =
        key.lowercase().filter { it.isLetterOrDigit() }

    // -- Field checks ---------------------------------------------------------------

    private fun isWireGuardKey(value: String): Boolean {
        val candidate = value.trim()
        if (candidate.length != BASE64_KEY_LENGTH) return false
        if (!KEY_PATTERN.matches(candidate)) return false
        return runCatching {
            Base64.getDecoder().decode(candidate).size == DECODED_KEY_LENGTH
        }.getOrDefault(false)
    }

    private fun isCidrOrAddress(entry: String): Boolean {
        val parts = entry.split('/', limit = 2)
        if (parts.size == 2) {
            val prefix = parts[1].toIntOrNull() ?: return false
            val family = literalIpBits(parts[0]) ?: return false
            return prefix in 0..family
        }
        return literalIpBits(entry) != null
    }

    private fun literalIpBits(text: String): Int? = when {
        text.contains(':') -> if (isIpv6Literal(text)) 128 else null
        text.count { it == '.' } == 3 -> if (isIpv4Literal(text)) 32 else null
        else -> null
    }

    private fun isIpv4Literal(text: String): Boolean {
        val octets = text.split('.')
        if (octets.size != 4) return false
        return octets.all { octet ->
            octet.isNotEmpty() && octet.length <= 3 && octet.all(Char::isDigit) &&
                octet.toInt() in 0..255 && !(octet.length > 1 && octet.startsWith('0'))
        }
    }

    private fun isIpv6Literal(text: String): Boolean {
        if (!text.contains(':')) return false
        if (text.startsWith(':') != text.endsWith(':') && text == ":") return false
        if (text.count { it == ':' } > 8) return false

        // "::" may appear at most once and compresses one or more zero groups.
        val doubleColonCount = Regex("::").findAll(text).count()
        if (doubleColonCount > 1) return false

        val groups = text.split(':')
        if (groups.size < 2) return false

        var emptyRuns = 0
        for (group in groups) {
            when {
                group.isEmpty() -> emptyRuns++
                group.length > 4 || !group.all(::isHexDigit) -> return false
            }
        }

        // Without compression an address is exactly 8 groups; the leading/trailing
        // empties produced by "::" are accounted for by emptyRuns.
        if (doubleColonCount == 0) {
            val realGroups = groups.count { it.isNotEmpty() }
            return realGroups == 8
        }
        val realGroups = groups.count { it.isNotEmpty() }
        return realGroups <= 7
    }

    private fun isHexDigit(char: Char): Boolean =
        char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F'

    private fun isDnsServer(entry: String): Boolean =
        literalIpBits(entry) != null || isHostname(entry)

    private fun isHostname(entry: String): Boolean {
        if (entry.length !in 1..253) return false
        if (!entry.all { it.isLetterOrDigit() || it == '.' || it == '-' }) return false
        val labels = entry.split('.')
        if (labels.any { it.isEmpty() || it.length > 63 || it.startsWith('-') || it.endsWith('-') }) {
            return false
        }
        return labels.size >= 2
    }

    private fun parseEndpoint(value: String): String? {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return null
        val separator = trimmed.lastIndexOf(':')
        if (separator <= 0 || separator == trimmed.lastIndex) return null
        val host = trimmed.substring(0, separator)
        val portText = trimmed.substring(separator + 1)
        val port = portText.toIntOrNull() ?: return null
        if (port !in 1..65535) return null
        val hostIsNumericLiteral = literalIpBits(host) != null
        if (!hostIsNumericLiteral && !isHostname(host)) return null
        return "$host:$port"
    }

    private fun parsePort(value: String, key: String, errors: MutableList<ConfigIssue>): Int? {
        val port = value.toIntOrNull()
        if (port == null || port !in 1..65535) {
            errors += error(key, IssueMessage.InvalidPort)
            return null
        }
        return port
    }

    private fun parseMtu(value: String, key: String, errors: MutableList<ConfigIssue>): Int? {
        val mtu = value.toIntOrNull()
        if (mtu == null || mtu !in 576..65535) {
            errors += error(key, IssueMessage.InvalidMtu)
            return null
        }
        return mtu
    }

    private fun parseKeepalive(
        value: String,
        key: String,
        errors: MutableList<ConfigIssue>,
    ): Int? {
        val seconds = value.toIntOrNull()
        if (seconds == null || seconds !in 0..65535) {
            errors += error(key, IssueMessage.InvalidKeepalive)
            return null
        }
        return seconds
    }

    private fun error(field: String, message: IssueMessage) =
        ConfigIssue(ConfigIssue.Severity.Error, field, message)

    private fun warn(field: String, message: IssueMessage) =
        ConfigIssue(ConfigIssue.Severity.Warning, field, message)

    private val SECTION_REGEX = Regex("^\\[\\s*(\\w+)\\s*]\\s*$")
}
