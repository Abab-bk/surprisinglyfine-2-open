package com.rorokaiiworks.goodidlegame.core.community

import kotlinx.serialization.Serializable

@Serializable
data class CommunitySaveData(
    val enchantingTable: EnchantingTableSaveData = EnchantingTableSaveData(),
    val square: SquareSaveData = SquareSaveData(),
    val altar: AltarSaveData = AltarSaveData(),
)

