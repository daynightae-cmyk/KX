package com.kx.vpn.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import com.kx.vpn.domain.LaunchSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Persistence contract for the launch gate.
 *
 * Deliberately read-only in TASK_00. [completeOnboarding] exists so TASK_01 has a
 * single, already-tested write path, and nothing in TASK_00 calls it — the app must
 * not report onboarding as accepted before the flow that accepts it is built.
 */
interface LaunchStateRepository {
    val snapshot: Flow<LaunchSnapshot>
    suspend fun completeOnboarding()
}

private val Context.launchPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kx_launch",
)

internal object LaunchKeys {
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
}

class PreferencesLaunchStateRepository(
    private val dataStore: DataStore<Preferences>,
) : LaunchStateRepository {

    /**
     * A read failure is surfaced, not swallowed: [LaunchSnapshot.storageDegraded]
     * makes the fallback visible to the UI and to tests instead of silently
     * reporting a fresh install.
     *
     * `catch` is placed after `map` so the recovery value is a [LaunchSnapshot] and
     * not an attempt to treat an error as a `Preferences` record.
     */
    override val snapshot: Flow<LaunchSnapshot> = dataStore.data
        .map { preferences ->
            LaunchSnapshot(
                onboardingCompleted = preferences[LaunchKeys.ONBOARDING_COMPLETED] ?: false,
                storageDegraded = false,
            )
        }
        .catch { error ->
            if (error is InterruptedException) throw error
            emit(
                LaunchSnapshot(
                    onboardingCompleted = false,
                    storageDegraded = true,
                ),
            )
        }

    override suspend fun completeOnboarding() {
        dataStore.edit { preferences ->
            preferences[LaunchKeys.ONBOARDING_COMPLETED] = true
        }
    }
}

/**
 * Production wiring. Kept out of the ViewModel so the launch logic stays testable
 * without a Context.
 */
fun createLaunchStateRepository(context: Context): LaunchStateRepository =
    PreferencesLaunchStateRepository(context.applicationContext.launchPreferencesDataStore)
