package com.rorokaiiworks.goodidlegame.dlcSocietal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.settings.ThemePreference.Companion.isDark
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.commons.InventoryItemListHeader
import com.rorokaiiworks.goodidlegame.ui.commons.SortDirection
import com.rorokaiiworks.goodidlegame.ui.inventory.InventorySort
import com.rorokaiiworks.goodidlegame.ui.inventory.InventorySortType
import com.rorokaiiworks.goodidlegame.ui.inventory.rarityBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi

data class CityInventoryScreenUiState(
    val selectedItem: Item?,
)

class CityInventoryScreenViewModel : ViewModel(), KoinComponent {
    val cityInventory: CityInventory by inject()
    var sort: InventorySort by mutableStateOf(
        InventorySort(InventorySortType.Name, SortDirection.Ascending)
    )

    private val _uiState = MutableStateFlow(CityInventoryScreenUiState(
        selectedItem = null
    ))
    val uiState = _uiState.asStateFlow()

    fun selectItem(item: Item?) {
        _uiState.update { it.copy(selectedItem = item) }
    }

    fun onChangeSort(sortType: InventorySortType) {
        sort = if (sort.type == sortType) {
            sort.copy(direction =
                if (sort.direction == SortDirection.Ascending) SortDirection.Descending
                else SortDirection.Ascending
            )
        } else {
            sort.copy(type = sortType)
        }
    }
}


@OptIn(ExperimentalUuidApi::class)
@Composable
fun CityInventoryScreen(
    viewModel: CityInventoryScreenViewModel = koinViewModel()
) {
    val inventoryItems = viewModel.cityInventory.inventory.items
    val sortedItems by remember(inventoryItems, viewModel.sort) {
        derivedStateOf {
            var result = inventoryItems.toList()
            result = when (viewModel.sort.type) {
                InventorySortType.Name ->
                    if (viewModel.sort.direction == SortDirection.Ascending) result.sortedBy { it.displayName }
                    else result.sortedByDescending { it.displayName }

                InventorySortType.Price ->
                    if (viewModel.sort.direction == SortDirection.Ascending) result.sortedBy { it.template.price }
                    else result.sortedByDescending { it.template.price }

                InventorySortType.Count ->
                    if (viewModel.sort.direction == SortDirection.Ascending) result.sortedBy { it.count }
                    else result.sortedByDescending { it.count }
            }
            result
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        InventoryItemListHeader(
            modifier = Modifier.weight(1f),
            sort = viewModel.sort,
            onChangeSort = viewModel::onChangeSort
        )

        LazyColumn(
            modifier = Modifier.weight(19f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(sortedItems, key = { it.uuid }) { item ->
                CityInventoryRow(item = item)
            }
        }
    }
}


@Composable
private fun CityInventoryRow(
    item: Item,
    i18n: I18n = koinInject(),
    settingsSaver: SettingsSaver = koinInject(),
) {
    val totalPrice = item.template.price * item.count
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .rarityBackground(
                rarity = item.template.rarity,
                isDark = settingsSaver.settings.value.themePreference.isDark()
            ),
        color = Color.Transparent,
        shape = RectangleShape,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameImage(
                    modifier = Modifier.size(32.dp),
                    iconName = item.template.id
                )

                Text(
                    text = i18n.tr(item.displayName),
                )
            }

            Text(
                text = "x${Humanizer.abbreviation(item.count)}",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                if (totalPrice > 0) {
                    Text(
                        text = Humanizer.abbreviation(totalPrice),
                    )

                    GameImage(
                        modifier = Modifier.size(24.dp),
                        iconName = "coins"
                    )
                }
            }
        }
    }
}
