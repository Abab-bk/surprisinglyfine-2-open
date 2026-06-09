package com.rorokaiiworks.goodidlegame.core

import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshots.SnapshotStateSet
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.Serializable

class GameState : IPersistable {
    val finishedQuests: MutableSet<String> = mutableSetOf()
    val unlockedSkills: SnapshotStateSet<String> = mutableStateSetOf(
        "skill_woodcutting"
    )

    private val _unlockEvents = Channel<String>(Channel.BUFFERED)
    val unlockEvents = _unlockEvents.receiveAsFlow()

    private var playTime: Long = 0L

    fun tick(delta: Int) {
        playTime += delta
    }

    fun unlockSkill(skillId: String) {
        if (unlockedSkills.add(skillId)) {
            _unlockEvents.trySend(skillId)
        }
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.gameState = GameStateSaveData(
            finishedQuests = finishedQuests.toSet(),
            unlockedSkills = unlockedSkills.toSet(),
            playTime = playTime,
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        playTime = gameSave.gameState.playTime

        finishedQuests.clear()
        unlockedSkills.clear()

        finishedQuests.addAll(gameSave.gameState.finishedQuests)
        unlockedSkills.addAll(gameSave.gameState.unlockedSkills)
    }
}


@Serializable
data class GameStateSaveData(
    val finishedQuests: Set<String> = emptySet(),
    val unlockedSkills: Set<String> = setOf("skill_woodcutting"),
    val playTime: Long = 0L,
)
