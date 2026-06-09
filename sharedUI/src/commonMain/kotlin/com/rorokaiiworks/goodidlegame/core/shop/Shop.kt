package com.rorokaiiworks.goodidlegame.core.shop

import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemRarity
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.requirements.Requirement
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Shop(
    dataTable: DataTable<ShopItem>,
    skillActionsTable: DataTable<SkillAction>,
    itemTemplates: DataTable<ItemTemplate>
) : KoinComponent {
    private val staticShopItems: List<ShopItem> = dataTable.all()

    private val dynamicShopItems: List<ShopItem> = generateShopItemsFromSkillActions(
        skillActionsTable = skillActionsTable,
        existingItems = staticShopItems,
        itemTemplates = itemTemplates
    )

    val shopItems: List<ShopItem> by lazy {
        buildList {
            // Static items are always listed first.
            addAll(sortByType(staticShopItems))
            addAll(sortByType(dynamicShopItems))
            add(ShopItem.InventorySlotShopItem)
        }
    }

    private val playerInventory: PlayerInventory by inject()
    private val itemService: ItemService by inject()

    private data class UnlockRequirement(
        val skillId: String,
        val level: Int,
        val actionId: String
    )

    private fun generateShopItemsFromSkillActions(
        skillActionsTable: DataTable<SkillAction>,
        existingItems: List<ShopItem>,
        itemTemplates: DataTable<ItemTemplate>
    ): List<ShopItem> {
        val existingGetItemShopItems = existingItems.filterIsInstance<ShopItem.GetItemShopItem>()
        val existingItemIds = existingGetItemShopItems.map { it.itemId }.toSet()
        val existingShopIds = existingItems.map { it.id }.toMutableSet()

        val unlockByItemId = mutableMapOf<String, UnlockRequirement>()

        skillActionsTable.all().forEach { action ->
            val itemId = action.getAlwaysDropItemId() ?: return@forEach
            if (itemId in existingItemIds) return@forEach
            if (itemTemplates.findOrNull(itemId)?.perk != null) return@forEach

            val candidate = UnlockRequirement(
                skillId = action.skillId,
                level = action.requiredLevel,
                actionId = action.id
            )

            val current = unlockByItemId[itemId]
            val shouldUpdate = current == null ||
                candidate.level < current.level ||
                (candidate.level == current.level && candidate.actionId < current.actionId)

            if (shouldUpdate) {
                unlockByItemId[itemId] = candidate
            }
        }

        return unlockByItemId.entries
            .sortedBy { it.key }
            .map { (itemId, unlock) ->
                val requirements = if (unlock.level <= 0) {
                    emptyList()
                } else {
                    listOf(Requirement.SkillRequirement(unlock.skillId, unlock.level))
                }

                val preferredId = "shop_from_skill_action_$itemId"
                var uniqueId = preferredId
                var suffix = 1

                while (uniqueId in existingShopIds) {
                    uniqueId = "${preferredId}_$suffix"
                    suffix++
                }
                existingShopIds += uniqueId

                ShopItem.GetItemShopItem(
                    id = uniqueId,
                    itemId = itemId,
                    rarity = itemTemplates.findOrNull(itemId)?.rarity ?: ItemRarity.Common,
                    requirements = requirements
                )
            }
    }

    private fun sortByType(items: List<ShopItem>): List<ShopItem> {
        return items.sortedWith(
            compareBy(
                { item ->
                    when (item) {
                        is ShopItem.GetItemShopItem -> itemService
                            .tryFindItemTemplate(item.itemId)
                            ?.type
                            ?.ordinal ?: Int.MAX_VALUE

                        else -> Int.MAX_VALUE
                    }
                },
                { item ->
                    when (item) {
                        is ShopItem.GetItemShopItem -> item.itemId
                        else -> item.id
                    }
                }
            )
        )
    }

    fun tryPurchase(shopItem: ShopItem, count: Long): Resource<Unit> {
        val totalPrice = shopItem.getPrice(count)

        if (playerInventory.coins < totalPrice) {
            return Resource.Error(
                code = 404,
                message = "not_enough_coins"
            )
        }

        shopItem.onPurchase(count)
        playerInventory.spendCoins(totalPrice)

        return Resource.Success(Unit)
    }
}
