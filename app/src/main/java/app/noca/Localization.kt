package app.noca

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject
import java.util.Locale

class Texts(context:Context,locale:Locale){
    private val english=JSONObject(context.assets.open("languages/en.json").bufferedReader().use{it.readText()})
    private val code=if(locale.language=="zh")if(locale.script=="Hant"||locale.country in listOf("TW","HK","MO"))"zh-Hant" else "zh-Hans" else locale.language
    private val values=runCatching{JSONObject(context.assets.open("languages/$code.json").bufferedReader().use{it.readText()})}.getOrDefault(english)
    operator fun get(key:String):String=values.optString(key,english.optString(key,key))
}
@Composable fun texts():Texts{val context=LocalContext.current;val conf=LocalConfiguration.current;return remember(context,conf.locales.toLanguageTags()){Texts(context,conf.locales[0])}}
data class LanguageOption(val tag:String,val label:String)
fun languageOptions(context:Context):List<LanguageOption>{
    val index=JSONObject(context.assets.open("languages/index.json").bufferedReader().use{it.readText()})
    return index.keys().asSequence().map{tag->val locale=Locale.forLanguageTag(tag);LanguageOption(tag,index.getString(tag).ifEmpty{locale.getDisplayName(locale)})}.sortedBy{it.label}.toList()
}
