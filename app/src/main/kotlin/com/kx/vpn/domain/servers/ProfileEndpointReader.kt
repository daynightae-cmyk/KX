package com.kx.vpn.domain.servers

import java.nio.charset.StandardCharsets

/**
 * Extracts the endpoint of a parsed configuration.
 *
 * The catalogue is built from the user's own configurations, so it needs to know
 * which host and port each one actually points at. That information comes from the
 * configuration the user supplied — it is read, never guessed, and never taken from
 * a bundled list.
 */
object ProfileEndpointReader {

    /**
     * The peer's identity as the configuration declares it.
     *
     * `publicKey` is the peer key, not the user's private key, and is only ever used to
     * derive a stable profile id — never displayed and never sent anywhere.
     */
    data class Endpoint(val host: String, val port: Int, val publicKey: String?)

    /**
     * Returns the first peer endpoint in the configuration.
     *
     * A configuration can declare several peers; the catalogue lists the
     * configuration once, with the first peer's endpoint, because the app drives one
     * tunnel at a time.
     */
    fun read(configText: String): Endpoint? {
        var inPeer = false
        var host: String? = null
        var port: Int? = null
        var key: String? = null
        for (rawLine in configText.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue
            if (line.startsWith("[")) {
                if (inPeer && host != null) break
                inPeer = line.equals("[Peer]", ignoreCase = true)
                continue
            }
            if (!inPeer) continue
            val assignment = line.split('=', limit = 2)
            if (assignment.size != 2) continue
            val name = assignment[0].trim()
            val value = assignment[1].trim()
            when {
                name.equals("Endpoint", ignoreCase = true) -> {
                    val separator = value.lastIndexOf(':')
                    if (separator <= 0) return null
                    val candidateHost = value.substring(0, separator).trim()
                    val candidatePort = value.substring(separator + 1).trim().toIntOrNull() ?: return null
                    if (!EndpointValidator.isValidHost(candidateHost)) return null
                    if (candidatePort !in 1..65535) return null
                    host = candidateHost
                    port = candidatePort
                }

                name.equals("PublicKey", ignoreCase = true) -> key = value
            }
        }
        val resolvedHost = host ?: return null
        return Endpoint(
            host = resolvedHost,
            port = port ?: return null,
            publicKey = key?.takeIf { it.isNotBlank() },
        )
    }
}

/**
 * A stable id for the profile a configuration belongs to.
 *
 * Derived from the peer endpoint and its public key, so importing the same file twice
 * updates one entry instead of filling the list with duplicates, while two accounts on
 * the same host stay distinct. Nothing user-visible is hashed and no secret is used: the
 * peer key is public by design.
 */
object EndpointIdentity {

    /** Matches the vault's accepted id pattern. */
    fun profileId(host: String, port: Int, publicKey: String?): String {
        val material = "$host:$port|${publicKey.orEmpty()}".toByteArray(StandardCharsets.UTF_8)
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(material)
        return "p" + digest.take(12).joinToString("") { "%02x".format(it) }
    }
}
