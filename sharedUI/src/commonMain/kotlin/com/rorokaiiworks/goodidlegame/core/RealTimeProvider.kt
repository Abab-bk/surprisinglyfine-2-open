package com.rorokaiiworks.goodidlegame.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

class RealTimeProvider : ITimeProvider {
    override val timeZone: TimeZone = TimeZone.currentSystemDefault()

    override val minuteTicker = MutableSharedFlow<Instant>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val now = Clock.System.now()
                minuteTicker.emit(now)

                val nextMinute = (now.toEpochMilliseconds() / 60000 + 1) * 60000
                val delayTime = nextMinute - Clock.System.now().toEpochMilliseconds()

                if (delayTime > 0) {
                    delay(delayTime)
                }
            }
        }
    }

    override fun now(): Instant = Clock.System.now()
    override fun nowMillis(): Long = now().toEpochMilliseconds()

    override fun localDateTime(): LocalDateTime = now().toLocalDateTime(timeZone)
}