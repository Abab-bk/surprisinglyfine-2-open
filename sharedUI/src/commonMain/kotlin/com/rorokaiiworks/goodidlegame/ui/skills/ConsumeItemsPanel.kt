package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.recipes.Recipe
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.commons.ConsumeItemEntry
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Plus
import com.rorokaiiworks.goodidlegame.core.items.Inventory
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun ConsumeItemsPanel(
    modifier: Modifier = Modifier,
    consumeItems: List<ItemEntry>,
    inventory: Inventory,
    onCraftBtnClick: (productId: String) -> Unit,
    i18n: I18n = koinInject(),
    itemTemplates: DataTable<ItemTemplate> = koinInject(named<ItemTemplate>()),
    recipes: DataTable<Recipe> = koinInject(named<Recipe>())
) {
    BaseCard(modifier = modifier) {
        CardTitle(title = i18n.tr("Consumes"))

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            for (consume in consumeItems) {
                val itemTemplate = itemTemplates.find(consume.itemId)

                ConsumeItemEntry(
                    consume = consume,
                    itemTemplates = itemTemplates,
                    inventory = inventory,
                ) {
                    if (recipes.all().any { it.product.itemId == itemTemplate.id }) {
                        IconButton(
                            modifier = Modifier.width(50.dp),
                            onClick = { onCraftBtnClick(itemTemplate.id) },
                        ) {
                            Icon(
                                imageVector = Feather.Plus,
                                contentDescription = "Craft"
                            )
                        }
                    }
                }
            }
        }
    }
}
