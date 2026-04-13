package com.admoseley.quietforaminute.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import android.net.Uri
import com.admoseley.quietforaminute.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChimePlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun playChime(uriString: String? = null) {
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(audioAttributes)

            if (uriString != null) {
                mp.setDataSource(context, Uri.parse(uriString))
            } else {
                val afd: AssetFileDescriptor = context.resources.openRawResourceFd(R.raw.chime) ?: return
                mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            }

            mp.setOnCompletionListener { it.release() }
            mp.setOnErrorListener { player, _, _ ->
                player.release()
                true
            }
            mp.prepare()
            mp.start()
        } catch (e: Exception) {
            Log.w("ChimePlayer", "Failed to play chime: $uriString", e)
        }
    }
}
