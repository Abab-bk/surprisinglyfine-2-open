package com.rorokaiiworks.goodidlegame.core.mastery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class MasteryLevel : IPersistable, KoinComponent {
    private val eventBus: EventBus by inject()

    private val scope = CoroutineScope(Dispatchers.Default)

    var totalXp: Int = 0
        private set

    var level: Int by mutableIntStateOf(0)
        private set

    var currentXp: Int by mutableIntStateOf(0)
        private set

    var maxXp: Int by mutableIntStateOf(100)
        private set

    fun addXp(xp: Int) {
        currentXp += xp
        totalXp += xp

        while (currentXp >= maxXp) {
            currentXp -= maxXp

            level++

            scope.launch {
                eventBus.emit(IEvent.MasteryLevelUp(level))
            }

            maxXp = calculateMaxXp(level)
        }
    }

    private fun calculateMaxXp(level: Int): Int {
        return 100 * (level + 1) * (level + 1)
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.masteryLevelSaveData = MasteryLevelSaveData(level, currentXp, totalXp)
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        gameSave.masteryLevelSaveData?.let {
            level = it.level
            currentXp = it.currentXp
            totalXp = it.totalXp
            maxXp = calculateMaxXp(it.level)
        }
    }
}


@Serializable
data class MasteryLevelSaveData(
    val level: Int,
    val currentXp: Int,
    val totalXp: Int,
)


@Serializable
data class MasteryLevelReward(
    val data: Map<Int, List<StatModifier>>
)
