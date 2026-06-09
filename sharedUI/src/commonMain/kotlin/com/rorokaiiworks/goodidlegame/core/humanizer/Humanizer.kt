package com.rorokaiiworks.goodidlegame.core.humanizer

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object Humanizer {
    @Composable
    private fun formatDuration(
        duration: Duration,
        i18n: I18n = koinInject()
    ): String {
        val absDuration = duration.absoluteValue

        val seconds = absDuration.inWholeMilliseconds / 1000.0
        val minutes = absDuration.inWholeSeconds / 60.0
        val hours = absDuration.inWholeMinutes / 60.0
        val days = absDuration.inWholeHours / 24.0

        return when {
            seconds < 60 -> {
                formatValue(seconds, i18n.tr("%s secs"))
            }
            minutes < 60 -> {
                formatValue(minutes, i18n.tr("%s mins"))
            }
            hours < 24 -> {
                formatValue(hours, i18n.tr("%s hours"))
            }
            days < 30 -> {
                if (days < 7) {
                    formatValue(days, i18n.tr("%s days"))
                } else {
                    formatValue(days / 7.0, i18n.tr("%s weeks"))
                }
            }
            days < 365 -> {
                formatValue(days / 30.4375, i18n.tr("%s months"))
            }
            else -> {
                formatValue(days / 365.25, i18n.tr("%s years"))
            }
        }
    }

    private val decimal = DecimalFormat("#.#")
    private fun formatValue(value: Double, pattern: String): String {
        val formattedNumber = if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            decimal.format(value)
        }
        return pattern.replace("%s", formattedNumber)
    }


    private val customFormat = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        day()
        char(' ')
        hour()
        char(':')
        minute()
    }

    @OptIn(ExperimentalTime::class)
    fun instant(instant: Instant): String {
        val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        return localDate.format(customFormat)
    }

    @Composable
    fun duration(duration: Duration): String {
        return formatDuration(duration)
    }

    fun abbreviation(number: Number, decimals: Int = 2): String {
        return formatAbbreviation(number.toDouble(), decimals)
    }

    internal fun formatAbbreviation(number: Double, decimals: Int): String {
        var current = number
        var index = 0

        while (current >= 1000 && index < prefixes.size - 1) {
            current /= 1000
            index++
        }

        return "${current.formatNumber(decimals)}${prefixes[index]}"
    }

    internal fun Double.formatNumber(
        decimals: Int
    ): String {
        val groupSeparator = ",".formatWithSpaceIfNeeded()
        val decimalSymbol = "."
        val rounded = formatWithDecimals(decimals)
        val parts = rounded.split('.')

        // Format the integer part
        val formattedIntegerPart = parts[0]
            .reversed()
            .chunked(3)
            .joinToString(groupSeparator)
            .reversed()

        // Format the decimal part with trailing zeros removed
        val decimalPart = if (parts.size > 1) parts[1] else ""
        val formattedDecimalPart = if (decimals > 0) {
            val truncatedDecimals = decimalPart.padEnd(decimals, '0').substring(0, decimals)
            val trimmedDecimals = truncatedDecimals.trimEnd('0')
            if (trimmedDecimals.isEmpty()) "" else decimalSymbol + trimmedDecimals
        } else {
            ""
        }

        return formattedIntegerPart + formattedDecimalPart
    }

    private fun Double.formatWithDecimals(decimals: Int): String {
        val multiplier = 10.0.pow(decimals)
        val numberAsString = (this * multiplier).roundToLong().toString().padStart(decimals + 1, '0')
        val decimalIndex = numberAsString.length - decimals - 1
        val mainRes = numberAsString.substring(0..decimalIndex)
        val fractionRes = numberAsString.substring(decimalIndex + 1)
        return if (fractionRes.isEmpty()) {
            mainRes
        } else {
            "$mainRes.$fractionRes"
        }
    }


    private val prefixes = arrayOf("", "K", "M", "B", "T")

    private fun String.formatWithSpaceIfNeeded(): String {
        return ifEmpty { " " }
    }
}