package com.rorokaiiworks.goodidlegame.core.items

interface IItemContainer {
    fun addItem(item: Item, emitEvent: Boolean = true): Boolean
    fun removeItem(item: Item): Boolean
    fun hasItem(item: Item): Boolean
    fun canAddItem(item: Item): Boolean
}