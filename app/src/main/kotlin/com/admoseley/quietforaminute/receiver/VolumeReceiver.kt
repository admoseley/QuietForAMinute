package com.admoseley.quietforaminute.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log

/** Which way a watched stream just crossed the "is it silent?" line. */
enum class VolumeTransition {
    /** Went to zero, or had its mute flag set — the user silenced the phone. */
    MUTED,

    /** Came back off zero, or had its mute flag cleared — the user restored volume themselves. */
    UNMUTED
}

/**
 * Detects the moment the user mutes or unmutes the **music** or **ring** stream.
 *
 * Two system broadcasts are watched:
 *  - [ACTION_VOLUME_CHANGED]      — the stream index changed (volume keys, volume-panel slider).
 *  - [ACTION_STREAM_MUTE_CHANGED] — the stream's mute flag flipped (the bell / speaker icon in
 *                                    the volume panel, ringer → vibrate/silent). On modern
 *                                    Android this does NOT always produce a VOLUME_CHANGED
 *                                    broadcast, so listening for only the first action misses
 *                                    the "tap the icon" gesture entirely.
 *
 * Only *transitions* fire the callback: a broadcast reporting 0 with a previous value of 0 is
 * ignored, so repeated or aliased broadcasts cannot double-trigger.
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
    private val onTransition: (transition: VolumeTransition, streamType: Int) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val streamType = intent.getIntExtra(EXTRA_STREAM_TYPE, -1)
        val newVolume = intent.getIntExtra(EXTRA_VOLUME_VALUE, -1)
        val prevVolume = intent.getIntExtra(EXTRA_PREV_VOLUME_VALUE, -1)
        val muted = intent.getBooleanExtra(EXTRA_STREAM_MUTED, false)
        Log.d(TAG, "action=${intent.action} stream=$streamType vol=$prevVolume->$newVolume muted=$muted")

        transitionFor(intent.action, streamType, newVolume, prevVolume, muted)?.let { transition ->
            onTransition(transition, streamType)
        }
    }

    companion object {
        private const val TAG = "VolumeReceiver"

        /**
         * Pure decision logic, pulled out of [onReceive] so it's testable without a real
         * `android.content.Intent`. Reading extras for the "wrong" action (e.g. mute-flag extras
         * on a VOLUME_CHANGED broadcast) is harmless — they're simply absent, giving the default
         * -1 / false — so [onReceive] always extracts all four and lets this decide.
         *
         * Returns null when the broadcast isn't a transition worth acting on.
         */
        internal fun transitionFor(
            action: String?,
            streamType: Int,
            newVolume: Int,
            prevVolume: Int,
            muted: Boolean
        ): VolumeTransition? {
            if (streamType !in WATCHED_STREAMS) return null
            return when (action) {
                ACTION_VOLUME_CHANGED -> when {
                    // Fire only on the edge (non-zero -> zero). prev == -1 means the extra was
                    // absent; treat that as a transition so a real mute is never missed.
                    newVolume == 0 && prevVolume != 0 -> VolumeTransition.MUTED
                    // The mirror case: came back up off zero. Same reasoning about an absent
                    // prev extra (-1) — while a mute timer is running the stream sits at zero,
                    // so any non-zero reading means someone raised it.
                    newVolume > 0 && prevVolume <= 0 -> VolumeTransition.UNMUTED
                    else -> null
                }
                ACTION_STREAM_MUTE_CHANGED ->
                    if (muted) VolumeTransition.MUTED else VolumeTransition.UNMUTED
                else -> null
            }
        }

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
