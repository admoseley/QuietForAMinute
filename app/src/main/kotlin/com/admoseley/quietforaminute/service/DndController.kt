package com.admoseley.quietforaminute.service

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns Do Not Disturb on for the length of a mute timer, and back off when it ends (issue #45).
 *
 * DND is deliberately layered *on top of* the volume mute rather than replacing it: the standard
 * filter used here ([NotificationManager.INTERRUPTION_FILTER_PRIORITY]) suppresses notifications,
 * calls and the buzzing that comes with them, but it does **not** silence media playback. Muting
 * the stream is still what makes the phone quiet; DND is what stops it interrupting.
 *
 * Requires the `ACCESS_NOTIFICATION_POLICY` special access — [canControlDnd] reports whether the
 * user has granted it. Every call here is a no-op when it hasn't been, so callers never have to
 * guard: without the grant, `setInterruptionFilter` throws SecurityException.
 */
@Singleton
class DndController @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    /** True once the user has granted DND access in system settings. */
    fun canControlDnd(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    /** True when some DND filter is already active, whoever turned it on. */
    fun isDndActive(): Boolean =
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN

    /**
     * Switches DND on and reports whether *this call* is what changed it.
     *
     * The return value is the whole point: it is persisted alongside the rest of the pending
     * restore so [disable] can tell "the app turned DND on for this timer" apart from "the user
     * already had DND on before the timer started". Only the former may be switched off later —
     * silently clearing a DND the user set themselves would be the app undoing a setting it never
     * owned. False here therefore means both "we couldn't" and "we didn't need to".
     */
    fun enable(): Boolean {
        if (!canControlDnd()) {
            Log.w(TAG, "DND requested but notification policy access is not granted")
            return false
        }
        if (isDndActive()) {
            Log.d(TAG, "DND already active — leaving it alone so we don't own turning it off")
            return false
        }
        return try {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            Log.d(TAG, "DND enabled for the mute duration")
            true
        } catch (e: SecurityException) {
            // Access can be revoked between the check above and here.
            Log.w(TAG, "Unable to enable DND", e)
            false
        }
    }

    /**
     * Switches DND back off. Callers must only invoke this when the matching [enable] returned
     * true — see the note there about not clearing a DND the user set for themselves.
     */
    fun disable() {
        if (!canControlDnd()) {
            Log.w(TAG, "Cannot clear DND — notification policy access was revoked mid-timer")
            return
        }
        try {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            Log.d(TAG, "DND cleared")
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to clear DND", e)
        }
    }

    private companion object {
        const val TAG = "DndController"
    }
}
