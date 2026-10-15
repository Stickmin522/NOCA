package app.noca

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class CardRepository(context:Context):SQLiteOpenHelper(context,"noca.db",null,1) {
    override fun onConfigure(db:SQLiteDatabase){db.setForeignKeyConstraintsEnabled(true)}
    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE cards(id TEXT PRIMARY KEY,name TEXT NOT NULL,art TEXT NOT NULL,balance INTEGER NOT NULL,read_at INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE records(card_id TEXT NOT NULL,raw TEXT NOT NULL,data TEXT NOT NULL,card_date TEXT NOT NULL,sequence INTEGER NOT NULL,read_at INTEGER NOT NULL,position INTEGER NOT NULL,PRIMARY KEY(card_id,raw),FOREIGN KEY(card_id) REFERENCES cards(id) ON DELETE CASCADE)")
    }
    override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int)=Unit
    fun cards():List<SavedCard> = readableDatabase.rawQuery("SELECT id,name,art,balance,read_at FROM cards ORDER BY read_at DESC",null).use { c->buildList{while(c.moveToNext()) add(SavedCard(c.getString(0),c.getString(1),c.getString(2),c.getInt(3),c.getLong(4)))}}
    fun history(id:String):List<Transaction> = readableDatabase.rawQuery("SELECT data FROM records WHERE card_id=? ORDER BY card_date DESC,sequence DESC,read_at DESC,position ASC",arrayOf(id)).use { c->buildList{while(c.moveToNext())add(Transaction.fromJson(org.json.JSONObject(c.getString(0))))}}
    fun remember(snapshot:CardSnapshot,name:String,art:String){
        val db=writableDatabase;db.beginTransaction()
        try{
            val v=ContentValues().apply{put("id",snapshot.id);put("name",name);put("art",art);put("balance",snapshot.balance);put("read_at",snapshot.readAt)}
            db.insertWithOnConflict("cards",null,v,SQLiteDatabase.CONFLICT_IGNORE)
            updateSnapshot(db,snapshot);db.setTransactionSuccessful()
        }finally{db.endTransaction()}
    }
    fun sync(snapshot:CardSnapshot):Boolean {
        val exists=readableDatabase.rawQuery("SELECT 1 FROM cards WHERE id=?",arrayOf(snapshot.id)).use{it.moveToFirst()}
        if(!exists)return false
        val db=writableDatabase;db.beginTransaction();try{updateSnapshot(db,snapshot);db.setTransactionSuccessful()}finally{db.endTransaction()};return true
    }
    private fun updateSnapshot(db:SQLiteDatabase,s:CardSnapshot){
        // Only mutable scan data is updated: the user's chosen name and artwork survive.
        db.update("cards",ContentValues().apply{put("balance",s.balance);put("read_at",s.readAt)},"id=?",arrayOf(s.id))
        s.history.forEachIndexed{index,t->
            val v=ContentValues().apply{put("card_id",s.id);put("raw",t.raw);put("data",t.toJson().toString());put("card_date",t.date);put("sequence",t.sequence);put("read_at",s.readAt);put("position",index)}
            // Existing rows retain their original arrival ordering. Enrich an unknown delta
            // if a later read supplies the preceding record, without dropping old history.
            val inserted=db.insertWithOnConflict("records",null,v,SQLiteDatabase.CONFLICT_IGNORE)
            if(inserted==-1L && t.delta!=null)db.update("records",ContentValues().apply{put("data",t.toJson().toString())},"card_id=? AND raw=?",arrayOf(s.id,t.raw))
        }
    }
    fun edit(id:String,name:String,art:String){writableDatabase.update("cards",ContentValues().apply{put("name",name);put("art",art)},"id=?",arrayOf(id))}
}
