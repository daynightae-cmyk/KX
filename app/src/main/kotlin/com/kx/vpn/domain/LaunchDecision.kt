package com.kx.vpn.domain

/**
 * What the app knows about itself at launch time.
 *
 * `onboardingCompleted` is read, never written, by TASK_00: the acceptance flow that
 * flips it belongs to TASK_01. Keeping the field here means the routing decision is
 * already real and tested, rather than a stub that TASK_01 has to invent.
 *
 * `storageDegraded` is the startup-failure fallback flag. When the persisted store
 * cannot be read the app still launches, but it must not pretend the state is known.
 */
data class LaunchSnapshot(
    val onboardingCompleted: Boolean = false,
    val storageDegraded: Boolean = false,
)

/** Destinations the launch gate can route to. */
enum class LaunchDestination {
    Welcome,
    Home,
}

/**
 * Pure routing rule. No Android types, so it is covered by ordinary JVM tests.
 *
 * A degraded store resolves to [LaunchDestination.Welcome]: if the app cannot tell
 * whether setup happened, asking the user to confirm setup is the safe answer, and
 * it is never treated as an implicit "already onboarded".
 */
fun decideLaunchDestination(snapshot: LaunchSnapshot): LaunchDestination =
    if (!snapshot.storageDegraded && snapshot.onboardingCompleted) {
        LaunchDestination.Home
    } else {
        LaunchDestination.Welcome
    }
