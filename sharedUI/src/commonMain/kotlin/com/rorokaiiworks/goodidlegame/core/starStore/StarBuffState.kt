package com.rorokaiiworks.goodidlegame.core.starStore

import kotlinx.serialization.Serializable

@Serializable
data class StarBuffState(
    val purchased: List<StarStoreItemSaveData> = emptyList(),
)
