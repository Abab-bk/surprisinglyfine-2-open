package com.rorokaiiworks.goodidlegame.core.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.rorokaiiworks.goodidlegame.core.items.Inventory
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemSaveData
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import com.rorokaiiworks.goodidlegame.core.players.Player
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


@Serializable
data class AltarSaveData(
    val slots: List<AltarSlotSaveData> = DEFAULT_SLOTS,
) {
    companion object {
        val DEFAULT_SLOTS = List(Altar.BASE_SLOTS) { AltarSlotSaveData() }
    }
}

@Serializable
data class AltarSlotSaveData(
    val item: ItemSaveData? = null
)

class Altar : KoinComponent {
    private val itemService: ItemService by inject()
    private val player: Player by inject()

    val slots: SnapshotStateList<AltarSlot> = mutableStateListOf(
        AltarSlot(),
        AltarSlot(),
    )

    fun canPurchaseSlot(): Boolean {
        return slots.size < MAX_PURCHASABLE_SLOTS
    }

    fun getSlotPurchaseCost(): List<ItemEntry> {
        val baseCost = ItemEntry("coins", BASE_SLOT_COST)
        return listOf(
            baseCost.copy(count = baseCost.count * (slots.size + 1))
        )
    }

    fun purchaseSlot(inventory: Inventory): Boolean {
        if (!canPurchaseSlot()) return false
        val cost = getSlotPurchaseCost()
        if (!inventory.canConsume(cost)) return false
        inventory.removeItems(cost)
        slots.add(AltarSlot())
        return true
    }

    fun tick(nowMills: Long) {}

    fun place(slot: AltarSlot, item: Item?) {
        if (slot !in slots) return

        if (slot.item != item) {
            if (slot.item?.template?.type == ItemType.Relic) {
                itemService.unequipItem(slot.item!!, player)
            }
        }

        slot.item = item
        slot.item?.let {
            itemService.equipItem(it, player)
        }
    }

    fun remove(slot: AltarSlot): Item? {
        if (slot !in slots) return null
        val item = slot.item
        item?.let {
            if (it.template.type == ItemType.Relic) {
                itemService.unequipItem(it, player)
            }
        }
        slot.item = null
        return item
    }

    fun toSaveData(): AltarSaveData {
        return AltarSaveData(
            slots = slots.map {
                AltarSlotSaveData(
                    item = it.item?.toSaveData()
                )
            },
        )
    }

    fun fromSaveData(data: AltarSaveData) {
        slots.clear()
        slots.addAll(data.slots.map {
            AltarSlot().apply {
                item = it.item?.let { itemSaveData -> itemService.fromSaveData(itemSaveData) }
            }
        })

        slots.forEach {
            it.item?.let { item ->
                if (item.template.type == ItemType.Relic) {
                    itemService.equipItem(item, player)
                }
            }
        }
    }

    companion object {
        const val BASE_SLOTS = 2
        const val MAX_PURCHASABLE_SLOTS = 10
        const val BASE_SLOT_COST = 1000L
    }
}

class AltarSlot {
    var item: Item? by mutableStateOf(null)
}