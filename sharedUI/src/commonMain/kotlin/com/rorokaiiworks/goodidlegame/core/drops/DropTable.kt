package com.rorokaiiworks.goodidlegame.core.drops

import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi

@Serializable
class DropTable(
    val entries: List<DropEntry>,
) {
    private val alwaysEntries: List<DropEntry> by lazy {
        entries.filter { it.isAlways }
    }

    private val chanceEntries: List<DropEntry> by lazy {
        entries.filter { !it.isAlways }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun pick(itemTemplates: DataTable<ItemTemplate>): List<Item> {
        val droppedItems = mutableListOf<Item>()
        for (entry in alwaysEntries) {
            val amount = Random.nextInt(entry.min, entry.max + 1)
            val template = itemTemplates.find(entry.itemId)
            repeat(amount) { droppedItems += Item(template) }
        }

        for (entry in chanceEntries) {
            val randomValue = Random.nextInt(TOTAL_WEIGHT)
            if (randomValue < entry.chance) {
                val amount = Random.nextInt(entry.min, entry.max + 1)
                val template = itemTemplates.find(entry.itemId)
                repeat(amount) { droppedItems += Item(template) }
            }
        }

        return droppedItems
    }

    companion object {
        const val TOTAL_WEIGHT = 100
    }
}