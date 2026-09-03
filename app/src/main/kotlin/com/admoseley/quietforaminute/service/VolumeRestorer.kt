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
    private val prefsRepository: PreferencesRepository,
    private val dndController: DndController
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    /**
     * Undoes everything a mute timer changed.
     *
     * @param manualRestoreVolume the per-mute override, or -1 to use the configured default
     *                            (scaled into [streamType]'s units — see [scaleVolumeUnits]).
     * @param clearDnd            switch Do Not Disturb back off. Pass the flag the timer recorded
     *                            when it started, never a fresh "is DND on?" check: DND the user
     *                            turned on themselves is not ours to clear (see [DndController]).
     * @param restoreMedia        also bring STREAM_MUSIC back. Only true when the timer muted media
     *                            *in addition* to [streamType] — i.e. DND was on and the trigger
     *                            was the ring stream.
     */
    suspend fun restore(
        streamType: Int,
        manualRestoreVolume: Int,
        clearDnd: Boolean = false,
        restoreMedia: Boolean = false
    ) {
        // DND comes off FIRST, before any volume is touched. While a DND policy is active the
        // system can refuse to raise the ringer out of silent (the SecurityException caught in
        // setStream below), so clearing it afterwards would mean the restore it was blocking had
        // already silently failed.
        if (clearDnd) dndController.disable()

        setStream(streamType, manualRestoreVolume)

        // Media was silenced by us, not by the user, so it gets the configured default rather than
        // the popup's per-mute slider value — that slider describes the stream they actually muted.
        if (restoreMedia && streamType != AudioManager.STREAM_MUSIC) {
            setStream(AudioManager.STREAM_MUSIC, manualRestoreVolume = -1)
        }
    }

    private suspend fun setStream(streamType: Int, manualRestoreVolume: Int) {
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
