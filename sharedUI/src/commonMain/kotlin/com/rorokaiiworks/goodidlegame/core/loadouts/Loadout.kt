package com.rorokaiiworks.goodidlegame.core.loadouts

import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.ItemSlot
import org.jetbrains.compose.resources.StringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class Loadout(
    val id: String,
    val name: String,
    val slots: List<ItemSlot>,
) : KoinComponent {
    private val logger: Logger by inject { parametersOf("Loadout: $id") }
    private val itemService: ItemService by inject()

    fun loadItem(itemSlot: ItemSlot, item: Item?, actor: IActor) {
        itemSlot.item = item

        item?.let {
            itemService.equipItem(
                item = it,
                actor = actor
            )
            logger.i { "Equipped item: ${it.template.id}" }
        }
    }
}