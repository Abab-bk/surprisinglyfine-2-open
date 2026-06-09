package com.rorokaiiworks.goodidlegame.ui.starStore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.starStore.StarStore
import com.rorokaiiworks.goodidlegame.core.starStore.StarStoreItem
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.commons.CoinsLabel
import com.rorokaiiworks.goodidlegame.ui.commons.GameDialog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


data class StarStoreScreenUiState(
    val selectedItem: StarStoreItem? = null,
    val purchaseSuccess: Boolean = false,
    val purchaseFailed: Boolean = false,
    val isLoading: Boolean = false,
)


class StarStoreScreenViewModel : ViewModel(), KoinComponent {
    val playerInventory: PlayerInventory by inject()
    private val starStore: StarStore by inject()

    private val _uiState = MutableStateFlow(StarStoreScreenUiState())
    val uiState = _uiState.asStateFlow()

    fun selectItem(starStoreItem: StarStoreItem?) {
        _uiState.update {
            it.copy(selectedItem = starStoreItem)
        }
    }

    fun dismissResultDialogs() {
        _uiState.update {
            it.copy(
                purchaseSuccess = false,
                purchaseFailed = false,
                selectedItem = null
            )
        }
    }

    fun buyItem() {
        val item = uiState.value.selectedItem ?: return

        _uiState.update {
            it.copy(isLoading = true)
        }

        viewModelScope.launch {
            val result = starStore.buyItem(item)
            when (result) {
                is Resource.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        purchaseFailed = true,
                    )
                }
                is Resource.Success<*> -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            purchaseSuccess = true,
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun StarStoreScreen(
    i18n: I18n = koinInject(),
    starStore: StarStore = koinInject(),
    viewModel: StarStoreScreenViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoinsLabel(
            value = viewModel.playerInventory.stars,
            iconName = "star"
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val items = starStore.items.all()
                items(
                    count = items.count(),
                    key = { index -> items[index].id }
                ) { index ->
                    StarStoreItemCard(
                        modifier = Modifier.height(100.dp),
                        starStoreItem = items[index],
                        onClick = { viewModel.selectItem(items[index]) }
                    )
                }
            }

            if (uiState.isLoading) {
                GameDialog(
                    title = i18n.tr("Loading"),
                    onDismissRequest = {},
                    content = {},
                )
            }

            if (uiState.selectedItem != null) {
                GameDialog(
                    title = i18n.tr("Buy"),
                    onDismissRequest = { viewModel.selectItem(null) },
                    onConfirmation = { viewModel.buyItem() }
                ) {
                    if (uiState.selectedItem?.isAdNeeded == true) {
                        Text(
                            text = i18n.tr(
                                "Watch {0} Ad for {1}?",
                                uiState.selectedItem?.price ?: 0,
                                uiState.selectedItem?.name ?: ""
                            )
                        )
                    } else {
                        Text(
                            text = i18n.tr(
                                "Spend {0} stars for {1}?",
                                uiState.selectedItem?.price ?: 0,
                                uiState.selectedItem?.name ?: ""
                            )
                        )
                    }
                }
            }

            if (uiState.purchaseSuccess) {
                GameDialog(
                    title = i18n.tr("Success"),
                    onDismissRequest = { viewModel.dismissResultDialogs() },
                ) {
                    Text(text = i18n.tr("Successfully purchased %s", uiState.selectedItem?.name ?: ""))
                }
            }

            if (uiState.purchaseFailed) {
                GameDialog(
                    title = i18n.tr("Failed"),
                    onDismissRequest = { viewModel.dismissResultDialogs() },
                ) {
                    Text(text = i18n.tr("Failed to purchase %s", uiState.selectedItem?.name ?: ""))
                }
            }
        }
    }
}
