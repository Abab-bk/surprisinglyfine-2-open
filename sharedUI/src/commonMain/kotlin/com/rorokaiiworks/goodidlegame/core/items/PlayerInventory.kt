@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.core.items

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.LaunchSettings
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi


@Serializable
data class PlayerInventorySaveData(
    val items: List<ItemSaveData> = emptyList(),
    var extraInventorySlotsPurchased: Int = 0,
)


class PlayerInventory(
    private val itemTemplates: DataTable<ItemTemplate>,
): IPersistable, KoinComponent {
    private val launchSettings: LaunchSettings by inject()
    private val itemService: ItemService by inject()

    var coins by mutableLongStateOf(0)
        private set

    var stars by mutableLongStateOf(0)
        private set

    private val eventBus: EventBus by inject()
    private val scope = CoroutineScope(Dispatchers.Default)
        private val i18n: I18n by inject()

    var extraSlotsPurchased: Int = 0
        private set
    var maxSlots: Int = 20
        private set(value) {
            field = value
            inventory.maxSlots = value
        }

    @OptIn(ExperimentalUuidApi::class)
    var inventory: Inventory = Inventory(
        maxSlots = maxSlots,
        onAddItem = {
            itemId, count ->

            updateCoins()

            scope.launch {
                eventBus.emit(IEvent.ItemCollected(itemId, count))
                eventBus.emit(IEvent.ToastMessage(
                    msg = "+ ${Humanizer.abbreviation(count)} ${i18n.tr(itemTemplates.find(itemId).name)}",
                    iconId = itemId
                ))
            }
        },
        onRemoveItem = {
            _, _ ->
            updateCoins()
        }
    )
        private set

    fun addExtraSlots(count: Int) {
        extraSlotsPurchased += count
        maxSlots += count
    }

    fun addItems(items: List<Item>) {
        items.forEach {
            inventory.addItem(it)
        }
    }

    fun addCoins(count: Long) {
        inventory.addItem(Item(
            template = itemTemplates.find("coins"),
            count = count
        ))
    }

    fun addStars(count: Long) {
        inventory.addItem(Item(
            template = itemTemplates.find("star"),
            count = count
        ))
    }

    fun spendCoins(count: Long) {
        inventory.removeItem(Item(
            template = itemTemplates.find("coins"),
            count = count
        ))
    }

    fun spendStars(count: Long) {
        inventory.removeItem(Item(
            template = itemTemplates.find("star"),
            count = count
        ))
    }

    private fun updateCoins() {
        val coinsItem = inventory.findItem("coins")
        coins = coinsItem?.count ?: 0

        val starsItem = inventory.findItem("star")
        stars = starsItem?.count ?: 0
    }

    init {
        if (launchSettings.debugMenu) {
            inventory.addItem(Item(itemTemplates.find("copper_sword")))
//            inventory.addItem(Item(itemTemplates.find("copper_shield")))
//            inventory.addItem(Item(itemTemplates.find("copper_armor")))
//            inventory.addItem(Item(itemTemplates.find("copper_helmet")))
//            inventory.addItem(Item(itemTemplates.find("copper_leg_armor")))
//            inventory.addItem(Item(itemTemplates.find("copper_boots")))
//
//            inventory.addItem(Item(itemTemplates.find("silver_sword")))
//            inventory.addItem(Item(itemTemplates.find("silver_shield")))
//            inventory.addItem(Item(itemTemplates.find("silver_armor")))
//            inventory.addItem(Item(itemTemplates.find("silver_helmet")))
//            inventory.addItem(Item(itemTemplates.find("silver_leg_armor")))
//            inventory.addItem(Item(itemTemplates.find("silver_boots")))
//
//            inventory.addItem(Item(itemTemplates.find("rainbow_sword")))
//
//            inventory.addItem(Item(itemTemplates.find("map_cave")))
//
//            inventory.addItem(Item(itemTemplates.find("basic_gather_yield_potion")))
//            inventory.addItem(Item(
//                template = itemTemplates.find("pine_wood"),
//                count = 100
//            ))
//            inventory.addItem(Item(itemTemplates.find("relic_computer")))
        }

        updateCoins()
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        val items = inventory.items.map { it.toSaveData() }
        gameSave.inventory = PlayerInventorySaveData(
            items = items,
            extraInventorySlotsPurchased = extraSlotsPurchased,
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        val items = gameSave.inventory.items.map { itemService.fromSaveData(it) }
        extraSlotsPurchased = gameSave.inventory.extraInventorySlotsPurchased
        maxSlots = 20 + extraSlotsPurchased
        items.forEach { inventory.addItem(it, emitEvent = false) }
    }
}
