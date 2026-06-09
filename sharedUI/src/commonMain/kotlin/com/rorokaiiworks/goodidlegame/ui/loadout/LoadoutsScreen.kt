package com.rorokaiiworks.goodidlegame.ui.loadout

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.window.core.layout.WindowSizeClass
import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.items.*
import com.rorokaiiworks.goodidlegame.core.loadouts.Loadout
import com.rorokaiiworks.goodidlegame.ui.commons.SelectItemPanel
import com.rorokaiiworks.goodidlegame.ui.isWideScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi


data class LoadoutsUiState(
    val isExchanging: Boolean = false,
    val selectedItemSlot: ItemSlot? = null,
    val selectedTabIndex: Int = 0
)

class LoadoutsScreenViewModel : ViewModel(), KoinComponent {
    private val _uiState = MutableStateFlow(LoadoutsUiState())
    val uiState = _uiState.asStateFlow()

    private val itemService: ItemService by inject()
    val i18n: I18n by inject()

    val loadoutsScrollerState = ScrollState(0)

    fun onSelectStatsTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun startExchange(itemSlot: ItemSlot) {
        _uiState.update {
            it.copy(isExchanging = true, selectedItemSlot = itemSlot)
        }
    }

    fun exchange(newItem: Item, from: IItemContainer, actor: IActor) {
        val selectedSlot = _uiState.value.selectedItemSlot ?: return

        val oldItem = selectedSlot.item

        if (oldItem != null) {
            itemService.unequipItem(item = oldItem, actor = actor)
            ItemTransfer.transferItem(
                to = from,
                from = selectedSlot,
                item = oldItem
            )
        }

        if (ItemTransfer.transferItem(
                to = selectedSlot,
                from = from,
                item = newItem.copy(count = 1)
        )
        ) {
            itemService.equipItem(item = newItem, actor = actor)
            _uiState.value = LoadoutsUiState()
        }
    }

    fun cancelExchange() {
        _uiState.value = LoadoutsUiState()
    }
}

@Composable
fun LoadoutsScreen(
    loadouts: List<Loadout>,
    actor: IActor,
    inventory: Inventory,
    viewModel: LoadoutsScreenViewModel = koinViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
) {
    val uiState by viewModel.uiState.collectAsState()
    val statsScrollState = rememberScrollState()

    if (isWideScreen(windowSizeClass)) {
        LoadoutsScreenWide(
            loadoutsScrollState = viewModel.loadoutsScrollerState,
            statsScrollState = statsScrollState,
            loadouts = loadouts,
            actor = actor,
            inventory = inventory,
            uiState = uiState,
            viewModel = viewModel
        )
    } else {
        LoadoutsScreenMobile(
            loadouts = loadouts,
            actor = actor,
            inventory = inventory,
            uiState = uiState,
            viewModel = viewModel,
            scrollState = viewModel.loadoutsScrollerState
        )
    }
}

@Composable
private fun LoadoutsScreenMobile(
    loadouts: List<Loadout>,
    actor: IActor,
    inventory: Inventory,
    uiState: LoadoutsUiState,
    scrollState: ScrollState,
    viewModel: LoadoutsScreenViewModel,
) {
    if (uiState.isExchanging) {
        ExchangePanel(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            uiState = uiState,
            viewModel = viewModel,
            inventory = inventory,
            actor = actor
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        for (loadout in loadouts) {
            LoadoutPanel(
                loadout = loadout,
                onClick = { viewModel.startExchange(it) },
                isWideScreen = false
            )
        }

        StatsPanel(
            uiState = uiState,
            viewModel = viewModel,
            actor = actor,
        )
    }
}


@Composable
private fun LoadoutsScreenWide(
    loadoutsScrollState: ScrollState,
    statsScrollState: ScrollState,
    loadouts: List<Loadout>,
    actor: IActor,
    inventory: Inventory,
    uiState: LoadoutsUiState,
    viewModel: LoadoutsScreenViewModel
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(loadoutsScrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            for (loadout in loadouts) {
                LoadoutPanel(
                    loadout = loadout,
                    onClick = { viewModel.startExchange(it) },
                    isWideScreen = true
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            if (uiState.isExchanging) {
                ExchangePanel(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    uiState = uiState,
                    viewModel = viewModel,
                    inventory = inventory,
                    actor = actor
                )
            } else {
                StatsPanel(
                    uiState = uiState,
                    viewModel = viewModel,
                    actor = actor,
                    scrollerState = statsScrollState
                )
            }
        }
    }
}


@Composable
private fun StatsPanel(
    uiState: LoadoutsUiState,
    viewModel: LoadoutsScreenViewModel,
    actor: IActor,
    scrollerState: ScrollState? = null
) {
    val modifier = if (scrollerState != null) {
        Modifier.verticalScroll(scrollerState)
    } else Modifier

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SecondaryScrollableTabRow(
            selectedTabIndex = uiState.selectedTabIndex,
        ) {
            actor.stats.sets.forEachIndexed { index, set ->
                Tab(
                    selected = index == uiState.selectedTabIndex,
                    onClick = { viewModel.onSelectStatsTab(index) },
                    text = { Text(viewModel.i18n.trc("stat_id", set.id)) },
                )
            }
        }

        val statSet = actor.stats.sets[uiState.selectedTabIndex]
        StatSetPanel(statSet, actor.effectManager.effects)
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ExchangePanel(
    modifier: Modifier = Modifier,
    uiState: LoadoutsUiState,
    viewModel: LoadoutsScreenViewModel,
    inventory: Inventory,
    actor: IActor
) {
    // TODO: 也许可以更激进地缓存一下
    val selections = remember(uiState.selectedItemSlot) {
        inventory.filterItemsByType(uiState.selectedItemSlot?.acceptType ?: emptySet())
    }

    SelectItemPanel(
        modifier = modifier,
        selectedItem = uiState.selectedItemSlot?.item,
        selections = selections,
        onSelect = {
            viewModel.exchange(
                newItem = it,
                from = inventory,
                actor
            )
        },
        onClose = viewModel::cancelExchange
    )
}
