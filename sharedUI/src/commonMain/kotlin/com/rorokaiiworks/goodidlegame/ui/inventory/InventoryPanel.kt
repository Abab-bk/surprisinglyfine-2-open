package com.rorokaiiworks.goodidlegame.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.Inventory
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.commons.GameTextFieldThin
import com.rorokaiiworks.goodidlegame.ui.commons.InventoryItemListHeader
import com.rorokaiiworks.goodidlegame.ui.commons.SortDirection
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi

data class InventorySort(
    val type: InventorySortType,
    val direction: SortDirection
)

enum class InventorySortType {
    Name, Price, Count
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun InventoryPanel(
    modifier: Modifier = Modifier,
    inventory: Inventory,
    sort: InventorySort,
    selectedItem: Item?,
    i18n: I18n = koinInject(),
    queryState: TextFieldState,
    itemsListState: LazyListState,
    onItemClick: (Item) -> Unit,
    onChangeSort: (InventorySortType) -> Unit
) {
    val filteredItems by remember(queryState.text, sort, inventory.items) {
        derivedStateOf {
            var result = inventory.items.toList()

            if (queryState.text.isNotBlank()) {
                result = result.filter {
                    it.displayName.contains(queryState.text, ignoreCase = true)
                }
            }

            result = when (sort.type) {
                InventorySortType.Name ->
                    if (sort.direction == SortDirection.Ascending) result.sortedBy { it.displayName }
                    else result.sortedByDescending { it.displayName }

                InventorySortType.Price ->
                    if (sort.direction == SortDirection.Ascending) result.sortedBy { it.template.price }
                    else result.sortedByDescending { it.template.price }

                InventorySortType.Count ->
                    if (sort.direction == SortDirection.Ascending) result.sortedBy { it.count }
                    else result.sortedByDescending { it.count }
            }

            result
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GameTextFieldThin(
            modifier = Modifier.fillMaxWidth().weight(1.5f),
            placeholder = { Text(text = i18n.tr("Search")) },
            state = queryState
        )

        InventoryItemListHeader(
            modifier = Modifier.weight(1f),
            sort = sort,
            onChangeSort = onChangeSort
        )

        LazyColumn(
            modifier = Modifier.weight(18f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            state = itemsListState
        ) {
            items(filteredItems, key = { it.uuid }) { item ->
                ItemRow(
                    name = item.displayName,
                    count = item.count,
                    rarity = item.template.rarity,
                    iconId = item.template.id,
                    price = item.template.price * item.count,
                    onClick = { onItemClick(item) },
                    isSelected = item == selectedItem
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = i18n.tr("Slots"))

                    Text(
                        text = "${inventory.usedSlots}/${inventory.maxSlots}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GameImage(
                        iconName = "coins",
                        modifier = Modifier.size(28.dp)
                    )

                    Text(
                        text = Humanizer.abbreviation(inventory.coins),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}




