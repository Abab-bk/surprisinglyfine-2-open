package com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.koin.core.component.KoinComponent

class PopulationPool(val tier: PopulationTier) : KoinComponent {
    val sources = mutableMapOf<String, PopulationSource>()

    var current by mutableIntStateOf(0)
        private set

    fun setSource(buildingId: String, buildingName: String, capacity: Int, iconId: String) {
        sources[buildingId] = PopulationSource(buildingId, buildingName, capacity, iconId)
        current = calculateCurrent()
    }

    fun removeSource(buildingId: String) {
        sources.remove(buildingId)
        current = calculateCurrent()
    }

    fun calculateCurrent(): Int {
        return sources.entries.sumOf { it.value.count }
    }
}
