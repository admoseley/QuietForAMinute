package com.admoseley.quietforaminute.receiver

import android.media.AudioManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises [VolumeReceiver.shouldTrigger] — the pure decision function [VolumeReceiver.onReceive]
 * delegates to — directly with primitive values, rather than constructing a real
 * `android.content.Intent` (unavailable/stubbed in a plain JVM unit test).
 */
class VolumeReceiverTest {

    private fun trigger(
        action: String? = VolumeReceiver.ACTION_VOLUME_CHANGED,
        streamType: Int = AudioManager.STREAM_MUSIC,
        newVolume: Int = -1,
        prevVolume: Int = -1,
        muted: Boolean = false
    ) = VolumeReceiver.shouldTrigger(action, streamType, newVolume, prevVolume, muted)

    @Test
    fun `volume changed from nonzero to zero triggers`() {
        assertTrue(trigger(newVolume = 0, prevVolume = 3))
    }

    @Test
    fun `volume changed from zero to zero does not trigger`() {
        // Repeated zero broadcasts (e.g. a duplicate system callback) must not double-fire.
        assertFalse(trigger(newVolume = 0, prevVolume = 0))
    }

    @Test
    fun `volume changed to a nonzero value does not trigger`() {
        assertFalse(trigger(newVolume = 5, prevVolume = 3))
    }

    @Test
    fun `volume changed with a missing previous-volume extra still triggers`() {
        // prevVolume == -1 means the extra was absent; treated as a transition so a real mute
        // is never missed.
        assertTrue(trigger(newVolume = 0, prevVolume = -1))
    }

    @Test
    fun `stream mute flag set triggers`() {
        assertTrue(
            trigger(action = VolumeReceiver.ACTION_STREAM_MUTE_CHANGED, muted = true)
        )
    }

    @Test
    fun `stream mute flag cleared does not trigger`() {
        assertFalse(
            trigger(action = VolumeReceiver.ACTION_STREAM_MUTE_CHANGED, muted = false)
        )
    }

    @Test
    fun `an unwatched stream never triggers regardless of action`() {
        assertFalse(trigger(streamType = AudioManager.STREAM_ALARM, newVolume = 0, prevVolume = 3))
        assertFalse(
            trigger(
                action = VolumeReceiver.ACTION_STREAM_MUTE_CHANGED,
                streamType = AudioManager.STREAM_NOTIFICATION,
                muted = true
            )
        )
    }

    @Test
    fun `ring stream is watched the same as music`() {
        assertTrue(trigger(streamType = AudioManager.STREAM_RING, newVolume = 0, prevVolume = 5))
    }

    @Test
    fun `an unrecognized action never triggers`() {
        assertFalse(trigger(action = "some.other.broadcast", newVolume = 0, prevVolume = 3, muted = true))
    }

    @Test
    fun `a null action never triggers`() {
        assertFalse(trigger(action = null, newVolume = 0, prevVolume = 3, muted = true))
    }
}
