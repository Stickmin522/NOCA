package app.noca

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal object GateTone {
    const val SAMPLE_RATE = 48_000
    private const val FREQUENCY = 2_800.0
    private const val AMPLITUDE = 0.22
    private const val RAMP_MS = 3

    val success: ShortArray by lazy {
        val beep = tone(durationMs = 85)
        beep + ShortArray(SAMPLE_RATE * 60 / 1_000) + beep
    }

    val failure: ShortArray by lazy { tone(durationMs = 480) }

    private fun tone(durationMs: Int): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1_000
        val ramp = SAMPLE_RATE * RAMP_MS / 1_000
        return ShortArray(count) { index ->
            val edge = min(index, count - 1 - index).coerceAtMost(ramp)
            val envelope = (1.0 - cos(PI * edge / ramp)) / 2.0
            val wave = sin(2.0 * PI * FREQUENCY * index / SAMPLE_RATE)
            (wave * envelope * AMPLITUDE * Short.MAX_VALUE).roundToInt().toShort()
        }
    }
}
