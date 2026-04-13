package com.admoseley.quietforaminute.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

/**
 * Detects when the user sets volume to zero (mute) on **music** or **ring** streams.
 * [com.admoseley.quietforaminute.service.MuteTimerService] only adjusts
 * [android.media.AudioManager.STREAM_MUSIC] when restoring or for scheduled mutes; ring volume
 * is intentionally unchanged on restore (user-controlled).
 *
 * Must be registered dynamically — [ACTION_VOLUME_CHANGED] cannot be received by statically
 * declared receivers on API 26+.
 */
class VolumeReceiver(
    private val onVolumeMuted: () -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_VOLUME_CHANGED) return

        val streamType = intent.getIntExtra(EXTRA_STREAM_TYPE, -1)
        if (streamType != AudioManager.STREAM_MUSIC &&
            streamType != AudioManager.STREAM_RING
        ) return

        val newVolume = intent.getIntExtra(EXTRA_VOLUME_VALUE, -1)

        // Trigger whenever volume becomes zero
        if (newVolume == 0) {
            onVolumeMuted()
        }
    }

    companion object {
        const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val EXTRA_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        const val EXTRA_VOLUME_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
        const val EXTRA_PREV_VOLUME_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE"
    }
}
