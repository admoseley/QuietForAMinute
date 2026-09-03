package com.admoseley.quietforaminute.data.datastore

import android.content.Context
import android.media.AudioManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quiet_prefs")

@Singleton
class PreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    companion object {
        val KEY_DEFAULT_VOLUME = intPreferencesKey("default_volume")
        val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val KEY_CHIME_ON_MUTE = booleanPreferencesKey("chime_on_mute")
        val KEY_CHIME_ON_RESTORE = booleanPreferencesKey("chime_on_restore")
        val KEY_MUTE_CHIME_URI = stringPreferencesKey("mute_chime_uri")
        val KEY_RESTORE_CHIME_URI = stringPreferencesKey("restore_chime_uri")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }

    /**
     * Stored in STREAM_MUSIC index units (0..getStreamMaxVolume(STREAM_MUSIC)), NOT a percentage.
     * Consumers restoring a different stream must scale it (see MuteTimerService). The value is
     * also clamped on every read site because the device max can differ after a restore/migration.
     */
    val defaultVolume: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[KEY_DEFAULT_VOLUME] ?: 7 }

    val overlayEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_OVERLAY_ENABLED] ?: true }

    val chimeOnMute: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_CHIME_ON_MUTE] ?: true }

    val chimeOnRestore: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_CHIME_ON_RESTORE] ?: true }

    val muteChimeUri: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[KEY_MUTE_CHIME_URI] }

    val restoreChimeUri: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[KEY_RESTORE_CHIME_URI] }

    val themeMode: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_THEME_MODE] ?: "SYSTEM" }

    suspend fun setDefaultVolume(volume: Int) {
        val max = context.getSystemService(AudioManager::class.java)
            .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        context.dataStore.edit { it[KEY_DEFAULT_VOLUME] = volume.coerceIn(0, max) }
    }

    suspend fun setOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_OVERLAY_ENABLED] = enabled }
    }

    suspend fun setChimeOnMute(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CHIME_ON_MUTE] = enabled }
    }

    suspend fun setChimeOnRestore(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CHIME_ON_RESTORE] = enabled }
    }

    suspend fun setMuteChimeUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(KEY_MUTE_CHIME_URI)
            else prefs[KEY_MUTE_CHIME_URI] = uri
        }
    }

    suspend fun setRestoreChimeUri(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(KEY_RESTORE_CHIME_URI)
            else prefs[KEY_RESTORE_CHIME_URI] = uri
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }
}
