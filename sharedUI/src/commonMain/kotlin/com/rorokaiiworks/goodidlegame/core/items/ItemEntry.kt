package com.rorokaiiworks.goodidlegame.core.items

import kotlinx.serialization.Serializable

@Serializable
data class ItemEntry(
    val itemId: String,
    val count: Long
)
