package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemModifiersPanel
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun SelectFromPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    selections: List<Item>,
    i18n: I18n = koinInject(),
    onSelect: (Item) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BaseCard {
            if (title == null) {
                CardTitle(title = i18n.tr("Inventory"))
            } else {
                CardTitle(title)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (item in selections) {
                    ItemTemplateEntry(
                        nameOverride = item.displayName,
                        itemTemplate = item.template,
                        columnContent = {
                            ItemModifiersPanel(
                                modifiers = item.allModifiers
                            )
                        },
                        onClick = {
                            onSelect(item)
                        }
                    ) {
                        Text(
                            text = Humanizer.abbreviation(item.count),
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun SelectTemplateFromPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    selections: List<ItemTemplate>,
    i18n: I18n = koinInject(),
    onSelect: (ItemTemplate) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BaseCard {
            if (title == null) {
                CardTitle(title = i18n.tr("Inventory"))
            } else {
                CardTitle(title)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (item in selections) {
                    ItemTemplateEntry(
                        itemTemplate = item,
                        columnContent = {
                            item.modifiers?.let {
                                ItemModifiersPanel(
                                    modifiers = it
                                )
                            }
                        },
                        onClick = {
                            onSelect(item)
                        }
                    )
                }
            }
        }
    }
}