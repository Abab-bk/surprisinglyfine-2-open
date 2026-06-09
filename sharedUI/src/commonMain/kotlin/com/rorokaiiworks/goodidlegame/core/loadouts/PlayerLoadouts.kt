package com.rorokaiiworks.goodidlegame.core.loadouts

import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.*
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class PlayerLoadouts : IPersistable, KoinComponent {
    private val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    private val player: Player by inject()

    val weaponItemSlot = ItemSlot(id = "slot_weapon", name = i18nWrapper("Weapon"), ItemType.Weapons)

    var loadouts: List<Loadout> = listOf(
        Loadout(
            id = "combatLoadout", name = i18nWrapper("Weapon"), listOf(
            weaponItemSlot,
                ItemSlot(id = "slot_shield", name = i18nWrapper("Shield"), setOf(ItemType.Shield)),
                ItemSlot(id = "slot_helmet", name = i18nWrapper("Helmet"), setOf(ItemType.Helmet)),
                ItemSlot(id = "slot_armor", name = i18nWrapper("Armor"), setOf(ItemType.Armor)),
                ItemSlot(id = "slot_leg_armor", name = i18nWrapper("LegArmor"), setOf(ItemType.LegArmor)),
                ItemSlot(id = "slot_boots", name = i18nWrapper("Boots"), setOf(ItemType.Boots)),
        )),

        Loadout(
            id = "accessoriesLoadout", name = i18nWrapper("Accessories"), listOf(
            ItemSlot(id = "slot_necklace", name = i18nWrapper("Necklace"), setOf(ItemType.Necklace)),
            ItemSlot(id = "slot_ring", name = i18nWrapper("Ring"), setOf(ItemType.Ring)),
            ItemSlot(id = "slot_bracelet", name = i18nWrapper("Bracelet"), setOf(ItemType.Bracelet)),
            ItemSlot(id = "slot_cape", name = i18nWrapper("Cape"), setOf(ItemType.Cape)),
        )),

        Loadout(
            id = "toolsLoadout", name = i18nWrapper("Tools"), listOf(
                ItemSlot(id = "slot_axe", name = i18nWrapper("Axe"), setOf(ItemType.Axe)),
                ItemSlot(id = "slot_pickaxe", name = i18nWrapper("Pickaxe"), setOf(ItemType.Pickaxe)),
                ItemSlot(id = "slot_spade", name = i18nWrapper("Spade"), setOf(ItemType.Spade)),
                ItemSlot(id = "slot_rod", name = i18nWrapper("Rod"), setOf(ItemType.Rod)),
                ItemSlot(id = "slot_trap", name = i18nWrapper("Trap"), setOf(ItemType.Trap)),
        )),
    )

    val equippedItems: Sequence<Item>
        get() = loadouts.asSequence()
            .flatMap { it.slots.asSequence() }
            .filter { slot -> slot.item != null }
            .map { slot -> slot.item!! }


    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.playerLoadouts = PlayerLoadoutsSaveData(
            loadouts = loadouts.associateBy(
                keySelector = { it.id },
                valueTransform = { loadout ->
                    loadout.slots.associateBy(
                        keySelector = { it.id },
                        valueTransform = { slot -> slot.item?.toSaveData() }
                    )
                }
            )
        )
    }


    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        val saveData = gameSave.playerLoadouts

        saveData.loadouts.forEach { (loadoutId, slotsData) ->
            val loadout = loadouts.firstOrNull { it.id == loadoutId } ?: return@forEach

            slotsData.forEach { (slotId, itemSaveData) ->
                val slot = loadout.slots.firstOrNull { it.id == slotId } ?: return@forEach

                val item = itemSaveData?.let {
                    val template = itemTemplates.find(it.itemId)
                    Item(template = template, count = it.count).apply {
                        customModifiers = it.customModifiers
                        enchantmentLevel = it.enchantmentLevel
                    }
                }

                loadout.loadItem(
                    itemSlot = slot,
                    item = item,
                    actor = player
                )
            }
        }
    }
}


@Serializable
data class PlayerLoadoutsSaveData(
    val loadouts: Map<String, Map<String, ItemSaveData?>> = emptyMap()
)
