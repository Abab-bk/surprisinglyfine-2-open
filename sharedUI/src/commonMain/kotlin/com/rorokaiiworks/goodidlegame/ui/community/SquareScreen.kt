package com.rorokaiiworks.goodidlegame.ui.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.composables.icons.feather.ArrowDown
import com.composables.icons.feather.ArrowUp
import com.composables.icons.feather.Feather
import com.rorokaiiworks.goodidlegame.core.community.Square
import com.rorokaiiworks.goodidlegame.core.community.SquareBuilding
import com.rorokaiiworks.goodidlegame.core.community.SquareBuildingTier
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.ui.commons.ConsumeItemEntry
import com.rorokaiiworks.goodidlegame.ui.commons.ConsumeItemsContent
import com.rorokaiiworks.goodidlegame.ui.commons.GameDialog
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemModifiersPanel
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named


sealed interface SquareScreenUiState {
    object Idle : SquareScreenUiState
    object UplevelSquareConfirm : SquareScreenUiState
}


class SquareScreenViewModel : ViewModel(), KoinComponent {
    val playerInventory: PlayerInventory by inject()
    val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    val i18n: I18n by inject()

    private val _uiState = MutableStateFlow<SquareScreenUiState>(SquareScreenUiState.Idle)
    val uiState = _uiState.asStateFlow()

    val inventory: PlayerInventory get() = playerInventory

    fun clickUnlockNextTier(building: SquareBuilding, square: Square) {
        if (!square.canUnlockNextTier(building)) return

        square.unlockNextTier(
            building = building,
            selectedModifierIndex = 0,
            inventory = playerInventory.inventory
        )
    }

    fun closeDialog() {
        _uiState.update { SquareScreenUiState.Idle }
    }

    fun activateOneTier(building: SquareBuilding, square: Square) {
        square.setActiveTierCount(building, building.activeTierCount + 1)
    }

    fun deactivateOneTier(building: SquareBuilding, square: Square) {
        square.setActiveTierCount(building, building.activeTierCount - 1)
    }

    fun setActiveTierCount(building: SquareBuilding, count: Int, square: Square) {
        square.setActiveTierCount(building, count)
    }

    fun selectModifier(building: SquareBuilding, tierIndex: Int, modifierIndex: Int, square: Square) {
        square.selectModifier(building, tierIndex, modifierIndex)
    }

    fun confirmCapacityUpgrade(square: Square): Boolean {
        return square.purchaseCapacityUpgrade(playerInventory.inventory)
    }

    fun showUplevelSquareConfirmDialog() {
        _uiState.update { SquareScreenUiState.UplevelSquareConfirm }
    }
}


@Composable
fun SquareScreen(
    modifier: Modifier = Modifier,
    square: Square,
    viewModel: SquareScreenViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            SquareHeader(
                square = square,
                onUpgradeCapacity = viewModel::showUplevelSquareConfirmDialog
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = 500.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                items(square.buildings, key = { it.template.id }) { building ->
                    SquareBuildingCard(
                        building = building,
                        square = square,
                        viewModel = viewModel
                    )
                }
            }
        }

        when (uiState) {
            SquareScreenUiState.Idle -> {}
            is SquareScreenUiState.UplevelSquareConfirm -> {
                UplevelSquareConfirmDialog(
                    itemTemplates = viewModel.itemTemplates,
                    playerInventory = viewModel.playerInventory,
                    square = square,
                    onConfirm = { if (viewModel.confirmCapacityUpgrade(square)) viewModel.closeDialog() },
                    onDismiss = { viewModel.closeDialog() }
                )
            }
        }
    }
}


