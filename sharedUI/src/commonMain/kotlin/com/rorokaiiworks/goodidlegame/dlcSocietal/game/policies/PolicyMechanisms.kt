package com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies

import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.Building
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate

enum class PolicyModifierType {
    ProductSpeed,
    MaintenanceCosts,
    WorkforceDemand,
    BuildBuildingCosts,
    HousingCapacity,
    SaturationDecaySpeed,
}

interface PolicyMechanism {
    val mechanismId: String

    fun onActivated() {}

    fun onDeactivated() {}

    fun onBuildingTick(building: Building) {}

    fun onBuildingPeriod(
        building: Building,
        costs: MutableList<ItemEntry>,
        yields: MutableList<ItemEntry>,
    ) { }

    fun getModifier(type: PolicyModifierType, template: BuildingTemplate?): Float = 0f
}

class PolicyMechanismRegistry(
    mechanisms: List<PolicyMechanism>,
) {
    private val byId: Map<String, PolicyMechanism> = mechanisms.associateBy { it.mechanismId }

    fun findOrNull(mechanismId: String): PolicyMechanism? = byId[mechanismId]
}


