package com.rorokaiiworks.goodidlegame.core.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.RandomSource
import com.rorokaiiworks.goodidlegame.core.items.Inventory
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemSaveData
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class EnchantingTableSaveData(
    val slots: List<EnchantingSlotSaveData> = List(3) { EnchantingSlotSaveData() }
)

@Serializable
data class EnchantingSlotSaveData(
    val originalItem: ItemSaveData? = null,
    val processedItem: ItemSaveData? = null,
    val startTime: Long? = null,
    val duration: Long? = null
)

data class EnchantmentPreview(
    val statId: String,
    val range: ClosedFloatingPointRange<Float>,
    val type: StatModifierType,
    val probability: Float
)

class EnchantingTable : KoinComponent {
    private val itemService: ItemService by inject()

    val slots: SnapshotStateList<EnchantingSlot> = mutableStateListOf(
        EnchantingSlot(initialState = EnchantingSlotState.Idle(null, null)),
        EnchantingSlot(initialState = EnchantingSlotState.Idle(null, null)),
        EnchantingSlot(initialState = EnchantingSlotState.Idle(null, null)),
    )

    fun getPurchaseSlotCosts(): List<ItemEntry> {
        return listOf(
            ItemEntry(
                itemId = "coins",
                count = 1000L * (slots.size + 1)
            )
        )
    }

    fun purchaseSlot(inventory: Inventory) {
        val costs = getPurchaseSlotCosts()
        if (!inventory.canConsume(costs)) return
        inventory.removeItems(costs)
        slots.add(EnchantingSlot(initialState = EnchantingSlotState.Idle(null, null)))
    }

    fun calculateEnchantingConsumes(item: Item): List<ItemEntry> {
        return listOf(
            ItemEntry(
                itemId = "coins",
                count = 50L * (item.enchantmentLevel + 1)
            )
        ) + when (item.enchantmentLevel) {
            0 -> listOf(
                ItemEntry(
                    itemId = "essence_ruby",
                    count = 3
                ),
            )
            1 -> listOf(
                ItemEntry(
                    itemId = "essence_yellow",
                    count = 3
                ),
            )
            2 -> listOf(
                ItemEntry(
                    itemId = "essence_green",
                    count = 3
                ),
            )
            3 -> listOf(
                ItemEntry(
                    itemId = "essence_moon",
                    count = 3
                ),
            )
            4 -> listOf(
                ItemEntry(
                    itemId = "essence_crystal",
                    count = 3
                ),
            )
            5 -> listOf(
                ItemEntry(
                    itemId = "essence_star",
                    count = 3
                ),
            )
            6 -> listOf(
                ItemEntry(
                    itemId = "essence_onyx",
                    count = 3
                ),
            )
            7 -> listOf(
                ItemEntry(
                    itemId = "essence_rainbow",
                    count = 3
                ),
            )
            else -> listOf(
                ItemEntry(
                    itemId = "essence_rainbow",
                    count = 6
                ),
            )
        }
    }

    fun tick(nowMills: Long) {
        slots.forEach { it.tick(nowMills) }
    }

    fun toSaveData(): EnchantingTableSaveData {
        return EnchantingTableSaveData(
            slots = slots.map { slot -> slot.toSaveData() }
        )
    }

    fun fromSaveData(saveData: EnchantingTableSaveData) {
        val loadedSlots = saveData.slots.map { data ->
            val state = when {
                data.startTime != null && data.duration != null && data.originalItem != null -> {
                    EnchantingSlotState.Running(
                        startTime = data.startTime,
                        duration = data.duration,
                        item = itemService.fromSaveData(data.originalItem)
                    )
                }

                else -> {
                    EnchantingSlotState.Idle(
                        originalItem = data.originalItem?.let { itemService.fromSaveData(it) },
                        processedItem = data.processedItem?.let { itemService.fromSaveData(it) }
                    )
                }
            }
            EnchantingSlot(state)
        }

        slots.clear()
        slots.addAll(loadedSlots)
    }

    fun getEnchantmentPreviews(type: ItemType): List<EnchantmentPreview> {
        val pool = modifierPools[type] ?: return emptyList()
        val total = pool.totalWeight.toFloat()
        return pool.entries.map {
            EnchantmentPreview(
                statId = it.statId,
                range = it.range,
                type = it.type,
                probability = it.weight / total
            )
        }
    }
}

sealed interface EnchantingSlotState {
    data class Idle(val originalItem: Item?, val processedItem: Item?) : EnchantingSlotState
    data class Running(
        val startTime: Long,
        val duration: Long,
        val item: Item
    ) : EnchantingSlotState {
        fun remains(now: Long): Duration = (duration - (now - startTime)).toDuration(DurationUnit.MILLISECONDS)

        fun getProgress(now: Long): Float {
            val elapsed = now - startTime
            return (elapsed.toFloat() / duration).coerceIn(0f, 1f)
        }
    }
}


