package com.rorokaiiworks.goodidlegame.core

import com.rorokaiiworks.goodidlegame.byteArrayToOkioSource
import com.rorokaiiworks.goodidlegame.core.settings.Language
import com.rorokaiiworks.goodidlegame.core.settings.Language.Companion.readBytes
import kotlinx.coroutines.runBlocking
import name.kropp.kotlinx.gettext.Gettext
import name.kropp.kotlinx.gettext.I18n
import name.kropp.kotlinx.gettext.Locale

class I18nLoader(
    override val locale: Locale,
    default: I18n
) : I18n {
    fun changeLanguage(language: Language): Boolean {
        if (gettext.locale == language.locale) return false

        runBlocking {
            gettext = Gettext.load(
                language.locale,
                byteArrayToOkioSource(language.readBytes())
            )
        }

        return true
    }

    private var gettext: I18n = default

    override fun tr(text: String): String = gettext.tr(text)

    override fun tr(text: String, vararg args: Pair<String, String>): String = gettext.tr(text, *args)

    override fun trn(text: String, plural: String, n: Int): String = gettext.trn(text, plural, n)

    override fun trn(text: String, plural: String, n: Long): String = gettext.trn(text, plural, n)

    override fun trn(
        text: String,
        plural: String,
        n: Int,
        vararg args: Pair<String, String>
    ): String = gettext.trn(text, plural, n, *args)

    override fun trn(
        text: String,
        plural: String,
        n: Long,
        vararg args: Pair<String, String>
    ): String = gettext.trn(text, plural, n, *args)


    override fun trc(context: String, text: String): String = gettext.trc(context, text)

    override fun trc(
        context: String,
        text: String,
        vararg args: Pair<String, String>
    ): String = gettext.trc(context, text, *args)

    override fun trnc(
        context: String,
        text: String,
        plural: String,
        n: Int
    ): String = gettext.trnc(context, text, plural, n)

    override fun trnc(
        context: String,
        text: String,
        plural: String,
        n: Long
    ): String = gettext.trnc(context, text, plural, n)

    override fun trnc(
        context: String,
        text: String,
        plural: String,
        n: Int,
        vararg args: Pair<String, String>
    ): String = gettext.trnc(context, text, plural, n, *args)

    override fun trnc(
        context: String,
        text: String,
        plural: String,
        n: Long,
        vararg args: Pair<String, String>
    ): String = gettext.trnc(context, text, plural, n, *args)


    override fun marktr(text: String): String = gettext.marktr(text)
}