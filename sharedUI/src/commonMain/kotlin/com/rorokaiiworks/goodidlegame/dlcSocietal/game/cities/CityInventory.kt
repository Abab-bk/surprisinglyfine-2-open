package com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.Inventory
import com.rorokaiiworks.goodidlegame.core.items.ItemSaveData
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.CityFormulas
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.math.min

@Serializable
data class CityInventorySave(
    val items: List<ItemSaveData> = emptyList(),
)

class CityInventory : KoinComponent {
    private val itemService: ItemService by inject()
    private val buildingTemplates: DataTable<BuildingTemplate> by inject(named<BuildingTemplate>())

    val inventory = Inventory(
        maxSlots = Int.MAX_VALUE,
        onAddItem = { itemId, _ ->
            if (itemId == "isle_bucks") syncBucks()
        },
        onRemoveItem = { itemId, _ ->
            if (itemId == "isle_bucks") syncBucks()
        }
    )

    init {
        CoroutineScope(Dispatchers.Default).launch {
            delay(100)
            inventory.addItemEntries(CityFormulas.getInitialItems(buildingTemplates))
        }
    }

    var isleBucks by mutableLongStateOf(0)
        private set

    fun addIsleBucks(amount: Long) {
        addItemById("isle_bucks", amount)
    }

    fun spendIsleBucks(amount: Long) {
        removeItemById("isle_bucks", amount)
    }

    fun addItemById(itemId: String, count: Long): Boolean {
        if (count <= 0) return false
        return inventory.addItem(itemService.createItem(itemId, count))
    }

    fun removeItemById(itemId: String, count: Long): Long {
        if (count <= 0) return 0

        var remaining = count
        var removed = 0L

        while (remaining > 0) {
            val stack = inventory.findItem(itemId) ?: break
            val toRemove = min(remaining, stack.count)
            val success = inventory.removeItem(itemService.createItem(itemId, toRemove))
            if (!success) break

            removed += toRemove
            remaining -= toRemove
        }

        return removed
    }

    fun clear() {
        inventory.clear()
        inventory.addItemEntries(CityFormulas.getInitialItems(buildingTemplates))
        syncBucks()
    }

    private fun syncBucks() {
        isleBucks = inventory.findItem("isle_bucks")?.count ?: 0
    }

    fun toSave() = CityInventorySave(items = inventory.items.map { it.toSaveData() })

    fun fromSave(save: CityInventorySave) {
        save.items
            .map { itemService.fromSaveData(it) }
            .forEach { inventory.addItem(it, emitEvent = false) }
    }
}
