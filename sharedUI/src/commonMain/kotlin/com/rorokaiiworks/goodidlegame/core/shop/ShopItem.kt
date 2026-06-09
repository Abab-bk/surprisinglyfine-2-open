package com.rorokaiiworks.goodidlegame.core.shop

import com.rorokaiiworks.goodidlegame.core.GameFormulas
import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.items.ItemRarity
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.requirements.Requirement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
sealed interface ShopItem : Template {
    val iconId: String
    val requirements: List<Requirement>
    val rarity: ItemRarity

    fun getName(): String
    fun getPrice(count: Long): Long
    fun getOwnedCount(): Long

    fun isAvailable(): Boolean = requirements.all { it.isMet() }
    fun onPurchase(count: Long)

    @Serializable
    @SerialName("getItemShopItem")
    data class GetItemShopItem(
        override val id: String,
        val itemId: String,
        override val rarity: ItemRarity,
        override val iconId: String = itemId,
        override val requirements: List<Requirement> = emptyList(),
    ): KoinComponent, ShopItem {
        private val playerInventory: PlayerInventory by inject()
        private val itemService: ItemService by inject()
        private val i18n: I18n by inject()

        override fun getName(): String {
            return i18n.tr(itemService.findItemTemplate(itemId).name)
        }

        override fun getPrice(count: Long): Long {
            val item = itemService.findItemTemplate(itemId)
            return GameFormulas.calculateItemPriceForPurchase(
                tier = item.tier,
                itemType = item.type
            ) * count
        }

        override fun getOwnedCount(): Long {
            return playerInventory.inventory.tryGetItem(itemId)?.count ?: 0L
        }

        override fun onPurchase(count: Long) {
            val item = itemService.createItem(
                itemId = itemId,
                count = count,
            )
            playerInventory.inventory.addItem(item)
        }
    }


    @Serializable
    @SerialName("inventory_slot")
    object InventorySlotShopItem : ShopItem, KoinComponent {
        override val id: String = "shop_inventory_slot"
        override val iconId: String = "inventory"
        override val requirements: List<Requirement> = emptyList()
        override val rarity: ItemRarity get() = ItemRarity.Rare

        private val playerInventory: PlayerInventory by inject()
        private val i18n: I18n by inject()

        override fun getPrice(count: Long): Long {
            var currentPurchased = playerInventory.extraSlotsPurchased
            var currentPrice = 0L

            for (i in 0 until count) {
                currentPrice += 100 * (currentPurchased + 1)
                currentPurchased++
            }

            return minOf(currentPrice, 10000L)
        }

        override fun getOwnedCount(): Long {
            return playerInventory.extraSlotsPurchased.toLong()
        }

        override fun getName(): String {
            return i18n.tr("Inventory Slots")
        }

        override fun onPurchase(count: Long) {
            playerInventory.addExtraSlots(count.toInt())
        }
    }
}
