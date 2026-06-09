@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.core.items

import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.props.FoodProp
import com.rorokaiiworks.goodidlegame.core.props.PotionProp
import com.rorokaiiworks.goodidlegame.core.props.Prop
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import com.rorokaiiworks.goodidlegame.core.traits.TraitSystem
import com.rorokaiiworks.goodidlegame.core.players.Player
import kotlin.uuid.ExperimentalUuidApi

class ItemService(
    private val itemTemplates: DataTable<ItemTemplate>,
    private val traitSystem: TraitSystem,
) {
    fun cloneItem(item: Item): Item {
        val newItem = item.copy().also {
            it.customModifiers = item.customModifiers
            it.enchantmentLevel = item.enchantmentLevel
        }

        return newItem
    }

    fun createItem(
        itemId: String,
        count: Long = 1,
    ): Item {
        val template = itemTemplates.find(itemId)
        return Item(
            template = template,
            count = count,
        )
    }

    fun findItemTemplate(itemId: String): ItemTemplate {
        return itemTemplates.find(itemId)
    }

    fun tryFindItemTemplate(itemId: String): ItemTemplate? {
        return itemTemplates.findOrNull(itemId)
    }

    fun tryFindByName(name: String): ItemTemplate? {
        return itemTemplates.all().find {
            it.name == name
        }
    }

    fun itemToProp(item: Item): Prop? {
        val template = item.template

        return when (template.type) {
            in ItemType.Potions -> {
                PotionProp(template)
            }
            ItemType.Food -> {
                FoodProp(template)
            }
            else -> {
                null
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun equipItem(item: Item, actor: IActor) {
        val effect = Effect(
            id = item.template.id,
            source = item.template.id,
            sourceName = item,
            modifiers = item.allModifiers
        )
        actor.effectManager.addEffect(effect)
        item.template.perk?.let { perk ->
            if (actor is Player) {
                traitSystem.activateExternalPerk(perk, item)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun unequipItem(item: Item, actor: IActor): Boolean {
        val removed = actor.effectManager.removeAllEffectsBySource(item.template.id)
        item.template.perk?.let { perk ->
            if (actor is Player) {
                traitSystem.deactivateExternalPerk(perk.id)
            }
        }
        return removed
    }

    fun fromSaveData(itemSaveData: ItemSaveData): Item {
        val template = findItemTemplate(itemSaveData.itemId)
        return Item(
            template = template,
            count = itemSaveData.count,
        ).apply {
            this.customModifiers = itemSaveData.customModifiers
            this.enchantmentLevel = itemSaveData.enchantmentLevel
        }
    }
}
