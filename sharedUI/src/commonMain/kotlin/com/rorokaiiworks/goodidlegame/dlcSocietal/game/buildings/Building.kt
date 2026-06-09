package com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings

import androidx.compose.runtime.*
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTier.Companion.toPopulationTier
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityState
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicyModifierType
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicySystem
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations.PopulationTier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.roundToInt
import kotlin.math.roundToLong


enum class BuildingState {
    Normal,
    PeriodCostsNotEnough,
    WorkforceNotEnough,
}

class Building(
    val template: BuildingTemplate,
    initialCount: Int,
) : KoinComponent {
    private val cityInventory: CityInventory by inject()
    private val policySystem: PolicySystem by inject()
    private val cityState: CityState by inject()

    var count by mutableIntStateOf(initialCount)
        private set

    var inactiveCount by mutableIntStateOf(0)
        private set

    val activeCount: Int
        get() = (count - inactiveCount).coerceAtLeast(0)

    var tickProgress by mutableFloatStateOf(0f)

    var currentState by mutableStateOf(BuildingState.Normal)
        private set

    var productivity: Float by mutableFloatStateOf(1f)

    val totalMaintenanceBalance
        get() = (
                template.maintenanceBalance
                    .toFloat() *
                        activeCount *
                        policyMultiplier(PolicyModifierType.MaintenanceCosts)
                ).roundToLong()

    fun getWorkforceDemand(tier: PopulationTier): Int =
        template.maintenanceWorkforce.find { it.tier == tier }?.count?.let { baseCount ->
            (
                    baseCount.toFloat() *
                            activeCount *
                            policyMultiplier(PolicyModifierType.WorkforceDemand)
                    ).roundToInt()
        } ?: 0

    private var timeElapsed = 0f

    private var cachedTotalCosts: List<ItemEntry> = emptyList()
    private var cachedTotalYields: List<ItemEntry> = emptyList()

    private var lastNeedsPopulation: Int = -1

    init {
        updateCaches()
        publishHousingCapacity()
    }

    fun addCount(amount: Int) {
        count += amount
        updateCaches()
        publishHousingCapacity()
    }

    fun updateInactiveCount(value: Int) {
        val clamped = value.coerceIn(0, count)
        if (inactiveCount == clamped) return
        inactiveCount = clamped
        updateCaches()
        publishHousingCapacity()
    }

    fun tick(delta: Float) {
        refreshNeedsIfPopulationChanged()
        policySystem.onBuildingTick(building = this)

        if (template.periodYields.isEmpty()) return

        if (activeCount <= 0) {
            currentState = BuildingState.Normal
            tickProgress = 0f
            return
        }

        val nextNormalState = if (productivity <= 0f) BuildingState.WorkforceNotEnough else BuildingState.Normal

        when (currentState) {
            BuildingState.WorkforceNotEnough,
            BuildingState.Normal -> {
                val elapsedToAdd = if (productivity > 0f) {
                    delta * productivity * policyMultiplier(PolicyModifierType.ProductSpeed)
                } else {
                    0f
                }

                val effectiveElapsed = (timeElapsed + elapsedToAdd).coerceAtLeast(0f)
                val completedPeriods = (effectiveElapsed / template.period).toLong().coerceAtLeast(0)

                if (completedPeriods <= 0) {
                    currentState = nextNormalState
                    timeElapsed = effectiveElapsed
                    tickProgress = (timeElapsed / template.period).coerceAtMost(1f)
                    return
                }

                val transaction = policySystem.applyBuildingPeriodMechanisms(
                    building = this,
                    baseCosts = cachedTotalCosts,
                    baseYields = cachedTotalYields,
                )

                val affordablePeriods = maxAffordablePeriods(transaction.costs, completedPeriods)
                if (affordablePeriods <= 0) {
                    currentState = BuildingState.PeriodCostsNotEnough
                    timeElapsed = template.period
                    tickProgress = 1f
                    return
                }

                val totalCosts = scaleEntries(transaction.costs, affordablePeriods)
                val totalYields = scaleEntries(transaction.yields, affordablePeriods)

                if (!cityInventory.inventory.canConsume(totalCosts)) {
                    currentState = BuildingState.PeriodCostsNotEnough
                    timeElapsed = template.period
                    tickProgress = 1f
                    return
                }

                cityInventory.inventory.removeItems(totalCosts)
                cityInventory.inventory.addItemEntries(totalYields)

                if (affordablePeriods < completedPeriods) {
                    currentState = BuildingState.PeriodCostsNotEnough
                    timeElapsed = template.period
                    tickProgress = 1f
                    return
                }

                currentState = nextNormalState
                timeElapsed = (effectiveElapsed - (affordablePeriods * template.period)).coerceIn(0f, template.period)
                tickProgress = (timeElapsed / template.period).coerceAtMost(1f)
            }

            BuildingState.PeriodCostsNotEnough -> {
                if (cityInventory.inventory.canConsume(cachedTotalCosts)) {
                    currentState = BuildingState.Normal
                }
            }
        }
    }

    private fun maxAffordablePeriods(costs: List<ItemEntry>, maxPeriods: Long): Long {
        if (costs.isEmpty()) return maxPeriods

        var minAffordable = maxPeriods
        costs.forEach { cost ->
            if (cost.count <= 0) return@forEach
            val available = cityInventory.inventory.findItem(cost.itemId)?.count ?: 0
            val affordable = available / cost.count
            if (affordable < minAffordable) minAffordable = affordable
            if (minAffordable <= 0) return 0
        }
        return minAffordable.coerceIn(0, maxPeriods)
    }

    private fun scaleEntries(entries: List<ItemEntry>, times: Long): List<ItemEntry> {
        if (times <= 1) return entries
        if (entries.isEmpty()) return entries

        val multiplier = times.toLong()
        return entries.mapNotNull { entry ->
            if (entry.count <= 0) return@mapNotNull null
            val scaled = (entry.count * multiplier).coerceAtMost(Long.MAX_VALUE)
            if (scaled <= 0) return@mapNotNull null
            entry.copy(count = scaled)
        }
    }

    private fun updateCaches() {
        cachedTotalCosts = template.periodCosts.map { it.copy(count = it.count * activeCount) }
        cachedTotalYields = template.periodYields.map { it.copy(count = it.count * activeCount) }

        if (template.buildingType == BuildingType.Residences) {
            val currentPopulation = getCurrentTierPopulation()
            lastNeedsPopulation = currentPopulation
        }
    }

    private fun refreshNeedsIfPopulationChanged() {
        if (template.buildingType != BuildingType.Residences) return

        val currentPopulation = getCurrentTierPopulation()
        if (currentPopulation == lastNeedsPopulation) return

        lastNeedsPopulation = currentPopulation
        publishHousingCapacity()
    }

    private fun getCurrentTierPopulation(): Int =
        cityState.populations[template.buildingTier.toPopulationTier()]?.current ?: 0

    private fun publishHousingCapacity() {
        if (template.buildingType != BuildingType.Residences) return

        val pool = cityState.populations[template.buildingTier.toPopulationTier()] ?: return
        pool.setSource(
            buildingId = template.id,
            buildingName = template.name,
            capacity = getHousingCapacity(),
            iconId = template.id,
        )
    }

    fun getHousingCapacity(): Int {
        if (template.buildingType != BuildingType.Residences) return 0
        return (activeCount.toFloat() * policyMultiplier(PolicyModifierType.HousingCapacity)).roundToInt()
    }

    private fun policyMultiplier(type: PolicyModifierType): Float =
        (1f + policySystem.getModifier(type, template)).coerceAtLeast(0f)
}
