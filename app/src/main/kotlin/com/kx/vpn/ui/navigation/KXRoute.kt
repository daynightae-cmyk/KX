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

    /** Reached once onboarding is accepted. Still a placeholder until TASK_02. */
    const val Home: String = "home"

    const val ArgStorageDegraded: String = "storageDegraded"
}
