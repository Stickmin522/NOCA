package app.noca

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CardDataTest {
    @Test
    fun absentArtworkFieldsStayNull() {
        val artwork =
            CardArtwork.fromJson(
                JSONObject("""{"id":"generic","name":"NOCA","file":null,"source":null}""")
            )
        assertNull(artwork.file)
        assertNull(artwork.source)
        assertNull(CardArtwork.fromJson(JSONObject("""{"id":"generic","name":"NOCA"}""")).file)
    }

    @Test
    fun artworkPathsCannotLeavePrivateStorage() {
        assertNull(artworkFileName("custom:../../outside.png"))
        assertNull(artworkFileName("custom:/tmp/image.png"))
        assertNull(artworkFileName("custom:null"))
        assertNull(artworkFileName("suica"))
        val filename = "a12b3456-7890-1234-5678-901234567890.png"
        assertEquals(filename, artworkFileName("custom:$filename"))
    }

    @Test
    fun transactionsRetainUnknownAmountsAndHistoryPositions() {
        val record =
            Transaction(
                "00112233445566778899aabbccddeeff",
                "2026-10-16",
                4248,
                "transit",
                null,
                0x3456,
                1,
                1,
                2,
                3,
                4,
                0,
                2,
            )
        val restored = Transaction.parse("[${record.toJson()}]").single()
        assertEquals(record, restored)
        assertTrue(record.toJson().isNull("delta"))
        assertEquals(0x3456, restored.sequence)
        assertEquals(2, restored.position)
    }

    @Test
    fun legacyCounterValuesAreNormalizedWithoutLosingCardBytes() {
        val json =
            JSONObject(
                """{"raw":"16010000354f0101010703e891123400","date":"2026-10-16","balance":1000,"kind":"transit","delta":null,"sequence":9507380,"process":1,"inLine":1,"inStation":1,"outLine":1,"outStation":7,"region":0}"""
            )
        val record = Transaction.fromJson(json)
        assertEquals(json.getString("raw"), record.raw)
        assertEquals(json.getInt("sequence") and 0xffff, record.sequence)
        assertNull(record.position)
    }

    @Test
    fun knownZeroAmountIsNotUnknown() {
        val record =
            Transaction(
                "00112233445566778899aabbccddeeff",
                "2026-10-16",
                1000,
                "other",
                0,
                7,
                0,
                0,
                0,
                0,
                0,
                0,
            )
        val restored = Transaction.fromJson(record.toJson())
        assertEquals(0, restored.delta)
        assertFalse(record.toJson().isNull("delta"))
    }

    @Test
    fun malformedTransactionDataIsRejected() {
        assertThrows(org.json.JSONException::class.java) { Transaction.parse("[{}]") }
        assertThrows(org.json.JSONException::class.java) { Transaction.parse("not-json") }
        assertTrue(Transaction.parse("[]").isEmpty())
    }
}
