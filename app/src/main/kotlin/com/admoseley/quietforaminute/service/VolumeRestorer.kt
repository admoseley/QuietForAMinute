package com.admoseley.quietforaminute.service

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.admoseley.quietforaminute.data.datastore.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * The actual "set the stream back" logic, pulled out of [MuteTimerService] so
 * [com.admoseley.quietforaminute.receiver.BackupRestoreReceiver] and
 * [com.admoseley.quietforaminute.receiver.BootReceiver] can perform the same restore without a
 * foreground service — both exist specifically for when that service's process is gone.
 *
 * Deliberately does not play a chime or show a Toast: those need [android.media.MediaPlayer]
 * prepare/threading and a live Activity-ish UI, both of which add failure modes to a path whose
 * entire job is "work even when everything else already failed". [MuteTimerService] layers those
 * on top of [restore] itself on its own (primary, non-fallback) path.
 */
@Singleton
class VolumeRestorer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prefsRepository: PreferencesRepository
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    /**
     * @param manualRestoreVolume the per-mute override, or -1 to use the configured default
     *                            (scaled into [streamType]'s units — see [scaleVolumeUnits]).
     */
    suspend fun restore(streamType: Int, manualRestoreVolume: Int) {
        val targetMax = audioManager.getStreamMaxVolume(streamType)
        val volume = if (manualRestoreVolume >= 0) {
            manualRestoreVolume
        } else {
            val default = prefsRepository.defaultVolume.first()
            val musicMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            scaleVolumeUnits(default, musicMax, targetMax)
        }.coerceIn(0, targetMax)

        // Open the window BEFORE touching the volume so the resulting broadcasts land inside it.
        OverlayServiceBridge.markProgrammaticChange()
        try {
            // If the user muted with the volume-panel icon the stream carries a mute *flag* and
            // the index alone will not bring sound back; clear the flag first (no-op otherwise).
            audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.setStreamVolume(streamType, volume, 0)
            Log.d(TAG, "Stream $streamType restored to $volume/$targetMax")
        } catch (e: SecurityException) {
            // Raising the ringer out of silent can be blocked by Do Not Disturb policy.
            Log.w(TAG, "Unable to restore stream $streamType", e)
        }
    }

    private companion object {
        const val TAG = "VolumeRestorer"
    }
}

/** `units` scaled from a `fromMax`-unit stream into a `toMax`-unit stream. Pure, for testing. */
fun scaleVolumeUnits(units: Int, fromMax: Int, toMax: Int): Int {
    if (fromMax <= 0) return 0
    return (units.toFloat() / fromMax * toMax).roundToInt()
}