@Composable
private fun SquareHeader(
    i18n: I18n = koinInject(),
    square: Square,
    onUpgradeCapacity: () -> Unit
) {
    val used = square.usedCapacity
    val max = square.maxCapacity
    val fillFraction = if (max > 0) used.toFloat() / max.toFloat() else 0f

    Surface(
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = i18n.tr("Capacity") + ": $used / $max",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (square.canUpgradeCapacity()) {
                    Button(
                        onClick = onUpgradeCapacity,
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            text = i18n.tr("Upgrade"),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = fillFraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (fillFraction > 0.9f) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}


@Composable
private fun SquareBuildingCard(
    i18n: I18n = koinInject(),
    building: SquareBuilding,
    square: Square,
    viewModel: SquareScreenViewModel
) {
    Surface(
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (!building.isBuilt) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = i18n.tr(building.template.name),
                        fontWeight = FontWeight.Bold
                    )
                    if (building.isBuilt) {
                        Text(
                            text = i18n.tr("Occupying") + " ${building.currentSize} / " +
                                    i18n.tr("Unlocked") + " ${building.unlockedTierCount} " +
                                    i18n.tr("Layers"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = i18n.tr("Not Built"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                if (building.isBuilt) {
                    ActivationStepper(
                        activeTierCount = building.activeTierCount,
                        unlockedTierCount = building.unlockedTierCount,
                        onIncrease = {
                            viewModel.activateOneTier(building, square)
                        },
                        onDecrease = {
                            viewModel.deactivateOneTier(building, square)
                        }
                    )
                }
            }

            if (building.isBuilt) {
                Spacer(modifier = Modifier.height(8.dp))
                building.template.tiers.take(building.unlockedTierCount).forEachIndexed { tierIndex, tier ->
                    val isActive = tierIndex < building.activeTierCount
                    val selectedModifier = building.selectedModifiersPerTier.getOrElse(tierIndex) { 0 }
                    TierRow(
                        tierIndex = tierIndex,
                        tier = tier,
                        isActive = isActive,
                        selectedModifiersIndex = selectedModifier,
                        onSelectModifier = { modIdx ->
                            viewModel.selectModifier(building, tierIndex, modIdx, square)
                        },
                        onToggleActive = {
                            val newCount = if (isActive && tierIndex == building.activeTierCount - 1) {
                                tierIndex
                            } else if (!isActive && tierIndex == building.activeTierCount) {
                                tierIndex + 1
                            } else {
                                if (isActive) tierIndex else tierIndex + 1
                            }
                            viewModel.setActiveTierCount(building, newCount, square)
                        }
                    )
                    if (tierIndex < building.unlockedTierCount - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            val canUnlock = square.canUnlockNextTier(building)
            val nextTier = building.nextTierToUnlock
            if (nextTier != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ConsumeItemsContent(
                        consumes = nextTier.cost,
                        itemTemplates = viewModel.itemTemplates,
                        inventory = viewModel.playerInventory.inventory
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.clickUnlockNextTier(building, square) },
                        enabled = canUnlock && viewModel.playerInventory.inventory.canConsume(nextTier.cost),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = i18n.tr("Upgrade"),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else if (building.isBuilt) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = i18n.tr("Max Level Reached"),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}


@Composable
private fun ActivationStepper(
    i18n: I18n = koinInject(),
    activeTierCount: Int,
    unlockedTierCount: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SmallIconButton(
            onClick = onDecrease,
            enabled = activeTierCount > 0
        ) {
            Icon(
                imageVector = Feather.ArrowDown,
                contentDescription = i18n.tr("Decrease Tier"),
                modifier = Modifier.size(18.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until unlockedTierCount) {
                Box(
                    modifier = Modifier
                        .size(if (i < activeTierCount) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < activeTierCount) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                )
            }
        }

        SmallIconButton(
            onClick = onIncrease,
            enabled = activeTierCount < unlockedTierCount
        ) {
            Icon(
                imageVector = Feather.ArrowUp,
                contentDescription = i18n.tr("Increase Tier"),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SmallIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.38f),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}


@Composable
private fun TierRow(
    tierIndex: Int,
    tier: SquareBuildingTier,
    i18n: I18n = koinInject(),
    isActive: Boolean,
    selectedModifiersIndex: Int,
    onSelectModifier: (Int) -> Unit,
    onToggleActive: () -> Unit
) {
    val bgColor = if (isActive)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Surface(
        shape = RectangleShape,
        color = bgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleActive)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                    Text(
                        text = i18n.tr("Tier") + " ${tierIndex + 1}",
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (isActive) {
                        Text(
                            text = i18n.tr("Active"),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Text(
                    text = i18n.tr("Size") + " ${tier.size}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isActive) 0.7f else 0.4f)
                )
            }

            if (tier.modifiers.size > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tier.modifiers.forEachIndexed { idx, modifiers ->
                        val isSelected = idx == selectedModifiersIndex
                        Surface(
                            shape = RectangleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    enabled = isActive,
                                    onClick = { onSelectModifier(idx) }
                                )
                                .alpha(if (isActive) 1f else 0.5f)
                        ) {
                            ItemModifiersPanel(
                                modifier = Modifier.padding(12.dp),
                                modifiers = modifiers
                            )
                        }
                    }
                }
            } else if (tier.modifiers.size == 1) {
                // Single modifier - show inline
                Spacer(modifier = Modifier.height(4.dp))
                ItemModifiersPanel(
                    modifiers = tier.modifiers[0],
                    modifier = Modifier.alpha(if (isActive) 1f else 0.5f)
                )
            }
        }
    }
}


@Composable
private fun UplevelSquareConfirmDialog(
    itemTemplates: DataTable<ItemTemplate>,
    playerInventory: PlayerInventory,
    square: Square,
    i18n: I18n = koinInject(),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GameDialog(
        title = i18n.tr("Upgrade"),
        onDismissRequest = onDismiss,
        onConfirmation = onConfirm,
    ) {
        Text(i18n.tr("Consumes"))

        square.getCapacityUpgradeCost().forEach {
            ConsumeItemEntry(
                consume = it,
                itemTemplates = itemTemplates,
                inventory = playerInventory.inventory,
            )
        }
    }
}