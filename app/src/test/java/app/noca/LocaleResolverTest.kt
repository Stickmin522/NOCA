package app.noca

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleResolverTest {
    private val available = setOf("en", "ja", "zh-Hans", "zh-Hant", "fr", "fil", "nb")

    @Test
    fun chineseScriptTakesPriorityOverRegion() {
        assertEquals(
            "zh-Hans",
            resolveLanguage(listOf(Locale.forLanguageTag("zh-Hans-TW")), available),
        )
        assertEquals(
            "zh-Hant",
            resolveLanguage(listOf(Locale.forLanguageTag("zh-Hant-CN")), available),
        )
    }

    @Test
    fun traditionalChineseRegionsUseTraditionalResources() {
        for (region in listOf("TW", "HK", "MO")) {
            assertEquals("zh-Hant", resolveLanguage(listOf(Locale("zh", region)), available))
        }
        assertEquals("zh-Hans", resolveLanguage(listOf(Locale.SIMPLIFIED_CHINESE), available))
    }

    @Test
    fun secondarySystemLanguageIsUsedBeforeEnglishFallback() {
        assertEquals("fr", resolveLanguage(listOf(Locale("xx"), Locale.CANADA_FRENCH), available))
        assertEquals("en", resolveLanguage(listOf(Locale("xx")), available))
        assertEquals("en", resolveLanguage(emptyList(), available))
    }

    @Test
    fun regionalLanguagesFallBackToTheirBase() {
        assertEquals("ja", resolveLanguage(listOf(Locale.JAPAN), available))
        assertEquals("fr", resolveLanguage(listOf(Locale.FRANCE), available))
    }

    @Test
    fun languageAliasesResolveToAvailableResources() {
        assertEquals("fil", resolveLanguage(listOf(Locale("tl")), available))
        assertEquals("nb", resolveLanguage(listOf(Locale("no")), available))
    }
}
