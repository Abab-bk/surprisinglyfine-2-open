package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.feather.ArrowDown
import com.composables.icons.feather.ArrowUp
import com.composables.icons.feather.Feather
import com.rorokaiiworks.goodidlegame.ui.inventory.InventorySort
import com.rorokaiiworks.goodidlegame.ui.inventory.InventorySortType
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

enum class SortDirection {
    Ascending, Descending
}

@Composable
fun DirectionArrow(
    modifier: Modifier = Modifier,
    direction: SortDirection
) {
    Icon(
        modifier = modifier,
        imageVector =
            if (direction == SortDirection.Ascending) Feather.ArrowUp
            else Feather.ArrowDown,
        contentDescription = null
    )
}


@Composable
fun HeaderButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                maxLines = 1
            )

            content()
        }
    }
}


@Composable
fun InventoryItemListHeader(
    modifier: Modifier = Modifier,
    sort: InventorySort,
    i18n: I18n = koinInject(),
    onChangeSort: (InventorySortType) -> Unit,
    content: @Composable RowScope.() -> Unit = { },
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
                onClick = { onChangeSort(InventorySortType.Name) },
            ) {
                if (sort.type == InventorySortType.Name) {
                    DirectionArrow(direction = sort.direction)
                }
            }

            VerticalDivider()

            HeaderButton(
                modifier = Modifier.weight(1f),
                text = i18n.tr("Count"),
                onClick = { onChangeSort(InventorySortType.Count) },
            ) {
                if (sort.type == InventorySortType.Count) {
                    DirectionArrow(direction = sort.direction)
                }
            }

            VerticalDivider()

            HeaderButton(
                modifier = Modifier.weight(1f),
                text = i18n.tr("Price"),
                onClick = { onChangeSort(InventorySortType.Price) },
            ) {
                if (sort.type == InventorySortType.Price) {
                    DirectionArrow(direction = sort.direction)
                }
            }

            content()
        }
    }
}