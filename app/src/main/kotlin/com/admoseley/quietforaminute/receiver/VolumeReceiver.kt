package com.admoseley.quietforaminute.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log

/**
 * Detects the moment the user mutes the **music** or **ring** stream.
 *
 * Two system broadcasts are watched:
 *  - [ACTION_VOLUME_CHANGED]      — the stream index changed (volume keys, volume-panel slider).
 *  - [ACTION_STREAM_MUTE_CHANGED] — the stream's mute flag flipped (the bell / speaker icon in
 *                                    the volume panel, ringer → vibrate/silent). On modern
 *                                    Android this does NOT always produce a VOLUME_CHANGED
 *                                    broadcast, so listening for only the first action misses
 *                                    the "tap the icon" gesture entirely.
 *
 * Only the *transition into* zero fires the callback. A broadcast that reports 0 with a
 * previous value of 0 is ignored, so repeated or aliased broadcasts cannot double-trigger.
 *
 * This class is deliberately stateless. It has no memory of the app's own programmatic volume
 * changes; filtering those out is [com.admoseley.quietforaminute.service.OverlayServiceBridge]'s
 * job (a short time window that expires on its own). The previous design kept a counter of
 * "broadcasts to swallow" here, which could get stuck and silently eat the next real mute.
 *
 * Must be registered dynamically — these actions cannot be received by manifest-declared
 * receivers on API 26+.
 */
class VolumeReceiver(
    private val onStreamMuted: (streamType: Int) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val streamType = intent.getIntExtra(EXTRA_STREAM_TYPE, -1)
        if (streamType !in WATCHED_STREAMS) return

        when (intent.action) {
            ACTION_VOLUME_CHANGED -> {
                val newVolume = intent.getIntExtra(EXTRA_VOLUME_VALUE, -1)
                val prevVolume = intent.getIntExtra(EXTRA_PREV_VOLUME_VALUE, -1)
                Log.d(TAG, "VOLUME_CHANGED stream=$streamType $prevVolume -> $newVolume")
                // Fire only on the edge (non-zero -> zero). prev == -1 means the extra was
                // absent; treat that as a transition so a real mute is never missed.
                if (newVolume == 0 && prevVolume != 0) onStreamMuted(streamType)
            }

            ACTION_STREAM_MUTE_CHANGED -> {
                val muted = intent.getBooleanExtra(EXTRA_STREAM_MUTED, false)
                Log.d(TAG, "STREAM_MUTE_CHANGED stream=$streamType muted=$muted")
                if (muted) onStreamMuted(streamType)
            }
        }
    }

    companion object {
        private const val TAG = "VolumeReceiver"

        // These action/extra strings are @hide in the SDK but have been stable since API 1
        // (VOLUME_CHANGED) and API 23 (STREAM_MUTE_CHANGED).
        const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        const val ACTION_STREAM_MUTE_CHANGED = "android.media.STREAM_MUTE_CHANGED_ACTION"
        const val EXTRA_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        const val EXTRA_VOLUME_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
        const val EXTRA_PREV_VOLUME_VALUE = "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE"
        const val EXTRA_STREAM_MUTED = "android.media.EXTRA_STREAM_VOLUME_MUTED"

        /** Streams that count as "the user muted the phone". */
        val WATCHED_STREAMS = setOf(AudioManager.STREAM_MUSIC, AudioManager.STREAM_RING)

        fun intentFilter(): IntentFilter = IntentFilter().apply {
            addAction(ACTION_VOLUME_CHANGED)
            addAction(ACTION_STREAM_MUTE_CHANGED)
        }
    }
}
