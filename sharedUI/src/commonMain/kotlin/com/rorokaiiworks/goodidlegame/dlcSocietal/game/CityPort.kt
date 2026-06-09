package com.rorokaiiworks.goodidlegame.dlcSocietal.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicyModifierType
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicySystem
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Serializable
enum class TradeMode(val label: String) {
    None(i18nWrapper("None")),
    Sell(i18nWrapper("Sell")),
}

@Serializable
data class TradeRule(
    val mode: TradeMode = TradeMode.None,
    val minStock: Int = 0,
)

@Serializable
data class CityPortSave(
    val tradeRules: Map<String, TradeRule> = emptyMap(),
    val saturationMap: Map<String, Float> = emptyMap(),
    val capacityLevel: Int = 1,
    val tradeIntervalLevel: Int = 1,
    val saturationSpeedLevel: Int = 1,
    val lastTickMills: Long = 0L,
)

class CityPort : KoinComponent {
    val tradeRules = mutableStateMapOf<String, TradeRule>()

    private val saturationMap = mutableStateMapOf<String, Float>()
    private val itemService: ItemService by inject()
    private val cityInventory: CityInventory by inject()
    private val eventBus: EventBus by inject()
    private val policySystem: PolicySystem by inject()

    private var lastTickMills: Long = 0

    fun clear() {
        tradeRules.clear()
        lastTradeResult = emptyList()
    }

    fun syncLastTickMills(mills: Long) {
        lastTickMills = mills
    }

    var capacityLevel by mutableStateOf(1)
        private set

    var tradeIntervalLevel by mutableStateOf(1)
        private set

    var saturationSpeedLevel by mutableStateOf(1)
        private set

    var capacity by mutableLongStateOf(0)
        private set

    var tradeInterval by mutableStateOf(Duration.ZERO)
        private set

    var nextTradeTime by mutableStateOf(0L)
        private set

    var nextTradeTimeDistance by mutableStateOf(Duration.ZERO)
        private set

    var lastTradeResult by mutableStateOf(emptyList<ItemEntry>())
        private set

    fun toSave() = CityPortSave(
        tradeRules = tradeRules,
        saturationMap = saturationMap,
        capacityLevel = capacityLevel,
        tradeIntervalLevel = tradeIntervalLevel,
        saturationSpeedLevel = saturationSpeedLevel,
        lastTickMills = lastTickMills,
    )

    fun fromSave(save: CityPortSave) {
        tradeRules.clear()
        saturationMap.clear()

        save.tradeRules.forEach { (itemId, rule) ->
            tradeRules[itemId] = rule
        }

        save.saturationMap.forEach { (itemId, saturation) ->
            saturationMap[itemId] = saturation
        }

        capacityLevel = save.capacityLevel
        tradeIntervalLevel = save.tradeIntervalLevel
        saturationSpeedLevel = save.saturationSpeedLevel
        lastTickMills = save.lastTickMills

        capacity = CityFormulas.calculatePortCapacityForLevel(capacityLevel)
        tradeInterval = CityFormulas.calculatePortTradeIntervalForLevel(tradeIntervalLevel)
    }

    fun getSaturation(itemId: String): Float = saturationMap[itemId] ?: 0f

    fun getPriceMultiplier(itemId: String): Float {
        val saturation = getSaturation(itemId).coerceIn(0f, 1f)
        val reduction = saturation * SATURATION_PRICE_REDUCTION
        return (1f - reduction).coerceAtLeast(SATURATION_PRICE_MIN_MULTIPLIER)
    }

    init {
        capacity = CityFormulas.calculatePortCapacityForLevel(capacityLevel)
        tradeInterval = CityFormulas.calculatePortTradeIntervalForLevel(tradeIntervalLevel)
    }

    fun tick(currentMills: Long) {
        if (lastTickMills == 0L) lastTickMills = currentMills

        var cursor = lastTickMills.coerceAtMost(currentMills)

        if (nextTradeTime <= 0L) nextTradeTime = cursor
        if (nextTradeTime < cursor) nextTradeTime = cursor

        val intervalMillis = tradeInterval.inWholeMilliseconds.coerceAtLeast(1L)

        var processedTrades = 0
        while (nextTradeTime <= currentMills && processedTrades < MAX_TRADES_PER_TICK) {
            val millsToTrade = (nextTradeTime - cursor).coerceAtLeast(0L)
            if (millsToTrade > 0L) {
                decaySaturation((millsToTrade / 1000L).toFloat())
                cursor += millsToTrade
            }

            trade()
            processedTrades += 1
            nextTradeTime += intervalMillis
        }

        val remainingMills = (currentMills - cursor).coerceAtLeast(0L)
        if (remainingMills > 0L) {
            decaySaturation((remainingMills / 1000L).toFloat())
        }

        lastTickMills = currentMills
        nextTradeTimeDistance = (nextTradeTime - currentMills).coerceAtLeast(0L).milliseconds
    }

