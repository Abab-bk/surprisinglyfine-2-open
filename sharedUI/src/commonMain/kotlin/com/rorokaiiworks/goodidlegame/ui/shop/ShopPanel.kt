package com.rorokaiiworks.goodidlegame.ui.shop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.shop.ShopItem
import com.rorokaiiworks.goodidlegame.ui.commons.*
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemRow
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

data class ShopSort(
    val type: ShopSortType,
    val direction: SortDirection
)

enum class ShopSortType {
    Name, Price
}

@Composable
fun ShopPanel(
    modifier: Modifier = Modifier,
    queryState: TextFieldState,
    items: List<ShopItem>,
    sort: ShopSort,
    selectedItem: ShopItem?,
    coins: Long,
    itemsListState: LazyListState,
    i18n: I18n = koinInject(),
    onItemClick: (ShopItem) -> Unit,
    onChangeSort: (ShopSortType) -> Unit
) {
    val filteredItems by remember(queryState.text, sort, items) {
        derivedStateOf {
            var result = items.filter { it.isAvailable() }

            if (queryState.text.isNotBlank()) {
                result = result.filter {
                    it.getName().contains(queryState.text, ignoreCase = true)
                }
            }

            result = when (sort.type) {
                ShopSortType.Name ->
                    if (sort.direction == SortDirection.Ascending) result.sortedBy { it.getName() }
                    else result.sortedByDescending { it.getName() }

                ShopSortType.Price ->
                    if (sort.direction == SortDirection.Ascending) result.sortedBy { it.getPrice(1) }
                    else result.sortedByDescending { it.getPrice(1) }
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

        ShopItemListHeader(
            modifier = Modifier.weight(1f),
            sort = sort,
            onChangeSort = onChangeSort
        )

        LazyColumn(
            state = itemsListState,
            modifier = Modifier.weight(18f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(filteredItems, key = { it.id }) { item ->
                ItemRow(
                    name = item.getName(),
                    count = null,
                    rarity = item.rarity,
                    iconId = item.iconId,
                    price = item.getPrice(1),
                    isSelected = item == selectedItem,
                    onClick = { onItemClick(item) }
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameImage(
                    iconName = "coins",
                    modifier = Modifier.size(28.dp)
                )

                Text(
                    text = Humanizer.abbreviation(coins),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun ShopItemListHeader(
    modifier: Modifier = Modifier,
    sort: ShopSort,
    i18n: I18n = koinInject(),
    onChangeSort: (ShopSortType) -> Unit
) {
    Surface(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderButton(
                modifier = Modifier.weight(1f),
                text = i18n.tr("Name"),
                onClick = { onChangeSort(ShopSortType.Name) },
            ) {
                if (sort.type == ShopSortType.Name) {
                    DirectionArrow(direction = sort.direction)
                }
            }

            VerticalDivider()

            HeaderButton(
                modifier = Modifier.weight(1f),
                text = i18n.tr("Price"),
                onClick = { onChangeSort(ShopSortType.Price) },
            ) {
                if (sort.type == ShopSortType.Price) {
                    DirectionArrow(direction = sort.direction)
                }
            }
        }
    }
}