package com.rorokaiiworks.goodidlegame.core.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.items.Inventory
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import com.rorokaiiworks.goodidlegame.core.stats.ISourceName
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named


private const val BASE_MAX_CAPACITY = 10
private const val MAX_PURCHASABLE_CAPACITY_UPGRADES = 20
private const val CAPACITY_INCREMENT = 5
private const val BASE_CAPACITY_UPGRADE_COST = 500L

@Serializable
data class SquareBuildingTemplate(
    override val id: String,
    val name: String,
    val tiers: List<SquareBuildingTier>
) : Template, ISourceName {
    override val sourceName: String get() = name
}

@Serializable
data class SquareBuildingTier(
    val size: Int,
    val cost: List<ItemEntry>,
    val modifiers: List<List<StatModifier>>
)

class SquareBuilding(
    val template: SquareBuildingTemplate,
) {
    var unlockedTierCount: Int by mutableStateOf(0)
    var activeTierCount: Int by mutableStateOf(0)
    var selectedModifiersPerTier: SnapshotStateList<Int> = mutableStateListOf()

    val isBuilt: Boolean get() = unlockedTierCount > 0
    val hasNextTier: Boolean get() = unlockedTierCount <= template.tiers.lastIndex
    val nextTierToUnlock: SquareBuildingTier? get() = template.tiers.getOrNull(unlockedTierCount)

    val activeTiers: List<SquareBuildingTier>
        get() = template.tiers.take(activeTierCount)

    val currentSize: Int
        get() = activeTiers.sumOf { it.size }

    fun sizeForTierCount(count: Int): Int =
        template.tiers.take(count).sumOf { it.size }

    fun selectedModifierIndexForTier(tierIndex: Int): Int =
        selectedModifiersPerTier.getOrElse(tierIndex) { 0 }

    val activeModifiers: List<StatModifier>
        get() = activeTiers.flatMapIndexed { tierIndex, tier ->
            val modifierIndex = selectedModifierIndexForTier(tierIndex)
            tier.modifiers.getOrElse(modifierIndex) { emptyList() }
        }
}

@Serializable
data class SquareSaveData(
    val maxCapacity: Int = BASE_MAX_CAPACITY,
    val buildings: List<SquareBuildingSaveData> = emptyList(),
    val purchasedCapacity: Int = 0
)

@Serializable
data class SquareBuildingSaveData(
    val templateId: String,
    val unlockedTierCount: Int,
    val activeTierCount: Int,
    val selectedModifiersPerTier: List<Int> = emptyList()
)

class Square : KoinComponent {
    private val squareBuildingsTable: DataTable<SquareBuildingTemplate> by inject(named<SquareBuildingTemplate>())
    private val player: Player by inject()

    val buildings: SnapshotStateList<SquareBuilding> = mutableStateListOf<SquareBuilding>().apply {
        addAll(squareBuildingsTable.all().map { SquareBuilding(template = it) })
    }

    var maxCapacity by mutableStateOf(BASE_MAX_CAPACITY)
    var purchasedCapacity by mutableStateOf(0)

    val usedCapacity: Int get() = buildings.sumOf { it.currentSize }
    val availableCapacity: Int get() = maxCapacity - usedCapacity

    fun canUpgradeCapacity(): Boolean = purchasedCapacity < MAX_PURCHASABLE_CAPACITY_UPGRADES

    fun getCapacityUpgradeCost(): List<ItemEntry> {
        val base = ItemEntry("coins", BASE_CAPACITY_UPGRADE_COST)
        return listOf(base.copy(count = base.count * (purchasedCapacity + 1)))
    }

    fun purchaseCapacityUpgrade(inventory: Inventory): Boolean {
        if (!canUpgradeCapacity()) return false
        val cost = getCapacityUpgradeCost()
        if (!inventory.canConsume(cost)) return false
        inventory.removeItems(cost)
        purchasedCapacity++
        maxCapacity += CAPACITY_INCREMENT
        return true
    }

    fun canUnlockNextTier(building: SquareBuilding): Boolean {
        if (!building.hasNextTier) return false
        val nextTier = building.nextTierToUnlock ?: return false
        return availableCapacity >= nextTier.size
    }

    fun unlockNextTier(
        building: SquareBuilding,
        selectedModifierIndex: Int,
        inventory: Inventory
    ): Boolean {
        if (!canUnlockNextTier(building)) return false
        val nextTier = building.nextTierToUnlock ?: return false
        if (!inventory.canConsume(nextTier.cost)) return false

        inventory.removeItems(nextTier.cost)

        val tierIndex = building.unlockedTierCount
        if (tierIndex < building.selectedModifiersPerTier.size) {
            building.selectedModifiersPerTier[tierIndex] = selectedModifierIndex
        } else {
            building.selectedModifiersPerTier.add(selectedModifierIndex)
        }

        building.unlockedTierCount++
        building.activeTierCount = building.unlockedTierCount // auto-activate
        syncBuildingEffect(building)
        return true
    }

    fun setActiveTierCount(building: SquareBuilding, count: Int): Boolean {
        val clamped = count.coerceIn(0, building.unlockedTierCount)
        val newSize = building.sizeForTierCount(clamped)
        val otherUsedCapacity = buildings.filter { it !== building }.sumOf { it.currentSize }
        if (newSize + otherUsedCapacity > maxCapacity) return false
        building.activeTierCount = clamped
        syncBuildingEffect(building)
        return true
    }

    fun selectModifier(building: SquareBuilding, tierIndex: Int, modifierIndex: Int): Boolean {
        if (tierIndex >= building.unlockedTierCount) return false
        val tier = building.template.tiers.getOrNull(tierIndex) ?: return false
        if (modifierIndex >= tier.modifiers.size) return false
        while (building.selectedModifiersPerTier.size <= tierIndex) {
            building.selectedModifiersPerTier.add(0)
        }
        building.selectedModifiersPerTier[tierIndex] = modifierIndex
        syncBuildingEffect(building)
        return true
    }

    private fun syncBuildingEffect(building: SquareBuilding) {
        player.effectManager.removeAllEffectsBySource("square:${building.template.id}")
        player.effectManager.addEffect(Effect(
            id = "square_building_${building.template.id}",
            source = "square:${building.template.id}",
            sourceName = building.template,
            modifiers = building.activeModifiers
        ))
    }

    fun tick(nowMills: Long) {}

    fun toSaveData(): SquareSaveData = SquareSaveData(
        maxCapacity = maxCapacity,
        purchasedCapacity = purchasedCapacity,
        buildings = buildings.filter { it.isBuilt }.map { building ->
            SquareBuildingSaveData(
                templateId = building.template.id,
                unlockedTierCount = building.unlockedTierCount,
                activeTierCount = building.activeTierCount,
                selectedModifiersPerTier = building.selectedModifiersPerTier.toList()
            )
        }
    )

    fun fromSaveData(saveData: SquareSaveData) {
        maxCapacity = saveData.maxCapacity
        purchasedCapacity = saveData.purchasedCapacity

        val builtMap = saveData.buildings.associateBy { it.templateId }

        val restored = squareBuildingsTable.all().map { template ->
            val save = builtMap[template.id]
            SquareBuilding(template = template).apply {
                if (save != null) {
                    unlockedTierCount = save.unlockedTierCount
                    activeTierCount = save.activeTierCount
                    selectedModifiersPerTier = save.selectedModifiersPerTier.toMutableStateList()
                }
            }
        }

        buildings.clear()
        buildings.addAll(restored)
        buildings.forEach { syncBuildingEffect(it) }
    }
}