package com.rorokaiiworks.goodidlegame.core

import name.kropp.kotlinx.gettext.I18n
import name.kropp.kotlinx.gettext.Locale

class FakeI18n : I18n {
    override val locale: Locale
        get() = Locale("en")

    override fun tr(text: String): String = text

    override fun tr(text: String, vararg args: Pair<String, String>): String {
        return text
    }

    override fun trn(text: String, plural: String, n: Int): String {
        return text
    }

    override fun trn(text: String, plural: String, n: Long): String {
        return text
    }

    override fun trn(
        text: String,
        plural: String,
        n: Int,
        vararg args: Pair<String, String>
    ): String {
        return text
    }

    override fun trn(
        text: String,
        plural: String,
        n: Long,
        vararg args: Pair<String, String>
    ): String {
        return text
    }

    override fun trc(context: String, text: String): String {
        return text
    }

    override fun trc(
        context: String,
        text: String,
        vararg args: Pair<String, String>
    ): String {
        return text
    }

    override fun trnc(
        context: String,
        text: String,
        plural: String,
        n: Int
    ): String {
        return text
    }

    override fun trnc(
        context: String,
        text: String,
        plural: String,
        n: Long
    ): String {
        return text
    }

    override fun trnc(
        context: String,
        text: String,
        plural: String,
        n: Int,
        vararg args: Pair<String, String>
    ): String {
        return text
    }

    override fun trnc(
        context: String,
        text: String,
        plural: String,
        n: Long,
        vararg args: Pair<String, String>
    ): String {
        return text
    }

    override fun marktr(text: String): String {
        return text
    }
}