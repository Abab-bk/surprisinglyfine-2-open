package com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations


data class PopulationSource(
    val buildingId: String,
    val buildingName: String,
    val count: Int,      // How many people *can* live here (set by building count)
    val iconId: String,
)