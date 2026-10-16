package app.noca

import org.json.JSONArray
import org.json.JSONObject

data class Transaction(
    val raw: String,
    val date: String,
    val balance: Int,
    val kind: String,
    val delta: Int?,
    val sequence: Int,
    val process: Int,
    val inLine: Int,
    val inStation: Int,
    val outLine: Int,
    val outStation: Int,
    val region: Int,
    val position: Int? = null,
) {
    val terminal: Int
        get() = raw.take(2).toIntOrNull(16) ?: -1

    val isBus: Boolean
        get() = terminal == 0x05 || (process and 0x7f) in setOf(0x0d, 0x0f, 0x1f, 0x23)

    fun toJson() =
        JSONObject()
            .put("raw", raw)
            .put("date", date)
            .put("balance", balance)
            .put("kind", kind)
            .put("delta", delta ?: JSONObject.NULL)
            .put("sequence", sequence)
            .put("process", process)
            .put("inLine", inLine)
            .put("inStation", inStation)
            .put("outLine", outLine)
            .put("outStation", outStation)
            .put("region", region)
            .put("position", position ?: JSONObject.NULL)

    companion object {
        fun fromJson(o: JSONObject) =
            Transaction(
                o.getString("raw"),
                o.getString("date"),
                o.getInt("balance"),
                o.getString("kind"),
                if (o.isNull("delta")) null else o.getInt("delta"),
                o.getInt("sequence") and 0xffff,
                o.getInt("process"),
                o.getInt("inLine"),
                o.getInt("inStation"),
                o.getInt("outLine"),
                o.getInt("outStation"),
                o.getInt("region"),
                if (o.isNull("position")) null else o.getInt("position"),
            )

        fun parse(text: String): List<Transaction> {
            val a = JSONArray(text)
            return (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
        }
    }
}

data class CardSnapshot(
    val id: String,
    val balance: Int,
    val readAt: Long,
    val history: List<Transaction>,
    val partial: Boolean = false,
)

data class SavedCard(
    val id: String,
    val name: String,
    val art: String,
    val balance: Int,
    val readAt: Long,
    val partial: Boolean = false,
)

enum class ThemeColor {
    LIME,
    MIST,
    CORAL,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

data class Preferences(
    val color: ThemeColor = ThemeColor.LIME,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val soundEnabled: Boolean = true,
)

enum class ReadFeedback {
    SUCCESS,
    FAILURE,
}

data class UiState(
    val cards: List<SavedCard> = emptyList(),
    val current: CardSnapshot? = null,
    val reading: Boolean = false,
    val error: String? = null,
    val syncNotice: Boolean = false,
    val preferences: Preferences = Preferences(),
    val nfcAvailable: Boolean = true,
    val nfcEnabled: Boolean = true,
)
