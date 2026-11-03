package com.kx.vpn.ui.navigation

/**
 * Route table for the app graph.
 *
 * Naming is stable across tasks: later slices add destinations here rather than
 * inventing parallel route strings.
 */
object KXRoute {
    const val Launch: String = "launch"

    /** First-run welcome. Reached from the launch gate when onboarding is not done. */
    const val Welcome: String = "welcome"

    /** Disclosure step; completing it is what writes the onboarding flag. */
    const val Onboarding: String = "onboarding"

    /** First-run configuration inspection. Local only, nothing persisted. */
    const val Import: String = "import"

    /** Reached once onboarding is accepted. */
    const val Home: String = "home"

    /** The user's own endpoint catalogue. */
    const val Servers: String = "servers"

    /** Security center with leak protection and kill switch toggles. */
    const val Security: String = "security"

    /** Per-app routing rules. */
    const val SplitTunneling: String = "split_tunneling"

    /** Protocols, MTU, DNS mode and keepalive controls. */
    const val AdvancedNetwork: String = "advanced_network"

    /** Historical data usage, ping trends and speed telemetry. */
    const val Statistics: String = "statistics"

    /** QR scanner, clipboard and manual setup entry points. */
    const val ImportConfig: String = "import_config"

    /** Profile and user preferences. */
    const val ProfileSettings: String = "profile_settings"

    const val ArgStorageDegraded: String = "storageDegraded"
}
