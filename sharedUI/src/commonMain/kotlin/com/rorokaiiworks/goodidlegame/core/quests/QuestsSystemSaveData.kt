@file:OptIn(ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.core.quests

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class QuestsSystemSaveData(
    val dailyQuestSeed: Long,
    val dailyQuestsProgress: List<QuestProgress> = emptyList(),
)

@Serializable
data class QuestProgress(
    val status: QuestStatus,
    val conditionCounts: List<Long>
)