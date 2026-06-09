package com.rorokaiiworks.goodidlegame.core.items

object ItemTransfer {
    fun transferItem(
        from: IItemContainer,
        to: IItemContainer,
        item: Item,
    ): Boolean {
        if (!from.hasItem(item)) return false
        if (!to.canAddItem(item)) return false
        from.removeItem(item)
        to.addItem(item)
        return true
    }
}