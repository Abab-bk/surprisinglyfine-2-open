@file:OptIn(ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.core

import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.time.ExperimentalTime

class TimeSystem : KoinComponent, IPersistable {
    private val logger: Logger by inject { parametersOf("TimeSystem") }
    private val timeProvider: ITimeProvider by inject()
    private val eventBus: EventBus by inject()
    private var lastDate: LocalDate? = null

    fun tick() {
        val now = timeProvider.now()
        val todayTime = now.toLocalDateTime(timeProvider.timeZone)
        val today = todayTime.date

        if (lastDate != null && (lastDate as LocalDate).daysUntil(today) >= 1) {
            eventBus.tryEmit(IEvent.NewDayEvent(today))
            logger.i { "New day event for $todayTime" }
        }

        lastDate = today
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.timeSystemSave = TimeSystemSave(
            lastDate = lastDate,
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        lastDate = gameSave.timeSystemSave.lastDate
    }
}

@Serializable
data class TimeSystemSave(
    val lastDate: LocalDate? = null,
)