    fun upgradeCapacity(
        canConsume: (List<ItemEntry>) -> Boolean,
        consume: (List<ItemEntry>) -> Unit
    ): Boolean {
        val costs = CityFormulas.calculatePortCapacityLevelUpCosts(capacityLevel)

        if (!canConsume(costs)) return false
        consume(costs)

        capacityLevel += 1

        capacity = CityFormulas.calculatePortCapacityForLevel(capacityLevel)

        return true
    }

    fun upgradeTradeInterval(
        canConsume: (List<ItemEntry>) -> Boolean,
        consume: (List<ItemEntry>) -> Unit
    ): Boolean {
        val costs = CityFormulas.calculatePortTradeIntervalLevelUpCosts(tradeIntervalLevel)

        if (!canConsume(costs)) return false
        consume(costs)

        tradeIntervalLevel += 1
        tradeInterval = CityFormulas.calculatePortTradeIntervalForLevel(tradeIntervalLevel)

        return true
    }

    fun upgradeSaturationSpeed(
        canConsume: (List<ItemEntry>) -> Boolean,
        consume: (List<ItemEntry>) -> Unit
    ): Boolean {
        val costs = CityFormulas.calculatePortSaturationSpeedLevelUpCosts(saturationSpeedLevel)

        if (!canConsume(costs)) return false
        consume(costs)

        saturationSpeedLevel += 1
        return true
    }

    private fun trade() {
        var remainingCapacity = capacity
        val selectedItems = tradeRules.entries
            .filter { it.value.mode == TradeMode.Sell }
            .mapNotNull { cityInventory.inventory.findItem(it.key) }
            .sortedByDescending { it.template.price }
            .mapNotNull { item ->
                if (remainingCapacity <= 0) return@mapNotNull null

                val amountToTake = minOf(item.count, remainingCapacity)
                remainingCapacity -= amountToTake

                ItemEntry(
                    itemId = item.template.id,
                    count = amountToTake
                )
            }

        if (selectedItems.isEmpty()) {
            lastTradeResult = emptyList()
            return
        }

        val rewardAmount = selectedItems.sumOf { entry ->
            val template = itemService.findItemTemplate(entry.itemId)
            val multiplier = getPriceMultiplier(entry.itemId)
            val unitPrice = (template.price * multiplier).roundToInt().coerceAtLeast(1)
            unitPrice * entry.count
        }

        selectedItems.forEach { entry ->
            addSaturation(entry.itemId, entry.count)
        }

        val reward = itemService.createItem("isle_bucks", rewardAmount)

        cityInventory.inventory.addItem(reward)
        cityInventory.inventory.removeItems(selectedItems)

        lastTradeResult = selectedItems
    }

    fun findRule(id: String): TradeRule {
        return tradeRules.getOrPut(id) { TradeRule() }
    }

    fun setRule(id: String, rule: TradeRule) {
        tradeRules[id] = rule
        eventBus.tryEmit(IEvent.CityItemTradeModeChanged(id, rule.mode))
    }

    private fun addSaturation(itemId: String, soldCount: Long) {
        if (soldCount <= 0) return
        val increasePerItem = CityFormulas.calculatePortSaturationIncreasePerItemForLevel(saturationSpeedLevel)
        val increase = (soldCount * increasePerItem).coerceAtLeast(0f)
        val current = getSaturation(itemId)
        saturationMap[itemId] = (current + increase).coerceIn(0f, 1f)
    }

    private fun decaySaturation(deltaSeconds: Float) {
        if (saturationMap.isEmpty()) return
        val multiplier = (1f + policySystem.getModifier(PolicyModifierType.SaturationDecaySpeed, null)).coerceAtLeast(0f)
        val decayPerSecond = CityFormulas.calculatePortSaturationDecayPerSecondForLevel(saturationSpeedLevel) * multiplier
        val decay = (deltaSeconds * decayPerSecond).coerceAtLeast(0f)
        saturationMap.keys.toList().forEach { key ->
            val newValue = (getSaturation(key) - decay).coerceAtLeast(0f)
            if (newValue <= 0f) saturationMap.remove(key) else saturationMap[key] = newValue
        }
    }

    companion object {
        private const val SATURATION_PRICE_REDUCTION = 0.60f
        private const val SATURATION_PRICE_MIN_MULTIPLIER = 0.40f
        private const val MAX_TRADES_PER_TICK = 20_000
    }
}
