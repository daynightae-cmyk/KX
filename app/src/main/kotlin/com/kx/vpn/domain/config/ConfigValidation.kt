package com.kx.vpn.domain.config

/**
 * Result of validating a user-supplied WireGuard configuration.
 *
 * The summary deliberately carries no secret material. `docs/SECURITY_MODEL.md` forbids
 * private keys, preshared keys or live config URIs from reaching logs, analytics,
 * evidence or screenshots, so the only shape that can be handed to a UI or a test
 * report is this redacted one.
 */
data class TunnelSummary(
    val interfaceHasPrivateKey: Boolean,
    val interfaceAddressCount: Int,
    val interfaceDnsServers: List<String>,
    val interfaceListenPort: Int?,
    val peerCount: Int,
    /** Peer public keys are safe to show: they are public by construction. */
    val peerPublicKeys: List<String>,
    val peerEndpoints: List<String>,
    val allowedIpCount: Int,
    val routesAllTraffic: Boolean,
    val mtu: Int?,
    val persistentKeepaliveSeconds: Int?,
)

/**
 * A single finding. `field` names the configuration key, never its value, because the
 * value of `PrivateKey` is exactly what must not be echoed back.
 */
data class ConfigIssue(
    val severity: Severity,
    val field: String,
    val messageKey: IssueMessage,
) {
    enum class Severity { Error, Warning }
}

/**
 * Message identifiers rather than formatted strings, so the UI can localise them and a
 * log can never accidentally print a config value that was interpolated into a message.
 */
enum class IssueMessage {
    Empty,
    TooLarge,
    TooManyLines,
    NotUtf8,
    ControlCharacters,
    MissingInterfaceSection,
    MultipleInterfaceSections,
    MissingPeerSection,
    UnknownSection,
    MissingKey,
    InvalidBase64Key,
    WrongKeyLength,
    InvalidPort,
    InvalidIpAddress,
    InvalidCidr,
    InvalidDnsServer,
    InvalidEndpoint,
    DuplicatePeer,
    NoAllowedIps,
    InvalidKeepalive,
    InvalidMtu,
}

sealed interface ConfigValidation {
    data class Valid(val summary: TunnelSummary, val warnings: List<ConfigIssue>) :
        ConfigValidation

    data class Invalid(val errors: List<ConfigIssue>) : ConfigValidation
}
