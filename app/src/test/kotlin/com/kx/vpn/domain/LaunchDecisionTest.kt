package com.kx.vpn.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Routing rules for the launch gate. Pure JVM, no Android.
 */
class LaunchDecisionTest {

    @Test
    fun freshInstallGoesToWelcome() {
        assertEquals(
            LaunchDestination.Welcome,
            decideLaunchDestination(LaunchSnapshot(onboardingCompleted = false)),
        )
    }

    @Test
    fun completedOnboardingGoesToHome() {
        assertEquals(
            LaunchDestination.Home,
            decideLaunchDestination(LaunchSnapshot(onboardingCompleted = true)),
        )
    }

    /**
     * The safety property: a store we could not read must never be interpreted as
     * "already set up". Failing towards Welcome is the only non-destructive choice.
     */
    @Test
    fun degradedStorageFailsTowardWelcomeEvenWhenFlagSaysComplete() {
        assertEquals(
            LaunchDestination.Welcome,
            decideLaunchDestination(
                LaunchSnapshot(onboardingCompleted = true, storageDegraded = true),
            ),
        )
    }

    @Test
    fun degradedStorageFailsTowardWelcome() {
        assertEquals(
            LaunchDestination.Welcome,
            decideLaunchDestination(
                LaunchSnapshot(onboardingCompleted = false, storageDegraded = true),
            ),
        )
    }

    @Test
    fun defaultSnapshotIsAFreshInstall() {
        val snapshot = LaunchSnapshot()
        assertEquals(false, snapshot.onboardingCompleted)
        assertEquals(false, snapshot.storageDegraded)
        assertEquals(LaunchDestination.Welcome, decideLaunchDestination(snapshot))
    }
}
