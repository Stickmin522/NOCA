package app.noca

import org.json.JSONArray
import org.json.JSONObject

data class Transaction(
    val raw: String, val date: String, val balance: Int, val kind: String,
    val delta: Int?, val sequence: Int, val process: Int,
    val inLine: Int, val inStation: Int, val outLine: Int, val outStation: Int,
    val region: Int
) {
    fun toJson() = JSONObject().put("raw",raw).put("date",date).put("balance",balance)
        .put("kind",kind).put("delta",delta?:JSONObject.NULL).put("sequence",sequence)
        .put("process",process).put("inLine",inLine).put("inStation",inStation)
        .put("outLine",outLine).put("outStation",outStation).put("region",region)
    companion object {
        fun fromJson(o:JSONObject) = Transaction(o.getString("raw"),o.getString("date"),
            o.getInt("balance"),o.getString("kind"),if(o.isNull("delta")) null else o.getInt("delta"),
            o.getInt("sequence"),o.getInt("process"),o.getInt("inLine"),o.getInt("inStation"),
            o.getInt("outLine"),o.getInt("outStation"),o.getInt("region"))
        fun parse(text:String):List<Transaction> { val a=JSONArray(text);return (0 until a.length()).map {fromJson(a.getJSONObject(it))} }
    }
}

data class CardSnapshot(val id:String,val balance:Int,val readAt:Long,val history:List<Transaction>,val partial:Boolean=false)
data class SavedCard(val id:String,val name:String,val art:String,val balance:Int,val readAt:Long)
enum class ThemeColor { LIME, MIST, CORAL }
enum class ThemeMode { SYSTEM, LIGHT, DARK }
data class Preferences(val color:ThemeColor=ThemeColor.LIME,val mode:ThemeMode=ThemeMode.SYSTEM)
data class UiState(
    val cards:List<SavedCard> = emptyList(), val current:CardSnapshot?=null,
    val reading:Boolean=false, val error:String?=null,val syncNotice:Boolean=false,
    val preferences:Preferences=Preferences(),val nfcAvailable:Boolean=true,val nfcEnabled:Boolean=true
)
