package com.kx.vpn.domain.security

/**
 * Which security controls this build actually enforces.
 *
 * The point of putting this in the domain rather than in the screen is that a UI cannot
 * then invent a status: the answer is one table, it is testable, and a control that is
 * only written to storage cannot render as though it did something.
 */

/** The controls the security centre exposes. */
enum class SecurityControlKey {
    KILL_SWITCH,
    DNS_LEAK_PROTECTION,
    IPV6_PROTECTION,
    AUTO_RECONNECT,
    TRACKER_BLOCKING,
    ALWAYS_ON_VPN,
}

enum class Enforcement {
    /** The app performs the thing the control says it does. */
    ENFORCED,

    /**
     * The app performs something real, but not what the short label implies, so the
     * control has to state what it actually changes before it is allowed to be on.
     */
    ENFORCED_WITH_DISCLOSURE,

    /**
     * The setting is persisted and nothing reads it. The control is shown **disabled with
     * the reason**, because a switch that moves, saves and changes nothing teaches the
     * user that security settings are decoration.
     */
    NOT_ENFORCED,
}

object SecurityEnforcement {

    /**
     * The single source of truth.
     *
     * Verified against the code, not against the artwork:
     *
     *  * `DNS_LEAK_PROTECTION` and `TRACKER_BLOCKING` are read by
     *    `VpnConfigModifier.applySecuritySettings`, which the session manager calls when
     *    it builds the configuration it hands to the tunnel — so they really do change
     *    what runs. They are `ENFORCED_WITH_DISCLOSURE` because what they change is the
     *    user's own `DNS` line, replaced with a hardcoded public resolver, and because
     *    "tracker blocking" is done by pointing DNS at a filtering resolver rather than by
     *    blocking anything in this app.
     *  * `ALWAYS_ON_VPN` opens the platform's own VPN settings, which is where always-on is
     *    actually granted. The app does not enforce it itself, and does not claim to.
     *  * `KILL_SWITCH`, `IPV6_PROTECTION` and `AUTO_RECONNECT` are written to storage and
     *    never read. Reconnection on a lost network happens through the tunnel state
     *    machine whether the switch is on or off.
     */
    fun of(key: SecurityControlKey): Enforcement = when (key) {
        SecurityControlKey.DNS_LEAK_PROTECTION -> Enforcement.ENFORCED_WITH_DISCLOSURE
        SecurityControlKey.TRACKER_BLOCKING -> Enforcement.ENFORCED_WITH_DISCLOSURE
        SecurityControlKey.ALWAYS_ON_VPN -> Enforcement.ENFORCED
        SecurityControlKey.KILL_SWITCH -> Enforcement.NOT_ENFORCED
        SecurityControlKey.IPV6_PROTECTION -> Enforcement.NOT_ENFORCED
        SecurityControlKey.AUTO_RECONNECT -> Enforcement.NOT_ENFORCED
    }

    /**
     * Whether the control may be operated.
     *
     * A persisted-only switch is not merely labelled: it is not tappable, because tapping
     * it would produce the experience of a setting that works.
     */
    fun isOperable(key: SecurityControlKey): Boolean = of(key) != Enforcement.NOT_ENFORCED

    /** The resolvers the disclosed controls substitute. Kept here so the label can name them. */
    const val DNS_LEAK_RESOLVERS = "1.1.1.1, 1.0.0.1"
    const val TRACKER_RESOLVERS = "94.140.14.14, 94.140.15.15"
}
