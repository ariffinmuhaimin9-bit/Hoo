package com.example.utils

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

object SoundHelper {
    private var toneGen: ToneGenerator? = null
    var isMuted: Boolean = false

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            Log.e("SoundHelper", "Failed to init ToneGenerator", e)
        }
    }

    fun playPop() {
        if (isMuted) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
        } catch (_: Exception) {}
    }

    fun playBoing() {
        if (isMuted) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_DTMF_P, 90)
        } catch (_: Exception) {}
    }

    fun playCoin() {
        if (isMuted) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
        } catch (_: Exception) {}
    }

    fun playFanfare() {
        if (isMuted) return
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 300)
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            toneGen?.release()
        } catch (_: Exception) {}
        toneGen = null
    }
}
