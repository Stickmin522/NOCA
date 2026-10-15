package app.noca

internal object CardSql {
    const val CREATE_CARDS =
        "CREATE TABLE cards(id TEXT PRIMARY KEY,name TEXT NOT NULL,art TEXT NOT NULL,balance INTEGER NOT NULL,read_at INTEGER NOT NULL,partial INTEGER NOT NULL DEFAULT 0)"
    const val CREATE_RECORDS =
        "CREATE TABLE records(card_id TEXT NOT NULL,raw TEXT NOT NULL,data TEXT NOT NULL,card_date TEXT NOT NULL,sequence INTEGER NOT NULL,read_at INTEGER NOT NULL,position INTEGER NOT NULL,PRIMARY KEY(card_id,raw),FOREIGN KEY(card_id) REFERENCES cards(id) ON DELETE CASCADE)"
    const val CREATE_INDEX =
        "CREATE INDEX IF NOT EXISTS records_by_card ON records(card_id,card_date DESC,sequence DESC)"
    const val MIGRATE_V2 = "ALTER TABLE cards ADD COLUMN partial INTEGER NOT NULL DEFAULT 0"
    const val CARDS =
        "SELECT id,name,art,balance,read_at,partial FROM cards ORDER BY read_at DESC,id ASC"
    const val HISTORY =
        "SELECT data FROM records WHERE card_id=? ORDER BY card_date DESC,sequence DESC,read_at DESC,position ASC"
    const val EXISTS = "SELECT 1 FROM cards WHERE id=?"
    const val INSERT_CARD =
        "INSERT OR IGNORE INTO cards(id,name,art,balance,read_at,partial) VALUES(?,?,?,?,?,?)"
    const val UPDATE_SNAPSHOT =
        "UPDATE cards SET balance=?,read_at=?,partial=? WHERE id=? AND read_at<=?"
    const val INSERT_RECORD =
        "INSERT OR IGNORE INTO records(card_id,raw,data,card_date,sequence,read_at,position) VALUES(?,?,?,?,?,?,?)"
    const val UPDATE_RECORD = "UPDATE records SET data=? WHERE card_id=? AND raw=?"
    const val EDIT_CARD = "UPDATE cards SET name=?,art=? WHERE id=?"
    const val DELETE_CARD = "DELETE FROM cards WHERE id=?"
}
