package app.noca

import java.io.Reader
import java.util.Locale

data class RailStation(val operator: String, val line: String, val name: String)

data class JourneyStop(val area: Int, val line: Int, val station: Int, val name: RailStation?) {
    val code: String
        get() = String.format(Locale.ROOT, "%d-%02X-%02X", area, line, station)
}

data class RailJourney(val from: JourneyStop, val to: JourneyStop)

class StationDirectory private constructor(private val stations: Map<Int, RailStation>) {
    fun journey(record: Transaction): RailJourney? {
        val process = record.process and 0x7f
        // Bus records and purchases store different data in the station bytes.
        if (record.kind in setOf("purchase", "topup") || record.isBus || process !in RAIL_PROCESSES)
            return null
        return RailJourney(
            stop((record.region ushr 6) and 3, record.inLine, record.inStation),
            stop((record.region ushr 4) and 3, record.outLine, record.outStation),
        )
    }

    private fun stop(area: Int, line: Int, station: Int) =
        JourneyStop(area, line, station, stations[key(area, line, station)])

    companion object {
        val EMPTY = StationDirectory(emptyMap())
        private val RAIL_PROCESSES = setOf(0x01, 0x04, 0x05, 0x06, 0x08, 0x13)

        private fun key(area: Int, line: Int, station: Int) =
            (area shl 16) or (line shl 8) or station

        fun read(reader: Reader): StationDirectory {
            val stations = mutableMapOf<Int, RailStation>()
            reader.buffered().useLines { lines ->
                lines
                    .drop(1)
                    .filter { it.isNotBlank() }
                    .forEach { row ->
                        val columns = row.split('\t')
                        require(columns.size == 6)
                        val (area, line, station) = columns.take(3).map { it.toInt() }
                        require(area in 0..3 && line in 0..255 && station in 0..255)
                        val name = RailStation(columns[3], columns[4], columns[5])
                        require(name.name.isNotBlank())
                        require(stations.put(key(area, line, station), name) == null)
                    }
            }
            return StationDirectory(stations)
        }
    }
}
