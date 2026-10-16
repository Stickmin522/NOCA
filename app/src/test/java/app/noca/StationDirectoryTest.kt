package app.noca

import org.junit.Assert.*
import org.junit.Test

class StationDirectoryTest {
    private val directory =
        StationDirectory.read(
            requireNotNull(javaClass.getResourceAsStream("/stations.tsv")).reader(Charsets.UTF_8)
        )

    private fun record(
        inLine: Int = 1,
        inStation: Int = 1,
        outLine: Int = 1,
        outStation: Int = 7,
        region: Int = 0,
        process: Int = 1,
        terminal: Int = 0x16,
        kind: String = "transit",
    ) =
        Transaction(
            "%02x".format(terminal) + "00".repeat(15),
            "2026-10-16",
            1000,
            kind,
            null,
            1,
            process,
            inLine,
            inStation,
            outLine,
            outStation,
            region,
        )

    @Test
    fun resolvesPublicStationCodesForBothEnds() {
        val journey = requireNotNull(directory.journey(record()))
        assertEquals("東京", journey.from.name?.name)
        assertEquals("品川", journey.to.name?.name)
        assertEquals("東海道線", journey.from.name?.line)
        assertEquals("東日本旅客鉄道", journey.from.name?.operator)
    }

    @Test
    fun entryAndExitAreasAreIndependent() {
        val journey = requireNotNull(directory.journey(record(0x82, 0x22, 0x82, 0x22, 0x60)))
        assertEquals("矢場町", journey.from.name?.name)
        assertEquals("天満橋", journey.to.name?.name)
    }

    @Test
    fun lowRegionFlagsDoNotAffectStationNames() {
        val journey = requireNotNull(directory.journey(record(0x82, 0x22, 0x82, 0x22, 0x6f)))
        assertEquals("矢場町", journey.from.name?.name)
        assertEquals("天満橋", journey.to.name?.name)
    }

    @Test
    fun unknownCodesDoNotUseAnotherAreasStations() {
        val journey = requireNotNull(directory.journey(record(0x82, 0x22, 0xff, 0xff)))
        assertNull(journey.from.name)
        assertNull(journey.to.name)
        assertEquals("0-82-22", journey.from.code)
        assertEquals("0-FF-FF", journey.to.code)
    }

    @Test
    fun doesNotInventRailJourneysForBusPurchasesOrTopups() {
        assertNull(directory.journey(record(terminal = 0x05)))
        for (process in listOf(0x0d, 0x0f, 0x1f, 0x23)) {
            assertNull(directory.journey(record(process = process)))
        }
        assertNull(directory.journey(record(kind = "purchase")))
        assertNull(directory.journey(record(process = 0x46, terminal = 0xc8)))
        assertNull(directory.journey(record(process = 2, kind = "topup")))
        assertNull(directory.journey(record(process = 3)))
    }

    @Test
    fun existingSavedJsonResolvesWithoutAdditionalFields() {
        val restored = Transaction.fromJson(record().toJson())
        assertFalse(restored.toJson().has("terminal"))
        assertEquals(0x16, restored.terminal)
        assertEquals("東京", directory.journey(restored)?.from?.name?.name)
        assertEquals("品川", directory.journey(restored)?.to?.name?.name)
    }

    @Test
    fun processFlagAndLegacyKindKeepStationResolution() {
        assertEquals(
            "東京",
            directory.journey(record(process = 0x81, kind = "other"))?.from?.name?.name,
        )
        assertEquals("東京", directory.journey(record(process = 6, kind = "other"))?.from?.name?.name)
    }

    @Test
    fun conflictingSourceCodesAreUnresolved() {
        val journey = requireNotNull(directory.journey(record(6, 77, 6, 87)))
        assertNull(journey.from.name)
        assertNull(journey.to.name)
    }

    @Test
    fun directoryRejectsMalformedOrDuplicateKeys() {
        val header = "area\tline\tstation\toperator\tline_name\tstation_name\n"
        val row = "0\t1\t1\tJR\t東海道線\t東京\n"
        assertThrows(IllegalArgumentException::class.java) {
            StationDirectory.read((header + row + row).reader())
        }
        assertThrows(IllegalArgumentException::class.java) {
            StationDirectory.read((header + "4\t1\t1\tJR\t東海道線\t東京\n").reader())
        }
    }
}
