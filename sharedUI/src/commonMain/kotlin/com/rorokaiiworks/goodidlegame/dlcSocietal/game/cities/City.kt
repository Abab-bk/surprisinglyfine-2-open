package com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.*
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.balances.Balance
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.Building
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicyModifierType
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicySaveData
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicySystem
import com.rorokaiiworks.goodidlegame.ui.NotificationType
import com.rorokaiiworks.goodidlegame.ui.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.math.ceil


@Serializable
data class CityBuildingSaveData(
    val id: String,
    val count: Int
)


@Serializable
data class CitySaveData(
    val finishedWelcomeTutorial: Boolean = false,
    val buildings: List<CityBuildingSaveData> = emptyList(),
    val savePortSave: CityPortSave = CityPortSave(),
    val inventorySave: CityInventorySave = CityInventorySave(),
    val lastTickMills: Long = 0L,
    val lastSettlementMills: Long = 0L,
    val policySave: PolicySaveData = PolicySaveData(),
    val greatTokenSave: GreatTokenSave? = null,
    val bankSave: BankSave? = null,
)


class City : KoinComponent, IPersistable {
    private val cityInventory: CityInventory by inject()
    private val balance: Balance by inject()
    private val notifier: Notifier by inject()
    val cityPort: CityPort by inject()
    private val eventBus: EventBus by inject()
    private val cityState: CityState by inject()
    private val policySystem: PolicySystem by inject()
    private val buildingTemplates: DataTable<BuildingTemplate> by inject(named<BuildingTemplate>())
    private val greatToken: GreatToken by inject()
    private val bank: Bank by inject()

    val buildings = mutableMapOf<String, Building>()
    var stats: CityStats by mutableStateOf(emptyCityStats())
        private set

    var finishedWelcomeTutorial: Boolean by mutableStateOf(false)
    var lastTickMills: Long by mutableStateOf(0L)

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            eventBus.events.collect {
                when (it) {
                    is IEvent.TutorialFinished -> {
                        finishedWelcomeTutorial = true
                    }

                    else -> {}
                }
            }
        }
    }

    fun startOver() {
        buildings.clear()
        cityInventory.clear()
        greatToken.clear()
        cityPort.clear()
    }

    fun tick(delta: Float, currentMills: Long) {
        val oldLastTickMills = if (lastTickMills == 0L) currentMills else lastTickMills
        val capped = (currentMills - oldLastTickMills > MAX_TICK_INTERVAL_MILLIS)

        val nowMillis = if (capped) {
            oldLastTickMills + MAX_TICK_INTERVAL_MILLIS
        } else {
            currentMills
        }

        lastTickMills = currentMills

        cityPort.tick(nowMillis)
        bank.tick(nowMillis)

        if (capped) cityPort.syncLastTickMills(currentMills)

        if (checkBankruptcyBadge()) return

        val tickBuildings = buildings.values.toList()

        cityState.tick(tickBuildings)

        val jumpSeconds = (nowMillis - oldLastTickMills) / 1000f
        val buildingDelta = if (jumpSeconds > delta) jumpSeconds else delta

        tickBuildings.forEach { it.tick(buildingDelta) }

        balance.tick(nowMillis, tickBuildings)
        if (capped) balance.syncLastSettlementMills(currentMills)

        stats = buildCityStats(
            cityInventory = cityInventory,
            populations = cityState.populations,
            workforceByTier = cityState.tierStats,
            buildings = tickBuildings,
            balance = balance.balance,
            settlementPeriodSeconds = Balance.BALANCE_PERIOD,
            secondsUntilSettlement = balance.secondsUntilSettlement.toInt()
        )
    }

    fun canAddBuilding(template: BuildingTemplate, count: Int): Boolean =
        cityInventory.inventory.canConsume(
            buildCostsWithPolicies(template, count)
        )

    fun addBuilding(template: BuildingTemplate, count: Int) {
        if (!canAddBuilding(template, count)) return

        cityInventory.inventory.removeItems(
            buildCostsWithPolicies(template, count)
        )

        val existing = buildings[template.id]
        if (existing == null) {
            buildings[template.id] = Building(template, count)
        } else {
            existing.addCount(count)
        }
        eventBus.tryEmit(IEvent.BuildingBuilt(template.id, count))
    }

    private fun buildCostsWithPolicies(template: BuildingTemplate, count: Int): List<ItemEntry> {
        val multiplier =
            (1f + policySystem.getModifier(PolicyModifierType.BuildBuildingCosts, template)).coerceAtLeast(0f)

        return template.buildCosts.mapNotNull { cost ->
            val base = cost.count * count
            val scaled = ceil(base * multiplier).toLong().coerceAtLeast(0)
            if (scaled <= 0) return@mapNotNull null
            ItemEntry(cost.itemId, scaled)
        }
    }

    private fun checkBankruptcyBadge(): Boolean {
        val route = AppDestination.CityDestination.route
        val bankrupted = cityInventory.isleBucks <= -5000

        if (notifier.badgeCountMap.containsKey(route)) {
            if (!bankrupted) notifier.badgeCountMap.remove(route)
            return true
        }

        if (bankrupted) {
            notifier.updateBadge(
                route = route,
                type = NotificationType.CityBankrupted,
            )
            return true
        }

        return false
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        val current = gameSave.citySaveData ?: CitySaveData()
        gameSave.citySaveData = current.copy(
            finishedWelcomeTutorial = finishedWelcomeTutorial,
            buildings = buildings.map { CityBuildingSaveData(it.key, it.value.count) },
            savePortSave = cityPort.toSave(),
            inventorySave = cityInventory.toSave(),
            lastTickMills = lastTickMills,
            lastSettlementMills = balance.lastSettlementMills,
            policySave = policySystem.toSave(),
            greatTokenSave = greatToken.toSave(),
            bankSave = bank.toSave(),
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        val saveData = gameSave.citySaveData ?: return
        finishedWelcomeTutorial = saveData.finishedWelcomeTutorial
        cityPort.fromSave(saveData.savePortSave)
        cityInventory.fromSave(saveData.inventorySave)
        balance.syncLastSettlementMills(saveData.lastSettlementMills)
        policySystem.fromSave(
            policySaveData = saveData.policySave,
        )
        buildings.clear()
        saveData.buildings.forEach { saveData ->
            val template = buildingTemplates.find(saveData.id)
            forceAddBuilding(template, saveData.count)
        }

        saveData.greatTokenSave?.let {
            greatToken.fromSave(it)
        }

        saveData.bankSave?.let {
            bank.fromSave(it)
        }

        lastTickMills = saveData.lastTickMills
    }

    fun forceAddBuilding(template: BuildingTemplate, count: Int) {
        val existing = buildings[template.id]
        if (existing == null) {
            buildings[template.id] = Building(template, count)
        } else {
            existing.addCount(count)
        }
    }

    companion object {
        const val MAX_TICK_INTERVAL_MILLIS = 43200000L
    }
}
