package app.noca

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Answers.RETURNS_SELF
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.same
import org.mockito.MockedConstruction
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Mockito.withSettings

class ScanSoundPlayerTest {
    @Test
    fun staticSuccessWritesPcmBeforeCheckingInitializedAndPlaying() {
        val output = Output()
        withAudioBuilders(output) { builders ->
            ScanSoundPlayer().use { player -> runBlocking { player.play(ReadFeedback.SUCCESS) } }

            val ordered = inOrder(output.track)
            ordered
                .verify(output.track)
                .write(
                    same(GateTone.success),
                    eq(0),
                    eq(GateTone.success.size),
                    eq(AudioTrack.WRITE_BLOCKING),
                )
            ordered.verify(output.track).state
            ordered.verify(output.track).play()
            ordered.verify(output.track).playbackHeadPosition
            ordered.verify(output.track).pause()
            ordered.verify(output.track).release()
            verify(builders.attributes.single()).setUsage(AudioAttributes.USAGE_MEDIA)
            verify(builders.attributes.single())
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            verify(builders.formats.single()).setSampleRate(GateTone.SAMPLE_RATE)
            verify(builders.formats.single()).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            verify(builders.formats.single()).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            verify(builders.tracks.single()).setTransferMode(AudioTrack.MODE_STATIC)
            verify(builders.tracks.single())
                .setBufferSizeInBytes(GateTone.success.size * Short.SIZE_BYTES)
        }
    }

    @Test
    fun staticFailureWritesTheCompleteLongToneAndReleasesAfterPlayback() {
        val output = Output()
        withAudioBuilders(output) {
            ScanSoundPlayer().use { player -> runBlocking { player.play(ReadFeedback.FAILURE) } }

            val ordered = inOrder(output.track)
            ordered
                .verify(output.track)
                .write(
                    same(GateTone.failure),
                    eq(0),
                    eq(GateTone.failure.size),
                    eq(AudioTrack.WRITE_BLOCKING),
                )
            ordered.verify(output.track).state
            ordered.verify(output.track).play()
            ordered.verify(output.track).playbackHeadPosition
            ordered.verify(output.track).release()
        }
    }

    @Test
    fun uninitializedOutputIsReleasedWithoutWritingOrPlaying() {
        val output = Output(initialState = AudioTrack.STATE_UNINITIALIZED)
        withAudioBuilders(output) {
            ScanSoundPlayer().use { player -> runBlocking { player.play(ReadFeedback.SUCCESS) } }

            verify(output.track, never())
                .write(any(ShortArray::class.java), anyInt(), anyInt(), anyInt())
            verify(output.track, never()).play()
            verify(output.track).release()
        }
    }

    @Test
    fun incompletePcmWriteNeverPlaysAndStillReleasesTheOutput() {
        val output = Output(writeResult = GateTone.success.size - 1)
        withAudioBuilders(output) {
            ScanSoundPlayer().use { player -> runBlocking { player.play(ReadFeedback.SUCCESS) } }

            verify(output.track)
                .write(
                    same(GateTone.success),
                    eq(0),
                    eq(GateTone.success.size),
                    eq(AudioTrack.WRITE_BLOCKING),
                )
            verify(output.track, never()).play()
            verify(output.track).release()
        }
    }

    @Test
    fun fullWriteMustInitializeOutputBeforePlayback() {
        val output = Output(initializeAfterWrite = false)
        withAudioBuilders(output) {
            ScanSoundPlayer().use { player -> runBlocking { player.play(ReadFeedback.SUCCESS) } }

            verify(output.track)
                .write(
                    same(GateTone.success),
                    eq(0),
                    eq(GateTone.success.size),
                    eq(AudioTrack.WRITE_BLOCKING),
                )
            verify(output.track, never()).play()
            verify(output.track).release()
        }
    }

