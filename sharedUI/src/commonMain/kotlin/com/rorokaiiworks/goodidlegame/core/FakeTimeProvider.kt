@file:OptIn(ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class FakeTimeProvider(
    initialTime: Instant = Clock.System.now(),
    override val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ITimeProvider {
    var realModel by mutableStateOf(true)

    override val minuteTicker = MutableSharedFlow<Instant>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    var nowTime: Instant = initialTime
        private set

    override fun now(): Instant {
        if (!realModel) return nowTime
        return Clock.System.now()
    }

    override fun nowMillis(): Long {
        if (!realModel) return nowTime.toEpochMilliseconds()
        return Clock.System.now().toEpochMilliseconds()
    }

    fun setTime(instant: Instant) {
        nowTime = instant
        minuteTicker.tryEmit(nowTime)
    }

    fun advance(duration: Duration) {
        nowTime += duration
        minuteTicker.tryEmit(nowTime)
    }

    fun advanceDays(days: Int) {
        advance(days.days)
    }

    override fun localDateTime(): LocalDateTime = nowTime.toLocalDateTime(timeZone)

    init {
        minuteTicker.tryEmit(nowTime)
    }
}
