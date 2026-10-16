package app.noca

import org.junit.Assert.*
import org.junit.Test

class CardHistoryTest {
    private fun record(balance: Int, sequence: Int, position: Int? = null, delta: Int? = null) =
        Transaction(
            "16" + "00".repeat(15),
            "2026-10-16",
            balance,
            "transit",
            delta,
            sequence,
            1,
            1,
            1,
            1,
            7,
            0,
            position,
        )

    @Test
    fun savedLegacyNeighboursRecoverTheirBalanceDifference() {
        val history =
            restoreHistoryAmounts(
                listOf(
                    ObservedTransaction(record(1700, 10), 1, 0),
                    ObservedTransaction(record(2000, 9), 1, 1),
                )
            )
        assertEquals(-300, history.first().delta)
        assertNull(history.last().delta)
    }

    @Test
    fun physicalBlockPositionsDoNotRequireConsecutiveCounters() {
        val history =
            restoreHistoryAmounts(
                listOf(
                    ObservedTransaction(record(1700, 50, 0), 1, 0),
                    ObservedTransaction(record(2000, 40, 1), 1, 1),
                )
            )
        assertEquals(-300, history.first().delta)
    }

    @Test
    fun missingBlocksAndSeparateScansDoNotInventAmounts() {
        val newest = ObservedTransaction(record(1700, 10, 0), 1, 0)
        assertNull(
            restoreHistoryAmounts(
                    listOf(
                        newest,
                        ObservedTransaction(record(2000, 9, 2), 1, 2),
                    )
                )
                .first()
                .delta
        )
        assertNull(
            restoreHistoryAmounts(
                    listOf(
                        newest,
                        ObservedTransaction(record(2000, 9, 1), 2, 1),
                    )
                )
                .first()
                .delta
        )
    }

    @Test
    fun aLegacyCounterGapWithoutBlockPositionsRemainsUnknown() {
        assertNull(
            restoreHistoryAmounts(
                    listOf(
                        ObservedTransaction(record(1700, 10), 1, 0),
                        ObservedTransaction(record(2000, 5), 1, 1),
                    )
                )
                .first()
                .delta
        )
    }

    @Test
    fun legacyCounterRolloverStillRecoversTheAmount() {
        assertEquals(
            -300,
            restoreHistoryAmounts(
                    listOf(
                        ObservedTransaction(record(1700, 0), 1, 0),
                        ObservedTransaction(record(2000, 65535), 1, 1),
                    )
                )
                .first()
                .delta,
        )
    }

    @Test
    fun knownAmountsRemainAvailableWithoutTheirPredecessor() {
        assertEquals(
            -300,
            restoreHistoryAmounts(listOf(ObservedTransaction(record(1700, 10, delta = -300), 2, 0)))
                .single()
                .delta,
        )
    }
}
