package com.admoseley.quietforaminute.service

import android.os.SystemClock

/**
 * In-process handshake between [MuteTimerService] (which changes volume programmatically) and
 * [OverlayService] (which must not mistake those changes for the user muting the phone).
 *
 * Why a time window and not a counter:
 * The previous implementation kept a counter of "volume broadcasts to swallow". The receiver only
 * decremented it when a *zero* volume broadcast arrived, but a restore goes from 0 to a positive
 * level, so the counter was never consumed. It sat at 1 until the user's next genuine mute, which
 * was then silently swallowed — the "popup only appears every other time" symptom.
 *
 * A window cannot get stuck: call [markProgrammaticChange] immediately before any
 * `setStreamVolume` / `adjustStreamVolume`, and [isWithinProgrammaticWindow] is true for a short
 * period afterwards, then expires by itself. A human pressing the volume key to zero within 1.5 s
 * of the app's own restore is the only case this window misclassifies, and that is acceptable.
 */
object OverlayServiceBridge {

    private const val WINDOW_MS = 1_500L

    @Volatile
    private var programmaticUntilElapsedMs = 0L

    /** Call right before the app changes any stream volume itself. */
    fun markProgrammaticChange() {
        programmaticUntilElapsedMs = SystemClock.elapsedRealtime() + WINDOW_MS
    }

    /** True while a recent programmatic volume change may still be producing broadcasts. */
    fun isWithinProgrammaticWindow(): Boolean =
        SystemClock.elapsedRealtime() < programmaticUntilElapsedMs
}
