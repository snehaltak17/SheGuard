package com.sheguard.sos

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.sheguard.R

object SirenManager {

    private var mediaPlayer: MediaPlayer? = null

    fun start(context: Context) {
        if (mediaPlayer?.isPlaying == true) {
            return
        }

        mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.siren_alarm).apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setVolume(1.0f, 1.0f)
            start()
        }
    }

    fun stop() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
    }

    fun isRunning(): Boolean = mediaPlayer?.isPlaying == true
}
