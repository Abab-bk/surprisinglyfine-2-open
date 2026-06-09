package com.rorokaiiworks.goodidlegame.core.starStore

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
@OptIn(ExperimentalTime::class)
data class StarStoreItemSaveData(
    val id: String,
    val purchasedAt: Instant?,
)
