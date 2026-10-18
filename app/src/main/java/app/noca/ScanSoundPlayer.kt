package app.noca

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull

class ScanSoundPlayer : AutoCloseable {
    private var track: AudioTrack? = null
    private var closed = false

    suspend fun play(feedback: ReadFeedback) {
        if (closed) return
        stop()
        val samples =
            when (feedback) {
                ReadFeedback.SUCCESS -> GateTone.success
                ReadFeedback.FAILURE -> GateTone.failure
            }
        var playing: AudioTrack? = null
        try {
            playing =
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(GateTone.SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * Short.SIZE_BYTES)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            if (playing.state != AudioTrack.STATE_INITIALIZED) return
            val written = playing.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            if (written != samples.size) return
            currentCoroutineContext().ensureActive()
            if (closed) return
            track = playing
            playing.play()
            withTimeoutOrNull(samples.size * 1_000L / GateTone.SAMPLE_RATE + 500L) {
                while (playing.playbackHeadPosition < samples.size) delay(10L)
            }
            delay(25L)
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (_: RuntimeException) {
            // An unavailable audio output must not interrupt a completed card read.
        } finally {
            if (track === playing) track = null
            playing?.let(::release)
        }
    }

    fun stop() {
        val playing = track
        track = null
        playing?.let(::release)
    }

    override fun close() {
        closed = true
        stop()
    }

    private fun release(playing: AudioTrack) {
        runCatching { playing.pause() }
        runCatching { playing.release() }
    }
}
