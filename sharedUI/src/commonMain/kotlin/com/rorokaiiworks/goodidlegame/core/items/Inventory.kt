package com.rorokaiiworks.goodidlegame.core.items

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.rorokaiiworks.goodidlegame.core.Result
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class Inventory(
    var maxSlots: Int,
    val items: SnapshotStateList<Item> = mutableStateListOf(),
    val onAddItem: (id: String, count: Long) -> Unit = { _, _ -> },
    val onRemoveItem: (id: String, count: Long) -> Unit = { _, _ -> }
) : IItemContainer, KoinComponent {
    val usedSlots: Int get() = items.size

    var coins by mutableLongStateOf(0)
        private set

    private fun updateCoins() {
        val coinsItem = findItem("coins")
        coins = coinsItem?.count ?: 0L
    }

    private val itemService: ItemService by inject()

    fun findItem(itemId: String): Item? {
        return items.firstOrNull { it.template.id == itemId }
    }

    override fun addItem(item: Item, emitEvent: Boolean): Boolean {
        if (!canAddItem(item)) return false

        var added: Long

        if (item.isUnique) {
            if (isFull()) return false

            items.add(item)
            added = item.count
        } else {
            val existingStack = items
                .firstOrNull { it.template.id == item.template.id && !it.isUnique }

            if (existingStack != null) {
                existingStack.count += item.count
                added = item.count
            } else {
                items.add(item)
                added = item.count
            }
        }

        if (emitEvent && added > 0) {
            updateCoins()
            onAddItem(item.template.id, added)
        }

        return added > 0
    }

    override fun removeItem(item: Item): Boolean {
        if (item.count <= 0) return false
        if (!item.isUnique) {
            val existingStack = items.firstOrNull { it.template.id == item.template.id } ?: return false

            if (existingStack.count < item.count) return false

            if (existingStack.count <= 1) {
                items.remove(existingStack)
                onRemoveItem(item.template.id, existingStack.count)
                return true
            }

            val newItem = existingStack.copy(count = existingStack.count - item.count)
            items.remove(existingStack)

            if (newItem.count > 0) items.add(newItem)

            onRemoveItem(item.template.id, item.count)
            return true
        }

        // is unique
        val existing = items.firstOrNull { it == item } ?: return false
        items.remove(existing)
        updateCoins()
        onRemoveItem(item.template.id, existing.count)
        return true
    }

    fun tryGetItem(id: String): Item? {
        val existing = items.firstOrNull { it.template.id == id } ?: return null
        return existing
    }

    override fun hasItem(item: Item): Boolean {
        if (item.isUnique) return items.contains(item)
        val existing = items.firstOrNull { it.template.id == item.template.id } ?: return false
        return existing.count >= item.count
    }

    fun canConsume(items: List<ItemEntry>): Boolean {
        return items
            .filter { it.count > 0 }
            .all { hasItem(itemService.createItem(it.itemId, it.count)) }
    }

    fun canConsumeResult(items: List<ItemEntry>): Result<Unit> {
        if (!canConsume(items)) return Result.Error(i18nWrapper("Items not enough"))
        return Result.Success(Unit)
    }

    fun removeItems(items: List<ItemEntry>) {
        items.forEach { removeItem(itemService.createItem(it.itemId, it.count)) }
    }

    fun addItemEntries(items: List<ItemEntry>) {
        items.forEach { addItem(itemService.createItem(it.itemId, it.count)) }
    }

    fun isFull(): Boolean = items.size >= maxSlots

    fun clear() {
        items.clear()
    }

    fun filterItemsByType(itemType: ItemType): List<Item> =
        items.filter { it.template.type == itemType }

    fun filterItemsByType(itemTypes: Set<ItemType>): List<Item> =
        items.filter { itemTypes.contains(it.template.type) }

    fun filterItemsByItemId(itemId: String): List<Item> =
        items.filter { it.template.id == itemId }

    override fun canAddItem(item: Item): Boolean {
        // 事实上，满槽只会导致无法开启新 Task
//        if (items.size >= maxSlots &&
//            !items.any { it.template.id == item.template.id && it.count < it.template.maxStack }) {
//            return false // 满槽 + 没有可填充的堆
//        }

        if (item.isUnique) {
            return items.size < maxSlots
        }

        return true
    }

    fun addItems(items: List<Item>) {
        items.forEach { addItem(it) }
    }
}