class EnchantingSlot(
    initialState: EnchantingSlotState
) : KoinComponent {
    private val itemService: ItemService by inject()
    private val randomSource: RandomSource by inject { parametersOf(RandomSource.TAG_ENCHANTING_TABLE) }
    private val timeProvider: ITimeProvider by inject()

    var state by mutableStateOf(initialState)
        private set

    fun clear() {
        state = EnchantingSlotState.Idle(null, null)
    }

    fun placeItem(item: Item) {
        state = EnchantingSlotState.Running(
            item = item,
            duration = 2.hours.toLong(DurationUnit.MILLISECONDS),
            startTime = timeProvider.nowMillis()
        )
    }

    fun tick(nowMills: Long) {
        val current = state

        if (current !is EnchantingSlotState.Running) return
        if (nowMills <= current.startTime + current.duration) return

        state = EnchantingSlotState.Idle(
            originalItem = current.item.copy(enchantmentLevel = current.item.enchantmentLevel + 1),
            processedItem = makeEnchantedItem(current.item),
        )
    }

    private fun makeEnchantedItem(item: Item): Item {
        val modifiersCount = randomSource.nextInt(1, 3)
        val newItem = itemService.cloneItem(item).also {
            it.enchantmentLevel += 1
            it.customModifiers = List(modifiersCount) { pickRandomModifier(item.template) }
        }

        return newItem
    }

    private fun pickRandomModifier(
        itemTemplate: ItemTemplate
    ): StatModifier {
        val pool = modifierPools[itemTemplate.type] ?: return StatModifier(
            statId = StatIds.Actor.Armor,
            value = 0f,
            type = StatModifierType.Flat,
            isAdditional = true
        )

        return pool.pick(randomSource)
    }

    fun toSaveData(): EnchantingSlotSaveData {
        return when (val s = state) {
            is EnchantingSlotState.Idle -> EnchantingSlotSaveData(
                originalItem = s.originalItem?.toSaveData(),
                processedItem = s.processedItem?.toSaveData()
            )

            is EnchantingSlotState.Running -> EnchantingSlotSaveData(
                originalItem = s.item.toSaveData(),
                startTime = s.startTime,
                duration = s.duration
            )
        }
    }
}


private data class ModifierEntry(
    val statId: String,
    val weight: Int,
    val range: ClosedFloatingPointRange<Float>,
    val type: StatModifierType = StatModifierType.Percent
)

private class ModifierPool(
    val entries: List<ModifierEntry>
) {
    val totalWeight = entries.sumOf { it.weight }

    fun pick(randomSource: RandomSource): StatModifier {
        if (entries.isEmpty()) return StatModifier(StatIds.Actor.Armor, 0f, StatModifierType.Flat)

        var randomValue = randomSource.nextInt(0, totalWeight)

        val selectedEntry = entries.firstOrNull {
            randomValue -= it.weight
            randomValue < 0
        } ?: entries.first()

        return StatModifier(
            statId = selectedEntry.statId,
            value = randomSource.nextFloat(selectedEntry.range.start, selectedEntry.range.endInclusive),
            type = selectedEntry.type,
            isAdditional = true
        )
    }
}

private fun weaponPool(damageStat: String) = ModifierPool(
    listOf(
        ModifierEntry(damageStat, 40, 0.05f..0.25f),
        ModifierEntry(StatIds.Actor.AttackSpeed, 30, 0.05f..0.15f),
        ModifierEntry(StatIds.Actor.HitChanceBonus, 30, 0.01f..0.1f)
    )
)

private fun armorPool(resStat: String) = ModifierPool(
    listOf(
        ModifierEntry(StatIds.Actor.Armor, 30, 0.05f..0.2f),
        ModifierEntry(StatIds.Actor.MaxHealth, 30, 0.05f..0.25f),
        ModifierEntry(resStat, 20, 0.05f..0.2f),
        ModifierEntry(StatIds.Actor.DodgeChanceBonus, 20, 0.01f..0.05f)
    )
)

private fun toolPool(efficiency: String, speed: String, xp: String, loot: String) = ModifierPool(
    listOf(
        ModifierEntry(efficiency, 25, 0.05f..0.2f),
        ModifierEntry(speed, 25, 0.05f..0.2f),
        ModifierEntry(xp, 25, 0.05f..0.2f),
        ModifierEntry(loot, 25, 0.05f..0.2f),
    )
)

