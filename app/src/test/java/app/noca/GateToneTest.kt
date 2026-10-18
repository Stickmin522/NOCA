package app.noca

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GateToneTest {
    @Test
    fun successHasTwoDistinctShortBurstsSeparatedBySilence() {
        val samples = GateTone.success
        val burstFrames = frames(85)
        val pauseFrames = frames(60)
        assertEquals(frames(230), samples.size)
        assertTrue(rms(samples, 0, burstFrames) > 4_000)
        assertEquals(0.0, rms(samples, burstFrames, pauseFrames), 0.0)
        assertTrue(rms(samples, burstFrames + pauseFrames, burstFrames) > 4_000)
        assertEquals(0, samples.first().toInt())
        assertEquals(0, samples.last().toInt())
        assertTrue(samples.all { abs(it.toInt()) <= Short.MAX_VALUE * 0.23 })
    }

    @Test
    fun successUsesACleanHighToneWithoutTelephoneDialFrequencies() {
        val samples = GateTone.success
        val start = frames(10)
        val count = frames(60)
        val carrier = magnitude(samples, start, count, 2_800.0)
        assertTrue(carrier > 7_000)
        for (frequency in listOf(697.0, 770.0, 852.0, 941.0, 1_209.0, 1_336.0, 1_477.0, 1_633.0)) {
            assertTrue(magnitude(samples, start, count, frequency) < carrier * 0.01)
        }
        assertTrue(magnitude(samples, start, count, 5_600.0) < carrier * 0.001)
    }

    @Test
    fun failureLengthensTheSameGateToneIntoOneContinuousBeep() {
        val samples = GateTone.failure
        assertEquals(frames(480), samples.size)
        for (startMs in 10..450 step 10) {
            assertTrue(rms(samples, frames(startMs), frames(10)) > 4_000)
        }
        val start = frames(10)
        val count = frames(400)
        val carrier = magnitude(samples, start, count, 2_800.0)
        assertTrue(carrier > 7_000)
        assertEquals(
            magnitude(GateTone.success, start, frames(60), 2_800.0),
            carrier,
            1.0,
        )
        assertTrue(magnitude(samples, start, count, 5_600.0) < carrier * 0.001)
        assertEquals(0, samples.first().toInt())
        assertEquals(0, samples.last().toInt())
        assertTrue(rms(samples, 0, frames(1)) < rms(samples, frames(10), frames(1)) * 0.2)
        assertTrue(
            rms(samples, samples.size - frames(1), frames(1)) <
                rms(samples, frames(10), frames(1)) * 0.2
        )
    }

    private fun frames(milliseconds: Int) = GateTone.SAMPLE_RATE * milliseconds / 1_000

    private fun rms(samples: ShortArray, start: Int, count: Int): Double =
        sqrt((start until start + count).sumOf { samples[it].toDouble() * samples[it] } / count)

    private fun magnitude(
        samples: ShortArray,
        start: Int,
        count: Int,
        frequency: Double,
    ): Double {
        var real = 0.0
        var imaginary = 0.0
        for (index in 0 until count) {
            val angle = 2.0 * PI * frequency * index / GateTone.SAMPLE_RATE
            real += samples[start + index] * cos(angle)
            imaginary += samples[start + index] * sin(angle)
        }
        return 2.0 * sqrt(real * real + imaginary * imaginary) / count
    }
}
