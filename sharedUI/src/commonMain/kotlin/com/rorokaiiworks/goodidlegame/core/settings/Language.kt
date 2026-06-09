package com.rorokaiiworks.goodidlegame.core.settings

import androidx.compose.ui.unit.LayoutDirection
import goodidlegame.sharedui.generated.resources.Res
import name.kropp.kotlinx.gettext.Locale
import java.net.URI

enum class Language(
    val displayName: String,
    val code: String,
    val fileName: String,
    val locale: Locale
) {
    English("English", "en", "messages_English.po", Locale("en")),
    ChineseSimplified("简体中文", "zh_CN", "messages_SimplifiedChinese.po", Locale("zh_CN")),
    TraditionalChinese("繁體中文", "zh_TW", "messages_TraditionalChinese.po", Locale("zh_TW")),
    German("Deutsch", "de", "messages_German.po", Locale("de")),
    Spanish("Español", "es", "messages_Spanish.po", Locale("es")),
    French("Français", "fr", "messages_French.po", Locale("fr")),
    Japanese("日本語", "ja", "messages_Japanese.po", Locale("ja")),
    Korean("한국어", "ko", "messages_Korean.po", Locale("ko")),
    Norwegian("Norsk", "no", "messages_Norwegian.po", Locale("no")),
    Czech("Čeština", "cs", "messages_Czech.po", Locale("cz")),
    Dutch("Nederlands", "nl", "messages_Dutch.po", Locale("nl")),
    Hungarian("Magyar", "hu", "messages_Hungarian.po", Locale("hu")),
    Italian("Italiano", "it", "messages_Italian.po", Locale("it")),
    Polish("Polski", "pl", "messages_Polish.po", Locale("pl")),
    Portuguese("Português", "pt", "messages_Portugal.po", Locale("pt")),
    PortugueseBrazil("Português do Brasil", "pt_BR", "messages_Portuguese-Brazil.po", Locale("pt_BR")),
    SpanishLatin("Español Latino", "es_419", "messages_Spanish-Latin-America.po", Locale("es_419")),
    Turkish("Türkçe", "tr", "messages_Turkish.po", Locale("tr")),
    Ukrainian("Українська", "uk", "messages_Ukrainian.po", Locale("uk")),
    Russian("Русский", "ru", "messages_Russian.po", Locale("ru")),
    Arabic("العربية", "ar", "messages_Arabic.po", Locale("ar")),;

    companion object {
        val Language.filePath: String get() = "files/i18n/${fileName}"
        val Language.uri: URI get() = URI.create(Res.getUri(filePath))
        val Language.isRightToLeft: Boolean get() = this == Arabic
        val Language.layoutDirection: LayoutDirection get() = if (isRightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr

        fun findByCode(code: String): Language = entries.find { it.code == code } ?: English

        suspend fun Language.readBytes(): ByteArray = Res.readBytes(filePath)

        fun getDefaultLanguage(): Language {
            val locale = androidx.compose.ui.text.intl.Locale.current
            val currentCode = locale.language + "_" + locale.region
            return Language.findByCode(currentCode)
        }
    }
}