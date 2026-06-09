package com.rorokaiiworks.goodidlegame.core.items

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.StringResource

class ItemSlot(
    val id: String,
    val name: String,
    val acceptType: Set<ItemType>
) : IItemContainer {
    var item by mutableStateOf<Item?>(null)

    var isLocked: Boolean
        get() = item?.isLocked ?: false
        set(value) = item?.isLocked = value

    fun clearItem() {
        item = null
    }

    override fun addItem(item: Item, emitEvent: Boolean): Boolean {
        if (!canAddItem(item)) return false

        if (this.item == null) {
            this.item = item
            return true
        }

//        if (current.template.id == item.template.id) {
//            val total = current.count + item.count
//            current.count = minOf(total, current.template.maxStack)
//            return true
//        }

        return false
    }

    override fun removeItem(item: Item): Boolean {
        val current = this.item ?: return false
        if (current.template.id != item.template.id) return false
        if (current.count < item.count) return false

        if (current.count <= 1) {
            this.item = null
            return true
        }

        this.item = current.copy(count = current.count - item.count)
        return true
    }

    override fun hasItem(item: Item): Boolean {
        val current = this.item ?: return false
        return current.template.id == item.template.id && current.count >= item.count
    }

    override fun canAddItem(item: Item): Boolean {
        if (item.template.type !in acceptType) return false
        val current = this.item
        return current == null
    }
}