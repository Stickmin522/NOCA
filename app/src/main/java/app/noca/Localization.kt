package app.noca

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import org.json.JSONObject

class Texts(context: Context, locales: List<Locale>) {
    private val english =
        JSONObject(context.assets.open("languages/en.json").bufferedReader().use { it.readText() })
    private val available =
        context.assets
            .list("languages")
            .orEmpty()
            .filter { it.endsWith(".json") && it != "index.json" }
            .map { it.removeSuffix(".json") }
            .toSet()
    private val code = resolveLanguage(locales, available)
    private val values = runCatching {
        JSONObject(
            context.assets.open("languages/$code.json").bufferedReader().use { it.readText() }
        )
    }.getOrDefault(english)

    operator fun get(key: String): String = values.optString(key, english.optString(key, key))
}

@Composable
fun texts(): Texts {
    val context = LocalContext.current
    val conf = LocalConfiguration.current
    return remember(context, conf.locales.toLanguageTags()) {
        Texts(context, (0 until conf.locales.size()).map { conf.locales[it] })
    }
}

data class LanguageOption(val tag: String, val label: String)

fun languageOptions(context: Context): List<LanguageOption> {
    val index =
        JSONObject(
            context.assets.open("languages/index.json").bufferedReader().use { it.readText() }
        )
    return index
        .keys()
        .asSequence()
        .map { tag ->
            val locale = Locale.forLanguageTag(tag)
            LanguageOption(tag, index.getString(tag).ifEmpty { locale.getDisplayName(locale) })
        }
        .sortedBy { it.label }
        .toList()
}
