package com.sheguard.sos

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.provider.Settings

class SirenPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    fun start() {
        if (mediaPlayer?.isPlaying == true) {
            return
        }
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(context, Settings.System.DEFAULT_ALARM_ALERT_URI)
            isLooping = true
            prepare()
            start()
        }
    }

    fun stop() {
        mediaPlayer?.run {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
}