private fun accessoryPool() = ModifierPool(
    listOf(
        ModifierEntry(StatIds.Skills.CombatYield, 10, 0.05f..0.15f),
        ModifierEntry(StatIds.Skills.GatherYield, 10, 0.05f..0.15f),
        ModifierEntry(StatIds.Skills.CraftYield, 10, 0.05f..0.15f),
        ModifierEntry(StatIds.Skills.CombatXp, 10, 0.05f..0.15f),
        ModifierEntry(StatIds.Skills.GatherXp, 10, 0.05f..0.15f),
        ModifierEntry(StatIds.Skills.CraftXp, 10, 0.05f..0.15f),
        ModifierEntry(StatIds.Skills.GatherSpeed, 10, 0.05f..0.15f),
        ModifierEntry(StatIds.Skills.CraftSpeed, 10, 0.05f..0.15f),
        ModifierEntry(StatIds.Player.OfflineRewardMultiplier, 10, 0.05f..0.15f),
        ModifierEntry(StatIds.Actor.MaxHealth, 10, 0.05f..0.15f),

        ModifierEntry(StatIds.Actor.SlashDamage, 10, 0.05f..0.25f),
        ModifierEntry(StatIds.Actor.PunctureDamage, 10, 0.05f..0.25f),
        ModifierEntry(StatIds.Actor.ImpactDamage, 10, 0.05f..0.25f),

        ModifierEntry(StatIds.Actor.SlashResistance, 10, 0.05f..0.25f),
        ModifierEntry(StatIds.Actor.PunctureResistance, 10, 0.05f..0.25f),
        ModifierEntry(StatIds.Actor.ImpactResistance, 10, 0.05f..0.25f),

        ModifierEntry(StatIds.Actor.AttackSpeed, 10, 0.05f..0.15f),
        ModifierEntry(StatIds.Actor.HitChanceBonus, 10, 0.01f..0.1f),
    )
)

private val modifierPools = mapOf(
    ItemType.Sword to weaponPool(StatIds.Actor.SlashDamage),
    ItemType.Hammer to weaponPool(StatIds.Actor.ImpactDamage),
    ItemType.Scythe to weaponPool(StatIds.Actor.SlashDamage),
    ItemType.Spear to weaponPool(StatIds.Actor.PunctureDamage),
    ItemType.Bow to weaponPool(StatIds.Actor.PunctureDamage),
    ItemType.Dart to weaponPool(StatIds.Actor.PunctureDamage),

    ItemType.Shield to armorPool(StatIds.Actor.ImpactResistance),
    ItemType.Helmet to armorPool(StatIds.Actor.PunctureResistance),
    ItemType.Armor to armorPool(StatIds.Actor.SlashResistance),
    ItemType.LegArmor to armorPool(StatIds.Actor.SlashResistance),
    ItemType.Boots to armorPool(StatIds.Actor.PunctureResistance),

    ItemType.Cape to ModifierPool(
        listOf(
            ModifierEntry(StatIds.Actor.DodgeChanceBonus, 30, 0.01f..0.05f),
            ModifierEntry(StatIds.Player.OfflineRewardMultiplier, 30, 0.05f..0.15f),
            ModifierEntry(StatIds.Skills.CombatXp, 20, 0.05f..0.15f),
            ModifierEntry(StatIds.Skills.GatherXp, 20, 0.05f..0.15f)
        )
    ),

    ItemType.Necklace to accessoryPool(),
    ItemType.Ring to accessoryPool(),
    ItemType.Bracelet to accessoryPool(),

    ItemType.Axe to toolPool(
        StatIds.Skills.WoodcuttingEfficiency,
        StatIds.Skills.WoodcuttingSpeed,
        StatIds.Skills.WoodcuttingXpMultiplier,
        StatIds.Skills.WoodcuttingLootMultiplier
    ),
    ItemType.Pickaxe to toolPool(
        StatIds.Skills.MiningEfficiency,
        StatIds.Skills.MiningSpeed,
        StatIds.Skills.MiningXpMultiplier,
        StatIds.Skills.MiningLootMultiplier
    ),

    ItemType.Spade to ModifierPool(
        listOf(
            ModifierEntry(StatIds.Skills.FarmingEfficiency, 20, 0.05f..0.2f),
            ModifierEntry(StatIds.Skills.FarmingSpeed, 20, 0.05f..0.2f),
            ModifierEntry(StatIds.Skills.FarmingXpMultiplier, 20, 0.05f..0.2f),
            ModifierEntry(StatIds.Skills.FarmingLootMultiplier, 20, 0.05f..0.2f),
            ModifierEntry(StatIds.Skills.ArchaeologyEfficiency, 10, 0.05f..0.2f),
            ModifierEntry(StatIds.Skills.ArchaeologySpeed, 10, 0.05f..0.2f)
        )
    ),

    ItemType.Rod to toolPool(
        StatIds.Skills.FishingEfficiency,
        StatIds.Skills.FishingSpeed,
        StatIds.Skills.FishingXpMultiplier,
        StatIds.Skills.FishingLootMultiplier
    ),

    ItemType.Trap to toolPool(
        StatIds.Skills.HuntingEfficiency,
        StatIds.Skills.HuntingSpeed,
        StatIds.Skills.HuntingXpMultiplier,
        StatIds.Skills.HuntingLootMultiplier
    ),
)
    