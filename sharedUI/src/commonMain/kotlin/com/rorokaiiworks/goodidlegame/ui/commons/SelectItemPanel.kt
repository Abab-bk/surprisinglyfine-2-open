package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemDetailPanel
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateDetailPanel
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun SelectItemPanel(
    modifier: Modifier = Modifier,
    selectedItem: Item?,
    selections: List<Item>,
    onSelect: (Item) -> Unit,
    onClose: () -> Unit,
    i18n: I18n = koinInject(),
    content: @Composable () -> Unit = { }
) = GenericSelectPanel(
    modifier = modifier,
    selected = selectedItem,
    selections = selections,
    onSelect = onSelect,
    detailPanel = { item ->
        ItemDetailPanel(
            item = item,
            title = { CardTitleWithCloseBtn(i18n.tr("Current Item"), onClose = onClose) }
        )
    },
    selectionPanel = { list, onPick ->
        SelectFromPanel(selections = list, onSelect = onPick)
    },
    content = content
)


@Composable
fun SelectItemTemplatePanel(
    modifier: Modifier = Modifier,
    selectedItem: ItemTemplate?,
    selections: List<ItemTemplate>,
    onSelect: (ItemTemplate) -> Unit,
    onClose: () -> Unit,
    i18n: I18n = koinInject(),
    content: @Composable () -> Unit = { }
) = GenericSelectPanel(
    modifier = modifier,
    selected = selectedItem,
    selections = selections,
    onSelect = onSelect,
    detailPanel = { template ->
        ItemTemplateDetailPanel(
            item = template,
            title = { CardTitleWithCloseBtn(i18n.tr("Current Item"), onClose = onClose) }
        )
    },
    selectionPanel = { list, onPick ->
        SelectTemplateFromPanel(selections = list, onSelect = onPick)
    },
    content = content
)


@Composable
fun <T> GenericSelectPanel(
    modifier: Modifier = Modifier,
    selected: T?,
    selections: List<T>,
    onSelect: (T) -> Unit,
    detailPanel: @Composable (T?) -> Unit,
    selectionPanel: @Composable (List<T>, (T) -> Unit) -> Unit,
    content: @Composable () -> Unit = { }
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        detailPanel(selected)
        selectionPanel(selections, onSelect)
        content()
    }
}