package com.admoseley.quietforaminute.data.datastore

import android.content.Context
import android.media.AudioManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quiet_prefs")

/**
 * A mute countdown's restore target, persisted to disk so it survives the process (and the
 * device) being killed. [manualRestoreVolume] mirrors [com.admoseley.quietforaminute.scheduler.EXTRA_RESTORE_VOLUME]
 * — -1 means "use the default restore volume at restore time", not a literal index.
 */
data class PendingRestore(
    val endEpochMillis: Long,
    val streamType: Int,
    val manualRestoreVolume: Int
)

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
        val KEY_PENDING_RESTORE_END_EPOCH = longPreferencesKey("pending_restore_end_epoch")
        val KEY_PENDING_RESTORE_STREAM_TYPE = intPreferencesKey("pending_restore_stream_type")
        val KEY_PENDING_RESTORE_VOLUME = intPreferencesKey("pending_restore_volume")
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

    /**
     * Non-null exactly while a mute countdown (manual or scheduled) is running — the safety net
     * that lets [com.admoseley.quietforaminute.receiver.BackupRestoreReceiver] and
     * [com.admoseley.quietforaminute.receiver.BootReceiver] restore volume even if
     * MuteTimerService's process (and, for BootReceiver, the device itself) was killed mid-timer.
     */
    val pendingRestore: Flow<PendingRestore?> = context.dataStore.data.map { prefs ->
        val endEpoch = prefs[KEY_PENDING_RESTORE_END_EPOCH] ?: return@map null
        val streamType = prefs[KEY_PENDING_RESTORE_STREAM_TYPE] ?: return@map null
        val manualRestoreVolume = prefs[KEY_PENDING_RESTORE_VOLUME] ?: return@map null
        PendingRestore(endEpoch, streamType, manualRestoreVolume)
    }

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

    suspend fun savePendingRestore(endEpochMillis: Long, streamType: Int, manualRestoreVolume: Int) {
        context.dataStore.edit {
            it[KEY_PENDING_RESTORE_END_EPOCH] = endEpochMillis
            it[KEY_PENDING_RESTORE_STREAM_TYPE] = streamType
            it[KEY_PENDING_RESTORE_VOLUME] = manualRestoreVolume
        }
    }

    suspend fun clearPendingRestore() {
        context.dataStore.edit {
            it.remove(KEY_PENDING_RESTORE_END_EPOCH)
            it.remove(KEY_PENDING_RESTORE_STREAM_TYPE)
            it.remove(KEY_PENDING_RESTORE_VOLUME)
        }
    }
}
