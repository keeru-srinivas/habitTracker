package com.example.cozytrack.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val userIdKey = stringPreferencesKey("user_id")

    val accessToken: Flow<String?> = appContext.authDataStore.data.map { preferences ->
        preferences[accessTokenKey]
    }

    val userId: Flow<String?> = appContext.authDataStore.data.map { preferences ->
        preferences[userIdKey]
    }

    suspend fun saveSession(accessToken: String, userId: String) {
        appContext.authDataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[userIdKey] = userId
        }
    }

    suspend fun clearSession() {
        appContext.authDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
