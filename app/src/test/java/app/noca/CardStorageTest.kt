package app.noca

import java.sql.Connection
import java.sql.DriverManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CardStorageTest {
    private lateinit var database: Connection

    @Before
    fun open() {
        database = DriverManager.getConnection("jdbc:sqlite::memory:")
        execute("PRAGMA foreign_keys=ON")
        execute(CardSql.CREATE_CARDS)
        execute(CardSql.CREATE_RECORDS)
        execute(CardSql.CREATE_INDEX)
        execute(CardSql.INSERT_CARD, "card", "Commute", "suica", 1000, 10L, 0)
    }

    @After fun close() = database.close()

    @Test
    fun rescanningPreservesNameAndArtwork() {
        execute(CardSql.UPDATE_SNAPSHOT, 800, 20L, 1, "card", 20L)
        assertEquals(
            listOf("card", "Commute", "suica", "800", "20", "1"),
            query(CardSql.CARDS).single(),
        )
    }

    @Test
    fun delayedSaveCannotRegressTheBalanceOrReplaceUserEdits() {
        execute(CardSql.EDIT_CARD, "Weekend", "pasmo", "card")
        execute(CardSql.UPDATE_SNAPSHOT, 1500, 30L, 0, "card", 30L)
        execute(CardSql.INSERT_CARD, "card", "Old name", "generic", 500, 5L, 1)
        execute(CardSql.UPDATE_SNAPSHOT, 500, 5L, 1, "card", 5L)
        assertEquals(
            listOf("card", "Weekend", "pasmo", "1500", "30", "0"),
            query(CardSql.CARDS).single(),
        )
    }

    @Test
    fun repeatedRecordsAreDeduplicatedAndCardOrderSurvivesCounterRollover() {
        execute(CardSql.INSERT_RECORD, "card", "older", "older-data", "2026-10-16", 65535, 10, 0)
        execute(CardSql.INSERT_RECORD, "card", "newer", "newer-data", "2026-10-16", 0, 20, 0)
        execute(CardSql.INSERT_RECORD, "card", "older", "duplicate", "2026-10-16", 65535, 20, 1)
        assertEquals(
            listOf("newer-data", "older-data"),
            query(CardSql.HISTORY, "card").map { it.first() },
        )
    }

    @Test
    fun rescanningUsesTheLatestObservedHistoryPosition() {
        execute(CardSql.INSERT_RECORD, "card", "record", "unknown", "2026-10-16", 7, 10, 2)
        execute(CardSql.INSERT_RECORD, "card", "record", "known", "2026-10-16", 7, 20, 0)
        execute(CardSql.UPDATE_RECORD, "known", 7, 20, 0, "card", "record", 20)
        assertEquals(
            listOf("known", "20", "0"),
            query("SELECT data,read_at,position FROM records").single(),
        )
    }

    @Test
    fun deletingACardCascadesOnlyItsOwnHistory() {
        execute(CardSql.INSERT_CARD, "other", "Other", "generic", 1, 10, 0)
        execute(CardSql.INSERT_RECORD, "card", "a", "a", "2026-10-16", 1, 10, 0)
        execute(CardSql.INSERT_RECORD, "other", "b", "b", "2026-10-16", 1, 10, 0)
        execute(CardSql.DELETE_CARD, "card")
        assertTrue(query(CardSql.EXISTS, "card").isEmpty())
        assertEquals(listOf(listOf("other")), query("SELECT card_id FROM records"))
    }

    @Test
    fun failedTransactionRollsBackTheBalanceUpdate() {
        database.autoCommit = false
        try {
            execute(CardSql.UPDATE_SNAPSHOT, 800, 20, 0, "card", 20)
            assertThrows(java.sql.SQLException::class.java) {
                execute(CardSql.INSERT_RECORD, "missing", "r", "r", "2026-10-16", 1, 20, 0)
            }
        } finally {
            database.rollback()
            database.autoCommit = true
        }
        assertEquals("1000", query(CardSql.CARDS).single()[3])
    }

    @Test
    fun versionOneMigrationPreservesCardsAndRecords() {
        execute("DROP TABLE records")
        execute("DROP TABLE cards")
        execute(
            "CREATE TABLE cards(id TEXT PRIMARY KEY,name TEXT NOT NULL,art TEXT NOT NULL,balance INTEGER NOT NULL,read_at INTEGER NOT NULL)"
        )
        execute("INSERT INTO cards VALUES('card','Commute','suica',4248,10)")
        execute(CardSql.CREATE_RECORDS)
        execute(CardSql.INSERT_RECORD, "card", "r", "saved", "2026-10-16", 1, 10, 0)
        execute(CardSql.MIGRATE_V2)
        execute(CardSql.CREATE_INDEX)
        assertEquals(
            listOf("card", "Commute", "suica", "4248", "10", "0"),
            query(CardSql.CARDS).single(),
        )
        assertEquals(listOf("saved"), query(CardSql.HISTORY, "card").map { it.first() })
    }

    @Test
    fun versionThreeMigrationNormalizesCountersAndKeepsAllHistory() {
        execute(CardSql.INSERT_RECORD, "card", "a", "first", "2026-10-16", 0x800007, 10, 0)
        execute(CardSql.INSERT_RECORD, "card", "b", "second", "2026-10-16", 0x400006, 10, 1)
        execute(CardSql.MIGRATE_V3)
        execute(CardSql.DROP_HISTORY_INDEX)
        execute(CardSql.CREATE_INDEX)
        assertEquals(
            listOf(listOf("7"), listOf("6")),
            query("SELECT sequence FROM records ORDER BY position"),
        )
        assertEquals(listOf("first", "second"), query(CardSql.HISTORY, "card").map { it.first() })
        assertEquals("1000", query(CardSql.CARDS).single()[3])
    }

    @Test
    fun olderSnapshotsCannotChangeANewerHistoryPosition() {
        execute(CardSql.INSERT_RECORD, "card", "r", "current", "2026-10-16", 10, 30, 2)
        execute(CardSql.UPDATE_RECORD, "stale", 10, 20, 0, "card", "r", 20)
        assertEquals(
            listOf("current", "30", "2"),
            query("SELECT data,read_at,position FROM records").single(),
        )
    }

    private fun execute(sql: String, vararg values: Any?) {
        database.prepareStatement(sql).use { statement ->
            values.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
            statement.executeUpdate()
        }
    }

    private fun query(sql: String, vararg values: Any?): List<List<String>> =
        database.prepareStatement(sql).use { statement ->
            values.forEachIndexed { index, value -> statement.setObject(index + 1, value) }
            statement.executeQuery().use { rows ->
                buildList {
                    while (rows.next()) add(
                        (1..rows.metaData.columnCount).map { rows.getString(it) }
                    )
                }
            }
        }
}
