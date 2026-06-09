package com.rorokaiiworks.goodidlegame.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface ITimeProvider {
    val timeZone: TimeZone
    val minuteTicker: MutableSharedFlow<Instant>

    @OptIn(ExperimentalTime::class)
    fun now(): Instant

    fun nowMillis(): Long

    fun localDateTime(): LocalDateTime
}