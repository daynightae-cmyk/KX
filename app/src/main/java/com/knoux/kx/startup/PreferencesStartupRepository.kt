package com.knoux.kx.startup

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.startupPreferences by preferencesDataStore(name = "startup")

class PreferencesStartupRepository(context: Context) : StartupRepository {
    private val store = context.applicationContext.startupPreferences

    override suspend fun isOnboardingAccepted(): Boolean =
        store.data.first()[ONBOARDING_ACCEPTED] ?: false

    private companion object {
        val ONBOARDING_ACCEPTED = booleanPreferencesKey("onboarding_accepted_v1")
    }
}
