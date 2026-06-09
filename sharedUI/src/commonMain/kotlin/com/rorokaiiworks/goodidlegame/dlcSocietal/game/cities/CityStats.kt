package com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities

import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.Building
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingState
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingType
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations.PopulationPool
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations.PopulationSource
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations.PopulationTier

fun emptyCityStats() : CityStats = CityStats(
    isleBucks = 0,
    balance = 0,
    settlementPeriodSeconds = 0f,
    secondsUntilSettlement = 0,
    populationByTier = emptyMap(),
    populationSources = emptyMap(),
    totalPopulation = 0,
    workforceByTier = emptyMap(),
    buildings = emptyList(),
)

data class CityStats(
    val isleBucks: Long,
    val balance: Long,

    val settlementPeriodSeconds: Float,
    val secondsUntilSettlement: Int,

    val populationByTier: Map<PopulationTier, PopulationTierStats>,
    val populationSources: Map<PopulationTier, List<PopulationSource>>,
    val totalPopulation: Int,

    val workforceByTier: Map<PopulationTier, WorkforceTierStats>,

    val buildings: List<BuildingStats>,
)

data class PopulationTierStats(
    val tier: PopulationTier,
    val current: Int,
)

data class BuildingStats(
    val id: String,
    val iconId: String,
    val count: Int,
    val activeCount: Int,
    val inactiveCount: Int,
    val buildingType: BuildingType,
    val currentState: BuildingState,
    val tickProgress: Float,
    val productivity: Float,
    val balanceMaintenance: Long,
    val current: Int?,
)

internal fun buildCityStats(
    cityInventory: CityInventory,
    populations: Map<PopulationTier, PopulationPool>,
    workforceByTier: Map<PopulationTier, WorkforceTierStats>,
    buildings: Collection<Building>,
    balance: Long,
    settlementPeriodSeconds: Float,
    secondsUntilSettlement: Int,
): CityStats {
    val populationByTier = populations.mapValues { (_, pool) ->
        PopulationTierStats(
            tier = pool.tier,
            current = pool.current,
        )
    }

    val buildingStats = mutableListOf<BuildingStats>()

    buildings.forEach { b ->
        val isResidential = b.template.buildingType == BuildingType.Residences
        val maintenance = b.totalMaintenanceBalance

        buildingStats += BuildingStats(
            id = b.template.id,
            iconId = b.template.getIconId(),
            count = b.count,
            activeCount = b.activeCount,
            inactiveCount = b.inactiveCount,
            buildingType = b.template.buildingType,
            currentState = b.currentState,
            tickProgress = b.tickProgress,
            productivity = b.productivity,
            balanceMaintenance = maintenance,
            current = if (isResidential) b.getHousingCapacity() else null,
        )
    }

    return CityStats(
        isleBucks = cityInventory.isleBucks,
        balance = balance,
        settlementPeriodSeconds = settlementPeriodSeconds,
        secondsUntilSettlement = secondsUntilSettlement,
        populationByTier = populationByTier,
        populationSources = populations.mapValues { (_, pool) -> pool.sources.values.toList() },
        totalPopulation = populationByTier.values.sumOf { it.current },
        workforceByTier = workforceByTier,
        buildings = buildingStats,
    )
}
