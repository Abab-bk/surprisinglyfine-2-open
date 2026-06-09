package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.Inventory
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun ConsumeItemEntry(
    consume: ItemEntry,
    itemTemplates: DataTable<ItemTemplate> = koinInject(named<ItemTemplate>()),
    inventory: Inventory,
    content: @Composable () -> Unit = { },
) {
    val itemTemplate = itemTemplates.find(consume.itemId)
    val ownedItem = inventory.findItem(itemTemplate.id)
    val ownedItemCount = ownedItem?.count ?: 0

    ItemTemplateEntry(
        itemTemplate = itemTemplate,
        nameColor = if (ownedItemCount < consume.count) MaterialTheme.colorScheme.error else null,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Humanizer.abbreviation(consume.count),
                color =
                    if (ownedItemCount >= consume.count) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${Humanizer.abbreviation(ownedItemCount)})",
                color =
                    if (ownedItemCount >= consume.count) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}


@Composable
fun ConsumeItemsContent(
    modifier: Modifier = Modifier,
    consumesModifier: Modifier = Modifier,
    consumes: List<ItemEntry>,
    itemTemplates: DataTable<ItemTemplate>,
    inventory: Inventory,
    i18n: I18n = koinInject(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(i18n.tr("Consumes"))

        Column(
            modifier = consumesModifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            consumes.forEach { consume ->
                ConsumeItemEntry(
                    consume = consume,
                    itemTemplates = itemTemplates,
                    inventory = inventory,
                )
            }
        }
    }
}