    @Test
    fun cancellationReleasesAnActiveOutput() {
        val output = Output(completePlayback = false)
        withAudioBuilders(output) {
            ScanSoundPlayer().use { player ->
                runBlocking {
                    val playback =
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            player.play(ReadFeedback.SUCCESS)
                        }
                    withTimeout(1_000L) { output.started.await() }
                    playback.cancelAndJoin()
                    verify(output.track, atLeastOnce()).pause()
                    verify(output.track, atLeastOnce()).release()
                }
            }
        }
    }

    @Test
    fun stopReleasesTheCurrentlyPlayingOutput() {
        val output = Output(completePlayback = false)
        withAudioBuilders(output) {
            ScanSoundPlayer().use { player ->
                runBlocking {
                    val playback =
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            player.play(ReadFeedback.SUCCESS)
                        }
                    withTimeout(1_000L) { output.started.await() }
                    player.stop()
                    verify(output.track, atLeastOnce()).release()
                    playback.cancelAndJoin()
                }
            }
        }
    }

    @Test
    fun closeReleasesActivePlaybackAndPreventsCreatingAnotherOutput() {
        val output = Output(completePlayback = false)
        withAudioBuilders(output) { builders ->
            ScanSoundPlayer().use { player ->
                runBlocking {
                    val playback =
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            player.play(ReadFeedback.SUCCESS)
                        }
                    withTimeout(1_000L) { output.started.await() }
                    player.close()
                    verify(output.track, atLeastOnce()).release()
                    playback.cancelAndJoin()
                    player.play(ReadFeedback.FAILURE)
                    assertEquals(1, builders.tracks.size)
                }
            }
        }
    }

    private class Output(
        initialState: Int = AudioTrack.STATE_NO_STATIC_DATA,
        writeResult: Int? = null,
        initializeAfterWrite: Boolean = true,
        completePlayback: Boolean = true,
    ) {
        val track = mock(AudioTrack::class.java)
        val started = CompletableDeferred<Unit>()
        private val state = AtomicInteger(initialState)
        private val playbackHead = AtomicInteger()
        private var sampleCount = 0

        init {
            `when`(track.state).thenAnswer { state.get() }
            `when`(track.playbackHeadPosition).thenAnswer { playbackHead.get() }
            `when`(track.write(any(ShortArray::class.java), anyInt(), anyInt(), anyInt()))
                .thenAnswer { call ->
                    sampleCount = call.getArgument(2)
                    val written = writeResult ?: sampleCount
                    if (initializeAfterWrite && written == sampleCount) {
                        state.set(AudioTrack.STATE_INITIALIZED)
                    }
                    written
                }
            doAnswer {
                    started.complete(Unit)
                    if (completePlayback) playbackHead.set(sampleCount)
                    null
                }
                .`when`(track)
                .play()
        }
    }

    private class Builders(
        attributes: MockedConstruction<AudioAttributes.Builder>,
        formats: MockedConstruction<AudioFormat.Builder>,
        tracks: MockedConstruction<AudioTrack.Builder>,
    ) {
        val attributes: List<AudioAttributes.Builder> = attributes.constructed()
        val formats: List<AudioFormat.Builder> = formats.constructed()
        val tracks: List<AudioTrack.Builder> = tracks.constructed()
    }

    private fun withAudioBuilders(output: Output, assertions: (Builders) -> Unit) {
        val attributes = mock(AudioAttributes::class.java)
        val format = mock(AudioFormat::class.java)
        mockConstruction(
                AudioAttributes.Builder::class.java,
                withSettings().defaultAnswer(RETURNS_SELF),
            ) { builder, _ ->
                `when`(builder.build()).thenReturn(attributes)
            }
            .use { attributeBuilders ->
                mockConstruction(
                        AudioFormat.Builder::class.java,
                        withSettings().defaultAnswer(RETURNS_SELF),
                    ) { builder, _ ->
                        `when`(builder.build()).thenReturn(format)
                    }
                    .use { formatBuilders ->
                        mockConstruction(
                                AudioTrack.Builder::class.java,
                                withSettings().defaultAnswer(RETURNS_SELF),
                            ) { builder, _ ->
                                `when`(builder.build()).thenReturn(output.track)
                            }
                            .use { trackBuilders ->
                                assertions(
                                    Builders(attributeBuilders, formatBuilders, trackBuilders)
                                )
                            }
                    }
            }
    }
}
