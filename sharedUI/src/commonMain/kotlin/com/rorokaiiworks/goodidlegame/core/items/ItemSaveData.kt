package com.rorokaiiworks.goodidlegame.core.items

import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import kotlinx.serialization.Serializable

@Serializable
data class ItemSaveData(
    val itemId: String,
    val count: Long,
    val customModifiers: List<StatModifier> = emptyList(),
    val enchantmentLevel: Int = 0,
)