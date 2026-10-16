package app.noca

import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.delay

class ScanSoundPlayer : AutoCloseable {
    private var generator: ToneGenerator? = null

    suspend fun play(feedback: ReadFeedback) {
        val tone =
            generator
                ?: runCatching {
                    ToneGenerator(AudioManager.STREAM_MUSIC, 60)
                }
                    .getOrNull()
                    ?.also { generator = it }
                ?: return
        try {
            when (feedback) {
                ReadFeedback.SUCCESS -> {
                    tone.startTone(ToneGenerator.TONE_DTMF_5, 85)
                    delay(160)
                    tone.startTone(ToneGenerator.TONE_DTMF_5, 85)
                    delay(100)
                }
                ReadFeedback.FAILURE -> {
                    tone.startTone(ToneGenerator.TONE_DTMF_5, 500)
                    delay(520)
                }
            }
        } finally {
            stop()
        }
    }

    fun stop() {
        generator?.stopTone()
    }

    override fun close() {
        stop()
        generator?.release()
        generator = null
    }
}
