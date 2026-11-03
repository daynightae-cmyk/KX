package com.kx.vpn.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kx.vpn.domain.LaunchDestination
import com.kx.vpn.domain.LaunchSnapshot
import com.kx.vpn.domain.decideLaunchDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

/**
 * Persistence behaviour of the launch gate, exercised against a real DataStore on a
 * real file — not a mocked map. That distinction matters: the failure paths below
 * only exist because the on-disk format can genuinely be unreadable.
 */
@RunWith(RobolectricTestRunner::class)
class PreferencesLaunchStateRepositoryTest {

    private val ids = AtomicLong(0)
    private val files = mutableListOf<File>()
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob())
    }

    @After
    fun tearDown() {
        scope.cancel()
        files.forEach { it.parentFile?.deleteRecursively() }
        files.clear()
    }

    private fun newStore() = PreferenceDataStoreFactory.create(scope = scope, produceFile = { tempDir() })

    private fun tempDir(): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "kx-datastore-${ids.incrementAndGet()}")
        dir.mkdirs()
        files.add(dir)
        return File(dir, "kx_launch.preferences_pb")
    }

    @Test
    fun aFreshInstallReportsOnboardingIncomplete() = runTest {
        val repository = PreferencesLaunchStateRepository(newStore())
        val snapshot = repository.snapshot.first()

        assertEquals(LaunchSnapshot(onboardingCompleted = false, storageDegraded = false), snapshot)
        assertEquals(LaunchDestination.Welcome, decideLaunchDestination(snapshot))
    }

    @Test
    fun completingOnboardingIsPersistedAndRoutedToHome() = runTest {
        val store = newStore()
        val repository = PreferencesLaunchStateRepository(store)

        repository.completeOnboarding()
        val snapshot = repository.snapshot.first()

        assertTrue("write must be readable back", snapshot.onboardingCompleted)
        assertEquals(false, snapshot.storageDegraded)
        assertEquals(LaunchDestination.Home, decideLaunchDestination(snapshot))
    }

    /**
     * Startup-failure fallback. A truncated/garbage preferences file makes DataStore
     * raise IOException; the app must still produce a usable snapshot and must not
     * claim the state is known.
     */
    @Test
    fun anUnreadableStoreDegradesInsteadOfThrowing() = runTest {
        val file = tempDir()
        file.parentFile!!.mkdirs()
        file.writeBytes(ByteArray(64) { 0xFF.toByte() })

        val repository = PreferencesLaunchStateRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
        )

        val snapshot = repository.snapshot.first()
        assertEquals(LaunchSnapshot(onboardingCompleted = false, storageDegraded = true), snapshot)
        assertEquals(
            "a degraded store must never route straight to Home",
            LaunchDestination.Welcome,
            decideLaunchDestination(snapshot),
        )
    }

    @Test
    fun aDirectoryThatCannotBeCreatedAlsoDegrades() = runTest {
        val blocked = File(tempDir(), "blocked")
        blocked.parentFile!!.mkdirs()
        // A file where a directory is required: DataStore cannot open this.
        blocked.writeText("not a directory")

        val repository = PreferencesLaunchStateRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { blocked }),
        )

        val snapshot = repository.snapshot.first()
        assertTrue("expected degraded read, got $snapshot", snapshot.storageDegraded)
    }

    /**
     * Guards the security contract: the launch store holds a boolean and nothing else.
     * If a future task quietly starts persisting tokens here, this test is the tripwire.
     */
    @Test
    fun storedFileContainsNoSecretMaterial() = runTest {
        val file = tempDir()
        val repository = PreferencesLaunchStateRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
        )
        repository.completeOnboarding()
        repository.snapshot.first()

        val raw = try {
            file.readBytes().toString(Charsets.ISO_8859_1)
        } catch (expected: IOException) {
            ""
        }
        listOf("private_key", "preshared", "Bearer", "password", "wireguard").forEach { needle ->
            assertTrue(
                "launch store must not contain '$needle'",
                !raw.contains(needle, ignoreCase = true),
            )
        }
    }
}
