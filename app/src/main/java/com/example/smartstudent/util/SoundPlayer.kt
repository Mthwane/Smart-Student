package com.example.smartstudent.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.smartstudent.R

/**
 * Plays short UI sound effects (success chime, error tone, mascot chirp) via
 * SoundPool — the right tool for low-latency, short (<1s) sounds, as opposed to
 * MediaPlayer which suits longer audio.
 *
 * Call [init] once (from [com.example.smartstudent.SmartStudentApplication]) before
 * any play* function is used. All sounds are generated tones bundled as WAV files in
 * res/raw — no network or licensing concerns.
 */
object SoundPlayer {
    private var soundPool: SoundPool? = null
    private var successId: Int = 0
    private var errorId: Int = 0
    private var chirpId: Int = 0
    private var loaded = false

    fun init(context: Context) {
        if (soundPool != null) return // already initialized

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attributes)
            .build()

        pool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) loaded = true
        }

        successId = pool.load(context, R.raw.success_chime, 1)
        errorId = pool.load(context, R.raw.error_tone, 1)
        chirpId = pool.load(context, R.raw.mascot_chirp, 1)

        soundPool = pool
    }

    fun playSuccess() = play(successId)
    fun playError() = play(errorId)
    fun playChirp() = play(chirpId)

    private fun play(soundId: Int) {
        if (!loaded || soundId == 0) return
        soundPool?.play(soundId, 0.8f, 0.8f, 1, 0, 1f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        loaded = false
    }
}
