@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.ui.community

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Plus
import com.rorokaiiworks.goodidlegame.core.community.Altar
import com.rorokaiiworks.goodidlegame.core.community.AltarSlot
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.ui.commons.GameDialog
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemEntryPanel
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemModifiersPanel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.uuid.ExperimentalUuidApi


sealed interface AltarScreenUiState {
    object Idle : AltarScreenUiState

    data class SelectingItem(
        val selected: Item?,
        val selectedSlot: AltarSlot
    ) : AltarScreenUiState

    data class ConfirmRemove(
        val slot: AltarSlot,
        val item: Item
    ) : AltarScreenUiState
}


class AltarScreenViewModel : ViewModel(), KoinComponent {
    val playerInventory: PlayerInventory by inject()
    val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())

    val i18n: I18n by inject()

    private val _uiState = MutableStateFlow<AltarScreenUiState>(
        value = AltarScreenUiState.Idle,
    )
    val uiState = _uiState.asStateFlow()

    val inventory: PlayerInventory get() = playerInventory

    fun clickSlot(slot: AltarSlot) {
        if (slot.item != null) {
            _uiState.update {
                AltarScreenUiState.ConfirmRemove(slot = slot, item = slot.item!!)
            }
            return
        }
        _uiState.update {
            AltarScreenUiState.SelectingItem(selected = null, selectedSlot = slot)
        }
    }

    fun closeDialog() {
        _uiState.update { AltarScreenUiState.Idle }
    }

    fun selectItem(item: Item, altarSlot: AltarSlot) {
        _uiState.update {
            AltarScreenUiState.SelectingItem(
                selected = item,
                selectedSlot = altarSlot
            )
        }
    }

    fun confirmSelectingItemDialog(uiState: AltarScreenUiState.SelectingItem, altar: Altar) {
        uiState.selected?.let {
            playerInventory.inventory.removeItem(it.copy(count = 1))
            altar.place(uiState.selectedSlot, it)
        }
        _uiState.update { AltarScreenUiState.Idle }
    }

    fun confirmRemove(
        state: AltarScreenUiState.ConfirmRemove,
        altar: Altar
    ) {
        val removedItem = state.slot.item ?: return
        playerInventory.inventory.addItem(removedItem)
        altar.remove(state.slot)
        _uiState.update { AltarScreenUiState.Idle }
    }
}


@Composable
fun AltarScreen(
    modifier: Modifier = Modifier,
    altar: Altar,
    viewModel: AltarScreenViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        FlowRow(
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            altar.slots.forEach { slot ->
                AltarSlotPanel(
                    modifier = Modifier.weight(1f),
                    altarSlot = slot,
                    onClick = { viewModel.clickSlot(slot) }
                )
            }
        }

        CommunityCostEntry(
            costs = altar.getSlotPurchaseCost(),
            itemTemplates = viewModel.itemTemplates,
            playerInventory = viewModel.playerInventory,
            enabled = altar.canPurchaseSlot(),
            onClick = {
                altar.purchaseSlot(
                    inventory = viewModel.playerInventory.inventory
                )
            },
            text = if (altar.canPurchaseSlot()) viewModel.i18n.tr("Purchase Slot") else viewModel.i18n.tr("Max Slots")
        )
    }

    if (uiState is AltarScreenUiState.SelectingItem) {
        val selectedState = uiState as AltarScreenUiState.SelectingItem
        GameDialog(
            title = viewModel.i18n.tr("Selections"),
            fullScreen = true,
            onDismissRequest = viewModel::closeDialog,
            onConfirmation = { viewModel.confirmSelectingItemDialog(selectedState, altar) }
        ) {
            LazyVerticalStaggeredGrid(
                modifier = Modifier.fillMaxSize(),
                columns = StaggeredGridCells.Adaptive(minSize = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
            ) {
                items(
                    viewModel.inventory.inventory.filterItemsByType(ItemType.Relic),
                    key = { it.uuid }
                ) { item ->
                    ItemEntryPanel(
                        item = item,
                        isSelected = selectedState.selected?.isSameItem(item) ?: false,
                        onClick = {
                            viewModel.selectItem(
                                item = item.copy(count = 1),
                                altarSlot = selectedState.selectedSlot
                            )
                        }
                    ) {
                        Text(text = "x ${item.count}")
                    }
                }
            }
        }
    }

    if (uiState is AltarScreenUiState.ConfirmRemove) {
        val confirmState = uiState as AltarScreenUiState.ConfirmRemove
        GameDialog(
            title = viewModel.i18n.tr("Remove"),
            onDismissRequest = viewModel::closeDialog,
            onConfirmation = { viewModel.confirmRemove(confirmState, altar) }
        ) {
            Text(viewModel.i18n.tr("Remove the relic?"))
        }
    }
}


@Composable
private fun AltarSlotPanel(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    altarSlot: AltarSlot,
    onClick: () -> Unit
) {
    val hasRelic = altarSlot.item != null
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .drawBehind {
                if (hasRelic) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                tertiaryColor.copy(alpha = 0.18f),
                                tertiaryColor.copy(alpha = 0.05f),
                            )
                        )
                    )
                } else {
                    drawRect(color = surfaceVariantColor)
                }
            }
            .border(
                width = 1.dp,
                color = if (hasRelic) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RectangleShape
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        if (altarSlot.item == null) {
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Feather.Plus,
                        contentDescription = i18n.tr("Place Relic"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = i18n.tr("Place Relic"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameImage(
                        iconName = altarSlot.item!!.template.id,
                        modifier = Modifier.size(44.dp),
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = altarSlot.item!!.displayName,
                            fontWeight = FontWeight.SemiBold,
                        )

                        ItemModifiersPanel(
                            modifiers = altarSlot.item?.allModifiers ?: emptyList()
                        )
                    }
                }
            }
        }
    }
}
