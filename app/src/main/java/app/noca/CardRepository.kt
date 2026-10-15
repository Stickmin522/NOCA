package app.noca

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

class CardRepository(context: Context) : SQLiteOpenHelper(context, "noca.db", null, 2) {
    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CardSql.CREATE_CARDS)
        db.execSQL(CardSql.CREATE_RECORDS)
        db.execSQL(CardSql.CREATE_INDEX)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(CardSql.MIGRATE_V2)
            db.execSQL(CardSql.CREATE_INDEX)
        }
    }

    fun cards(): List<SavedCard> =
        readableDatabase
            .rawQuery(
                CardSql.CARDS,
                null,
            )
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            SavedCard(
                                cursor.getString(0),
                                cursor.getString(1),
                                cursor.getString(2),
                                cursor.getInt(3),
                                cursor.getLong(4),
                                cursor.getInt(5) != 0,
                            )
                        )
                    }
                }
            }

    fun history(id: String): List<Transaction> =
        readableDatabase
            .rawQuery(
                CardSql.HISTORY,
                arrayOf(id),
            )
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(
                        Transaction.fromJson(JSONObject(cursor.getString(0)))
                    )
                }
            }

    fun remember(snapshot: CardSnapshot, name: String, art: String) {
        require(name.isNotBlank() && name.trim().length <= 40)
        transaction { db ->
            db.execSQL(
                CardSql.INSERT_CARD,
                arrayOf<Any>(
                    snapshot.id,
                    name.trim(),
                    art,
                    snapshot.balance,
                    snapshot.readAt,
                    if (snapshot.partial) 1 else 0,
                ),
            )
            updateSnapshot(db, snapshot)
        }
    }

    fun sync(snapshot: CardSnapshot): Boolean = transaction { db ->
        val exists = db.rawQuery(CardSql.EXISTS, arrayOf(snapshot.id)).use { it.moveToFirst() }
        if (exists) updateSnapshot(db, snapshot)
        exists
    }

    private fun updateSnapshot(db: SQLiteDatabase, snapshot: CardSnapshot) {
        // A delayed save must not overwrite a more recent NFC scan.
        db.execSQL(
            CardSql.UPDATE_SNAPSHOT,
            arrayOf<Any>(
                snapshot.balance,
                snapshot.readAt,
                if (snapshot.partial) 1 else 0,
                snapshot.id,
                snapshot.readAt,
            ),
        )
        snapshot.history.forEachIndexed { index, record ->
            val data = record.toJson().toString()
            db.execSQL(
                CardSql.INSERT_RECORD,
                arrayOf<Any>(
                    snapshot.id,
                    record.raw,
                    data,
                    record.date,
                    record.sequence,
                    snapshot.readAt,
                    index,
                ),
            )
            if (record.delta != null) {
                db.execSQL(CardSql.UPDATE_RECORD, arrayOf(data, snapshot.id, record.raw))
            }
        }
    }

    fun edit(id: String, name: String, art: String) {
        require(name.isNotBlank() && name.trim().length <= 40)
        writableDatabase.execSQL(CardSql.EDIT_CARD, arrayOf(name.trim(), art, id))
    }

    fun forget(id: String) {
        writableDatabase.execSQL(CardSql.DELETE_CARD, arrayOf(id))
    }

    private inline fun <T> transaction(block: (SQLiteDatabase) -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = block(db)
            db.setTransactionSuccessful()
            return result
        } finally {
            db.endTransaction()
        }
    }
}
