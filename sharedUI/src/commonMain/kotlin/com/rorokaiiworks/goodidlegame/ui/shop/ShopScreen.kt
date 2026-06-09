@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.events.ToastType
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.shop.Shop
import com.rorokaiiworks.goodidlegame.core.shop.ShopItem
import com.rorokaiiworks.goodidlegame.ui.commons.ShopItemPurchasePanel
import com.rorokaiiworks.goodidlegame.ui.commons.SortDirection
import com.rorokaiiworks.goodidlegame.ui.isWideScreen
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi

class ShopScreenViewModel : ViewModel(), KoinComponent {
    val playerInventory: PlayerInventory by inject()
    val eventBus: EventBus by inject()
    val shop: Shop by inject()
    val i18n: I18n by inject()

    var selectedItem: ShopItem? by mutableStateOf(null)
    var sort: ShopSort by mutableStateOf(
        ShopSort(ShopSortType.Name, SortDirection.Ascending)
    )

    val itemsListState = LazyListState()
    val queryState = TextFieldState()

    fun onChangeSort(sortType: ShopSortType) {
        sort = if (sort.type == sortType) {
            sort.copy(direction =
                if (sort.direction == SortDirection.Ascending) SortDirection.Descending
                else SortDirection.Ascending
            )
        } else {
            sort.copy(type = sortType)
        }
    }

    fun onClose() {
        selectedItem = null
    }

    fun onSelectItem(item: ShopItem) {
        selectedItem = item
    }

    fun purchase(shopItem: ShopItem, count: Long) {
        val result = shop.tryPurchase(shopItem = shopItem, count = count)
        if (result is Resource.Error) {
            viewModelScope.launch {
                eventBus.emit(
                    IEvent.ToastMessage(
                        msg = i18n.tr("Not enough coins"),
                        toastType = ToastType.Error
                    )
                )
            }
        }
    }
}

@Composable
fun ShopScreen(
    viewModel: ShopScreenViewModel = koinViewModel(),
) {
    val isWideScreen = isWideScreen()

    when (isWideScreen) {
        true -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShopPanel(
                    modifier = Modifier.weight(1f),
                    items = viewModel.shop.shopItems,
                    selectedItem = viewModel.selectedItem,
                    onItemClick = { viewModel.onSelectItem(it) },
                    sort = viewModel.sort,
                    onChangeSort = viewModel::onChangeSort,
                    coins = viewModel.playerInventory.coins,
                    itemsListState = viewModel.itemsListState,
                    queryState = viewModel.queryState
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (viewModel.selectedItem != null) {
                        ShopItemPurchasePanel(
                            selectedItem = viewModel.selectedItem!!,
                            playerCoins = viewModel.playerInventory.coins,
                            onPerformPurchase = viewModel::purchase,
                            onClose = { viewModel.onClose() },
                            inputState = rememberTextFieldState()
                        )
                    }
                }
            }
        }
        false -> {
            if (viewModel.selectedItem != null) {
                ShopItemPurchasePanel(
                    selectedItem = viewModel.selectedItem!!,
                    playerCoins = viewModel.playerInventory.coins,
                    onPerformPurchase = viewModel::purchase,
                    onClose = { viewModel.onClose() },
                    inputState = rememberTextFieldState()
                )
                return
            }

            ShopPanel(
                modifier = Modifier.fillMaxSize(),
                items = viewModel.shop.shopItems,
                selectedItem = viewModel.selectedItem,
                onItemClick = { viewModel.onSelectItem(it) },
                sort = viewModel.sort,
                onChangeSort = viewModel::onChangeSort,
                coins = viewModel.playerInventory.coins,
                itemsListState = viewModel.itemsListState,
                queryState = viewModel.queryState,
            )
        }
    }
}