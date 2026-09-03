package com.admoseley.quietforaminute.receiver

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Exercises [VolumeReceiver.transitionFor] — the pure decision function [VolumeReceiver.onReceive]
 * delegates to — directly with primitive values, rather than constructing a real
 * `android.content.Intent` (unavailable/stubbed in a plain JVM unit test).
 */
class VolumeReceiverTest {

    private fun transition(
        action: String? = VolumeReceiver.ACTION_VOLUME_CHANGED,
        streamType: Int = AudioManager.STREAM_MUSIC,
        newVolume: Int = -1,
        prevVolume: Int = -1,
        muted: Boolean = false
    ) = VolumeReceiver.transitionFor(action, streamType, newVolume, prevVolume, muted)

    // --- Muting ---------------------------------------------------------------------------

    @Test
    fun `volume changed from nonzero to zero is a mute`() {
        assertEquals(VolumeTransition.MUTED, transition(newVolume = 0, prevVolume = 3))
    }

    @Test
    fun `volume changed from zero to zero is not a transition`() {
        // Repeated zero broadcasts (e.g. a duplicate system callback) must not double-fire.
        assertNull(transition(newVolume = 0, prevVolume = 0))
    }

    @Test
    fun `volume changed with a missing previous-volume extra is still a mute`() {
        // prevVolume == -1 means the extra was absent; treated as a transition so a real mute
        // is never missed.
        assertEquals(VolumeTransition.MUTED, transition(newVolume = 0, prevVolume = -1))
    }

    @Test
    fun `stream mute flag set is a mute`() {
        assertEquals(
            VolumeTransition.MUTED,
            transition(action = VolumeReceiver.ACTION_STREAM_MUTE_CHANGED, muted = true)
        )
    }

    // --- Unmuting -------------------------------------------------------------------------

    @Test
    fun `volume raised off zero is an unmute`() {
        assertEquals(VolumeTransition.UNMUTED, transition(newVolume = 4, prevVolume = 0))
    }

    @Test
    fun `volume raised with a missing previous-volume extra is an unmute`() {
        // While a timer runs the stream sits at zero, so a non-zero reading means someone
        // raised it even when the system omitted the previous-value extra.
        assertEquals(VolumeTransition.UNMUTED, transition(newVolume = 4, prevVolume = -1))
    }

    @Test
    fun `volume changed between two nonzero values is not a transition`() {
        // Nudging 3 -> 5 crosses no line worth acting on.
        assertNull(transition(newVolume = 5, prevVolume = 3))
    }

    @Test
    fun `stream mute flag cleared is an unmute`() {
        assertEquals(
            VolumeTransition.UNMUTED,
            transition(action = VolumeReceiver.ACTION_STREAM_MUTE_CHANGED, muted = false)
        )
    }

    // --- Filtering ------------------------------------------------------------------------

    @Test
    fun `an unwatched stream is never a transition regardless of action`() {
        assertNull(transition(streamType = AudioManager.STREAM_ALARM, newVolume = 0, prevVolume = 3))
        assertNull(
            transition(
                action = VolumeReceiver.ACTION_STREAM_MUTE_CHANGED,
                streamType = AudioManager.STREAM_NOTIFICATION,
                muted = true
            )
        )
    }

    @Test
    fun `ring stream is watched the same as music`() {
        assertEquals(
            VolumeTransition.MUTED,
            transition(streamType = AudioManager.STREAM_RING, newVolume = 0, prevVolume = 5)
        )
        assertEquals(
            VolumeTransition.UNMUTED,
            transition(streamType = AudioManager.STREAM_RING, newVolume = 5, prevVolume = 0)
        )
    }

    @Test
    fun `an unrecognized action is never a transition`() {
        assertNull(transition(action = "some.other.broadcast", newVolume = 0, prevVolume = 3, muted = true))
    }

    @Test
    fun `a null action is never a transition`() {
        assertNull(transition(action = null, newVolume = 0, prevVolume = 3, muted = true))
    }
}
