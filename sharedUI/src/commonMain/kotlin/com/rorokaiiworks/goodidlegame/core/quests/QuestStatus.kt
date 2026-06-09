package com.rorokaiiworks.goodidlegame.core.quests

import kotlinx.serialization.Serializable

@Serializable
enum class QuestStatus {
    InProgress,
    Completed,
    RewardClaimed
}
