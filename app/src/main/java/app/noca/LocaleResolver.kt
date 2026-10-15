package app.noca

import java.util.Locale

internal fun localeCandidates(locale: Locale): List<String> {
    val language =
        when (locale.language) {
            "iw" -> "he"
            "in" -> "id"
            "ji" -> "yi"
            "no" -> "nb"
            "tl" -> "fil"
            else -> locale.language
        }
    if (language == "zh") {
        val script =
            locale.script.ifEmpty {
                if (locale.country in listOf("TW", "HK", "MO")) "Hant" else "Hans"
            }
        return listOf(locale.toLanguageTag(), "zh-$script", "zh").distinct()
    }
    return listOfNotNull(
            locale.toLanguageTag(),
            locale.script.takeIf { it.isNotEmpty() }?.let { "$language-$it" },
            locale.country.takeIf { it.isNotEmpty() }?.let { "$language-$it" },
            language,
        )
        .distinct()
}

internal fun resolveLanguage(locales: List<Locale>, available: Set<String>): String =
    locales
        .asSequence()
        .flatMap { localeCandidates(it).asSequence() }
        .firstOrNull { it in available } ?: "en"
