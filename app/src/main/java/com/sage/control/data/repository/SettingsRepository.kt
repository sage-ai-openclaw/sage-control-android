package com.sage.control.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val COMPACT_MODE = booleanPreferencesKey("compact_mode")
        val ACTIVE_THEME = stringPreferencesKey("active_theme")
    }

    val serverUrl: Flow<String> = dataStore.data.map { it[SERVER_URL] ?: "" }
    val authToken: Flow<String> = dataStore.data.map { it[AUTH_TOKEN] ?: "" }
    val darkMode: Flow<Boolean> = dataStore.data.map { it[DARK_MODE] ?: true }
    val fontSize: Flow<Float> = dataStore.data.map { it[FONT_SIZE] ?: 16f }
    val soundEnabled: Flow<Boolean> = dataStore.data.map { it[SOUND_ENABLED] ?: true }
    val compactMode: Flow<Boolean> = dataStore.data.map { it[COMPACT_MODE] ?: false }
    val activeTheme: Flow<String> = dataStore.data.map { it[ACTIVE_THEME] ?: "default" }

    val isLoggedIn: Flow<Boolean> = dataStore.data.map { 
        it[SERVER_URL]?.isNotEmpty() == true && it[AUTH_TOKEN]?.isNotEmpty() == true 
    }

    suspend fun saveCredentials(serverUrl: String, authToken: String) {
        dataStore.edit { prefs ->
            prefs[SERVER_URL] = serverUrl
            prefs[AUTH_TOKEN] = authToken
        }
    }

    suspend fun clearCredentials() {
        dataStore.edit { prefs ->
            prefs.remove(SERVER_URL)
            prefs.remove(AUTH_TOKEN)
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setFontSize(size: Float) {
        dataStore.edit { it[FONT_SIZE] = size }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun setCompactMode(enabled: Boolean) {
        dataStore.edit { it[COMPACT_MODE] = enabled }
    }

    suspend fun setActiveTheme(theme: String) {
        dataStore.edit { it[ACTIVE_THEME] = theme }
    }
}