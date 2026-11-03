package com.kx.vpn.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A minimal in-memory `DataStore`, shared by the repository tests in this package.
 *
 * AndroidX DataStore cannot overwrite an existing preferences file on Windows: it moves a
 * temporary file with `ATOMIC_MOVE` and **without** `REPLACE_EXISTING`, which POSIX allows
 * and Windows does not. Any test that writes more than once therefore fails on a Windows
 * development machine with "Unable to rename …" while passing on Linux, which is where CI
 * and the app itself run. Behavioural tests use this store; the real-disk path is covered
 * separately by a single-write test.
 */
class InMemoryPreferencesDataStore : DataStore<Preferences> {

    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (Preferences) -> Preferences,
    ): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
