package com.rorokaiiworks.goodidlegame.dlcSocietal.game

import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTier
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.WorkforceItem
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations.PopulationTier
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object CityFormulas {
    fun calculateCityItemPrice(tier: Int, populationTier: PopulationTier): Int {
        val baseTier = when (populationTier) {
            PopulationTier.Farmer -> 0
            PopulationTier.Worker -> 1
            PopulationTier.Astrologer -> 2
            PopulationTier.Alchemist -> 3
        }

        val localTier = tier.coerceAtLeast(1)
        val effectiveTier = localTier + baseTier * 2

        val basePrice = when (populationTier) {
            PopulationTier.Farmer -> 45f
            PopulationTier.Worker -> 70f
            PopulationTier.Astrologer -> 120f
            PopulationTier.Alchemist -> 200f
        }
        val tierOffset = (localTier - 1) * 12f
        return ((basePrice + tierOffset) * 1.35.pow(effectiveTier - 1)).roundToInt().coerceAtLeast(15)
    }

    fun calculateBuildingPeriod(
        dependencyCount: Int,
        dependedByCount: Int,
        buildingTier: BuildingTier,
        yields: List<ItemEntry>
    ): Float {
        val complexity = calculateComplexityScore(
            dependencyCount = dependencyCount,
            dependedByCount = dependedByCount
        )
        val base = when (buildingTier) {
            BuildingTier.Farmer -> 2.2f
            BuildingTier.Worker -> 3.0f
            BuildingTier.Astrologer -> 4.0f
            BuildingTier.Alchemist -> 5.5f
        }
        val productionCount = yields.sumOf { it.count.coerceAtLeast(0) }
        val yieldScore = sqrt(productionCount.toFloat().coerceAtLeast(1f))
        val rawPeriod = base + yieldScore * 0.8f + complexity * 0.25f
        return ((rawPeriod.coerceIn(2.0f, 12f)) * 10f)
            .roundToInt() / 10f
    }

    fun calculateMaintenanceBalance(dependencyCount: Int, dependedByCount: Int, buildingTier: BuildingTier): Int {
        val complexity = calculateComplexityScore(
            dependencyCount = dependencyCount,
            dependedByCount = dependedByCount
        )
        val tierBase = when (buildingTier) {
            BuildingTier.Farmer -> 4f
            BuildingTier.Worker -> 8f
            BuildingTier.Astrologer -> 14f
            BuildingTier.Alchemist -> 22f
        }
        return (tierBase + complexity * 2.2f)
            .roundToInt()
            .coerceAtLeast(1)
    }

    fun calculateMaintenanceWorkforce(dependencyCount: Int, dependedByCount: Int, buildingTier: BuildingTier): List<WorkforceItem> {
        val complexity = calculateComplexityScore(
            dependencyCount = dependencyCount,
            dependedByCount = dependedByCount
        )
        val primaryDemand = when (buildingTier) {
            BuildingTier.Farmer -> 1.5f
            BuildingTier.Worker -> 2.5f
            BuildingTier.Astrologer -> 3.5f
            BuildingTier.Alchemist -> 5.0f
        } + complexity * 0.7f

        val primary = primaryDemand.roundToInt().coerceAtLeast(1)

        return when (buildingTier) {
            BuildingTier.Farmer -> listOf(
                WorkforceItem(tier = PopulationTier.Farmer, count = primary)
            )

            BuildingTier.Worker -> listOf(
                WorkforceItem(tier = PopulationTier.Worker, count = primary),
            )

            BuildingTier.Astrologer -> listOf(
                WorkforceItem(tier = PopulationTier.Astrologer, count = primary),
            )

            BuildingTier.Alchemist -> listOf(
                WorkforceItem(tier = PopulationTier.Alchemist, count = primary),
            )
        }
    }

    fun calculatePeriodYields(
        yields: List<ItemEntry>,
        dependencyCount: Int,
        dependedByCount: Int,
        buildingTier: BuildingTier
    ): List<ItemEntry> {
        val tierMultiplier = when (buildingTier) {
            BuildingTier.Farmer     -> 1.0f
            BuildingTier.Worker     -> 1.35f
            BuildingTier.Astrologer -> 1.7f
            BuildingTier.Alchemist  -> 2.1f
        }
        val demandBonus = (dependedByCount.coerceAtLeast(0) * 0.06f).coerceAtMost(0.3f)
        val multiplier = tierMultiplier * (1f + demandBonus)

        val tierBaseIncome = when (buildingTier) {
            BuildingTier.Farmer     -> 8
            BuildingTier.Worker     -> 18
            BuildingTier.Astrologer -> 40
            BuildingTier.Alchemist  -> 80
        }
        val complexity = calculateComplexityScore(dependencyCount, dependedByCount)
        val incomeAmount = (tierBaseIncome * (1f + complexity * 0.12f)).roundToLong().coerceAtLeast(1)

        return yields.map { entry ->
            if (entry.itemId == "isle_bucks") entry.copy(count = incomeAmount)
            else entry.copy(count = (entry.count * multiplier).roundToLong().coerceAtLeast(1))
        }
    }

    fun calculatePeriodCosts(
        costs: List<ItemEntry>,
        dependencyCount: Int,
        dependedByCount: Int,
        buildingTier: BuildingTier
    ): List<ItemEntry> {
        val tierMultiplier = when (buildingTier) {
            BuildingTier.Farmer     -> 1.0f
            BuildingTier.Worker     -> 1.15f
            BuildingTier.Astrologer -> 1.35f
            BuildingTier.Alchemist  -> 1.6f
        }
        val complexity = calculateComplexityScore(dependencyCount, dependedByCount)
        val multiplier = tierMultiplier * (1f + complexity * 0.05f)
        return costs.map { entry ->
            if (entry.itemId == "isle_bucks") entry
            else entry.copy(count = (entry.count * multiplier).roundToLong().coerceAtLeast(1))
        }
    }

    fun calculateBuildCosts(
        costs: List<ItemEntry>,
        dependencyCount: Int,
        dependedByCount: Int,
        buildingTier: BuildingTier
    ): List<ItemEntry> {
        val tierBase = when (buildingTier) {
            BuildingTier.Farmer     -> 1.0f
            BuildingTier.Worker     -> 1.35f
            BuildingTier.Astrologer -> 1.8f
            BuildingTier.Alchemist  -> 2.4f
        }
        val complexity = calculateComplexityScore(dependencyCount, dependedByCount)
        val multiplier = tierBase * (1f + complexity * 0.08f)

        val tierBaseCost = when (buildingTier) {
            BuildingTier.Farmer     -> 750
            BuildingTier.Worker     -> 2500
            BuildingTier.Astrologer -> 7000
            BuildingTier.Alchemist  -> 20000
        }
        val costAmount = (tierBaseCost * (1f + complexity * 0.12f)).roundToLong().coerceAtLeast(1)

        return costs.map { entry ->
            if (entry.itemId == "isle_bucks") entry.copy(count = costAmount)
            else entry.copy(count = (entry.count * multiplier).roundToLong().coerceAtLeast(1))
        }
    }

    private fun calculateComplexityScore(dependencyCount: Int, dependedByCount: Int): Float {
        val dCount = dependencyCount.coerceAtLeast(0)
        val dbCount = dependedByCount.coerceAtLeast(0)

        return (dCount * 1.6f) + (dbCount * 0.9f)
    }

    // City Port
    fun calculatePortCapacityLevelUpCosts(level: Int): List<ItemEntry> {
        val clampedLevel = level.coerceAtLeast(1)
        val cost = (150f * clampedLevel.toFloat().pow(1.7f)).roundToLong().coerceAtLeast(1)
        return listOf(ItemEntry("isle_bucks", cost))
    }

    fun calculatePortTradeIntervalLevelUpCosts(level: Int): List<ItemEntry> {
        val clampedLevel = level.coerceAtLeast(1)
        val cost = (165f * clampedLevel.toFloat().pow(1.75f)).roundToLong().coerceAtLeast(1)
        return listOf(ItemEntry("isle_bucks", cost))
    }

    fun calculatePortSaturationSpeedLevelUpCosts(level: Int): List<ItemEntry> {
        val clampedLevel = level.coerceAtLeast(1)
        val cost = (140f * clampedLevel.toFloat().pow(1.65f)).roundToLong().coerceAtLeast(1)
        return listOf(ItemEntry("isle_bucks", cost))
    }

    fun calculatePortCapacityForLevel(level: Int): Long {
        val clampedLevel = level.coerceAtLeast(1)
        return (40L + clampedLevel * 30)
    }

    fun calculatePortTradeIntervalForLevel(level: Int): Duration {
        val clampedLevel = level.coerceAtLeast(1)
        val baseSeconds = 6.minutes.inWholeSeconds.toFloat()
        val intervalSeconds = (baseSeconds * 0.92f.pow(clampedLevel - 1)).coerceAtLeast(45f)
        return intervalSeconds.toDouble().seconds
    }

    fun calculatePortSaturationIncreasePerItemForLevel(level: Int): Float {
        val clampedLevel = level.coerceAtLeast(1)
        val base = 0.010f
        return (base * 0.93f.pow(clampedLevel - 1)).coerceAtLeast(0.0025f)
    }

    fun calculatePortSaturationDecayPerSecondForLevel(level: Int): Float {
        val clampedLevel = level.coerceAtLeast(1)
        val base = 0.0020f
        return (base * 1.10f.pow(clampedLevel - 1)).coerceAtMost(0.050f)
    }

    fun getInitialItems(buildingTemplates: DataTable<BuildingTemplate>): List<ItemEntry> {
        val farmerBuildingCost = buildingTemplates
            .find("farmer_residences")
            .buildCosts.first { it.itemId == "isle_bucks" }.count

        val treeFarmerBuildingCost = buildingTemplates
            .find("farmer_tree_farm")
            .buildCosts.first { it.itemId == "isle_bucks" }.count

        val sawMillBuildingCost = buildingTemplates
            .find("farmer_sawmill")
            .buildCosts.first { it.itemId == "isle_bucks" }.count

        val farmerBuildingPlank = buildingTemplates
            .find("farmer_residences")
            .buildCosts.first { it.itemId == "plank" }.count

        val initialIsleBucks = (farmerBuildingCost * 5 + treeFarmerBuildingCost + sawMillBuildingCost) * 3

        return listOf(
            ItemEntry("isle_bucks", initialIsleBucks),
            ItemEntry("plank", farmerBuildingPlank * 5),
        )
    }

    fun calculateItemGreatTokenTargetProgress(itemTemplate: ItemTemplate): Long {
        return 500000L / itemTemplate.price
    }
}
