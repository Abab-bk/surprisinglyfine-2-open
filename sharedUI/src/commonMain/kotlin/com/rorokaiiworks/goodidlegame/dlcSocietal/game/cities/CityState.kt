package com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities

import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.Building
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations.PopulationPool
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations.PopulationTier

data class WorkforceTierStats(
    val tier: PopulationTier,
    val totalSupply: Int,
    val totalDemand: Int,
    val satisfaction: Float,
    val supplies: PopulationPool?,
    val demands: List<WorkforceInfo>
)

data class WorkforceInfo(
    val buildingId: String,
    val count: Int
)

class CityState {
    var tierStats: Map<PopulationTier, WorkforceTierStats> = mapOf()

    val populations: Map<PopulationTier, PopulationPool> =
        PopulationTier.entries.associateWith { tier -> PopulationPool(tier) }

    fun tick(buildings: List<Building>) {
        val (stats, satisfactionPerTier) = computeWorkforceStats(buildings)
        applyProductivity(buildings, satisfactionPerTier)
        tierStats = stats
    }

    private fun computeWorkforceStats(
        buildings: List<Building>,
    ): Pair<Map<PopulationTier, WorkforceTierStats>, Map<PopulationTier, Float>> {
        val demandCounts = mutableMapOf<PopulationTier, Int>()
        val demandSources = mutableMapOf<PopulationTier, MutableList<WorkforceInfo>>()

        buildings.forEach { building ->
            building.template.maintenanceWorkforce.forEach { maintenance ->
                val tier = maintenance.tier
                val demand = building.getWorkforceDemand(tier)
                if (demand <= 0) return@forEach

                demandCounts[tier] = (demandCounts[tier] ?: 0) + demand
                demandSources.getOrPut(tier) { mutableListOf() }.add(
                    WorkforceInfo(buildingId = building.template.id, count = demand)
                )
            }
        }

        val stats = PopulationTier.entries.associateWith { tier ->
            val totalSupply = populations[tier]?.current ?: 0
            val totalDemand = demandCounts[tier] ?: 0
            val satisfaction = if (totalDemand <= 0) 1f
            else (totalSupply.toFloat() / totalDemand).coerceAtMost(1f)

            WorkforceTierStats(
                tier = tier,
                totalSupply = totalSupply,
                totalDemand = totalDemand,
                satisfaction = satisfaction,
                supplies = populations[tier],
                demands = demandSources[tier] ?: emptyList(),
            )
        }

        val satisfactionPerTier = stats.mapValues { (_, s) -> s.satisfaction }
        return stats to satisfactionPerTier
    }

    private fun applyProductivity(
        buildings: List<Building>,
        satisfactionPerTier: Map<PopulationTier, Float>,
    ) {
        buildings.forEach { building ->
            val maintenance = building.template.maintenanceWorkforce
            if (maintenance.isEmpty()) {
                building.productivity = 1f
                return@forEach
            }

            var minSatisfaction = 1f
            for (m in maintenance) {
                val s = satisfactionPerTier[m.tier] ?: 1f
                if (s < minSatisfaction) minSatisfaction = s
                if (minSatisfaction <= 0f) break
            }

            building.productivity = minSatisfaction
        }
    }
}
