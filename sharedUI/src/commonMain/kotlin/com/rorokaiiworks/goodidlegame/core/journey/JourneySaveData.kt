package com.rorokaiiworks.goodidlegame.core.journey

import com.rorokaiiworks.goodidlegame.core.quests.QuestProgress
import kotlinx.serialization.Serializable

@Serializable
data class JourneySaveData(
    val currentQuestId: String?,
    val progress: QuestProgress?
)