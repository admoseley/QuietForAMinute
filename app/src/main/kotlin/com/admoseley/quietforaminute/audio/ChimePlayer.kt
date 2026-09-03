package com.admoseley.quietforaminute.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.admoseley.quietforaminute.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fire-and-forget playback of the mute / restore chime.
 *
 * A fresh [MediaPlayer] is created per call and released on completion or error, so overlapping
 * calls (mute chime while a restore chime is still ringing) never fight over one instance.
 *
 * Playback goes through USAGE_NOTIFICATION_EVENT, i.e. the notification stream. On most devices
 * that stream is aliased to the ringer, so the "chime on mute" will be inaudible when the user
 * has just muted the *ringer* — that is expected, not a bug.
 */
@Singleton
class ChimePlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /**
     * @param uriString a `content://` ringtone URI chosen in Settings, or null for the bundled
     *                  chime. User-chosen URIs are opened with this app's own permissions; the
     *                  ringtone picker only returns media-store / system ringtone URIs, so no
     *                  persisted URI permission is needed.
     */
    fun playChime(uriString: String? = null) {
        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(audioAttributes)

            if (uriString != null) {
                mp.setDataSource(context, Uri.parse(uriString))
            } else {
                context.resources.openRawResourceFd(R.raw.chime).use { afd ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
            }

            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { it.release() }
            mp.setOnErrorListener { player, what, extra ->
                Log.w(TAG, "MediaPlayer error what=$what extra=$extra for $uriString")
                player.release()
                true
            }
            // prepareAsync: the callers run on the main thread and a content:// source can
            // involve disk / content-provider I/O, so never block on prepare().
            mp.prepareAsync()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play chime: $uriString", e)
            mp.release()
        }
    }

    private companion object {
        const val TAG = "ChimePlayer"
    }
}
