package com.rorokaiiworks.goodidlegame.core.persistent

import kotlinx.serialization.Serializable

@Serializable
enum class GameSaveVersion {
    V1_LEGACY_COMMUNITY,
    V2_COMMUNITY_REWORK,
}
