@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.ui.community

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.composables.icons.feather.*
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.community.EnchantingSlot
import com.rorokaiiworks.goodidlegame.core.community.EnchantingSlotState
import com.rorokaiiworks.goodidlegame.core.community.EnchantingTable
import com.rorokaiiworks.goodidlegame.core.community.EnchantmentPreview
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.prettyPrint
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType
import com.rorokaiiworks.goodidlegame.ui.commons.ConsumeItemsContent
import com.rorokaiiworks.goodidlegame.ui.commons.GameDialog
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.commons.HighlightTextLabel
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemEntryPanel
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemModifiersPanel
import kotlinx.coroutines.delay
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


sealed interface EnchantingTableScreenUiState {
    object Idle : EnchantingTableScreenUiState

    data class ConfirmStopEnchanting(
        val slot: EnchantingSlot,
        val originalItem: Item,
    ) : EnchantingTableScreenUiState

    data class SelectingItem(
        val selected: Item?,
        val selectedSlot: EnchantingSlot
    ) : EnchantingTableScreenUiState

    data class ChoosingResult(
        val slot: EnchantingSlot,
        val originalItem: Item,
        val processedItem: Item
    ) : EnchantingTableScreenUiState
}


class EnchantingTableScreenViewModel : ViewModel(), KoinComponent {
    val timeProvider: ITimeProvider by inject()
    val playerInventory: PlayerInventory by inject()
    val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    val i18n: I18n by inject()

    private val _uiState = MutableStateFlow<EnchantingTableScreenUiState>(
        value = EnchantingTableScreenUiState.Idle,
    )
    val uiState = _uiState.asStateFlow()

    fun clickSlot(slot: EnchantingSlot) {
        when (val state = slot.state) {
            is EnchantingSlotState.Idle -> {
                if (state.originalItem == null) {
                    _uiState.update {
                        EnchantingTableScreenUiState.SelectingItem(selected = null, selectedSlot = slot)
                    }
                    return
                }

                if (state.processedItem != null) {
                    _uiState.update {
                        EnchantingTableScreenUiState.ChoosingResult(
                            slot = slot,
                            originalItem = state.originalItem,
                            processedItem = state.processedItem,
                        )
                    }
                }
            }
            is EnchantingSlotState.Running -> {
                _uiState.update {
                    EnchantingTableScreenUiState.ConfirmStopEnchanting(
                        slot = slot,
                        originalItem = state.item,
                    )
                }
            }
        }
    }

    fun closeDialog() {
        _uiState.update { EnchantingTableScreenUiState.Idle }
    }

    fun confirmStopEnchanting(state: EnchantingTableScreenUiState.ConfirmStopEnchanting) {
        playerInventory.inventory.addItem(state.originalItem)
        state.slot.clear()
        _uiState.update { EnchantingTableScreenUiState.Idle }
    }

    fun confirmSelectingItemDialog(
        uiState: EnchantingTableScreenUiState.SelectingItem,
        enchantingTable: EnchantingTable
    ) {
        uiState.selected?.let {
            val costs = enchantingTable.calculateEnchantingConsumes(it)
            if (!playerInventory.inventory.canConsume(costs)) return

            playerInventory.inventory.removeItems(costs)
            playerInventory.inventory.removeItem(it.copy(count = 1))

            uiState.selectedSlot.placeItem(it)
        }
        _uiState.update { EnchantingTableScreenUiState.Idle }
    }

    fun selectItem(item: Item, enchantingSlot: EnchantingSlot) {
        _uiState.update {
            EnchantingTableScreenUiState.SelectingItem(
                selected = item,
                selectedSlot = enchantingSlot
            )
        }
    }

    fun confirmResultChoice(state: EnchantingTableScreenUiState.ChoosingResult, kept: Item) {
        playerInventory.inventory.addItem(kept)
        state.slot.clear()
        _uiState.update { EnchantingTableScreenUiState.Idle }
    }
}


@Composable
fun EnchantingTableScreen(
    modifier: Modifier = Modifier,
    viewModel: EnchantingTableScreenViewModel = koinViewModel(),
    enchantingTable: EnchantingTable
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        FlowRow(
            modifier = Modifier.padding(8.dp),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            enchantingTable.slots.forEach { slot ->
                EnchantingSlotPanel(
                    modifier = Modifier.weight(1f),
                    enchantingSlot = slot,
                    now = { viewModel.timeProvider.nowMillis() },
                    onClick = { viewModel.clickSlot(slot) }
                )
            }
        }

        CommunityCostEntry(
            costs = enchantingTable.getPurchaseSlotCosts(),
            itemTemplates = viewModel.itemTemplates,
            playerInventory = viewModel.playerInventory,
            onClick = { enchantingTable.purchaseSlot(viewModel.playerInventory.inventory) }
        )
    }

    when (val state = uiState) {
        is EnchantingTableScreenUiState.ChoosingResult -> {
            ChooseResultDialog(
                state = state,
                onKeep = { kept -> viewModel.confirmResultChoice(state, kept) },
                onDismiss = viewModel::closeDialog,
            )
        }
        is EnchantingTableScreenUiState.ConfirmStopEnchanting -> {
            ConfirmStopEnchantingDialog(
                state = state,
                onConfirm = { viewModel.confirmStopEnchanting(state) },
                onDismiss = viewModel::closeDialog,
            )
        }
        EnchantingTableScreenUiState.Idle -> {}
        is EnchantingTableScreenUiState.SelectingItem -> {
            SelectingDialog(
                uiState = state,
                enchantingTable = enchantingTable,
                viewModel = viewModel
            )
        }
    }
}


@Composable
private fun SelectingDialog(
    uiState: EnchantingTableScreenUiState.SelectingItem,
    enchantingTable: EnchantingTable,
    viewModel: EnchantingTableScreenViewModel
) {
    val costs = uiState.selected?.let { enchantingTable.calculateEnchantingConsumes(it) }
    GameDialog(
        title = viewModel.i18n.tr("Selections"),
        fullScreen = true,

        confirmEnabled = uiState.selected != null && costs?.let {
            viewModel.playerInventory.inventory.canConsume(it)
        } ?: false,

        onDismissRequest = viewModel::closeDialog,
        onConfirmation = { viewModel.confirmSelectingItemDialog(
            uiState = uiState,
            enchantingTable = enchantingTable
        ) }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LazyVerticalStaggeredGrid(
                modifier = Modifier.weight(0.75f),
                columns = StaggeredGridCells.Adaptive(minSize = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
            ) {
                items(
                    viewModel.playerInventory.inventory.filterItemsByType(ItemType.Equipment),
                    key = { it.uuid }
                ) { item ->
                    ItemEntryPanel(
                        item = item,
                        isSelected = uiState.selected?.isSameItem(item) ?: false,
                        onClick = {
                            viewModel.selectItem(
                                item = item.copy(count = 1),
                                enchantingSlot = uiState.selectedSlot
                            )
                        },
                        nameOverride = item.displayName,
                    ) {
                        Text(
                            text = "x ${item.count}",
                        )
                    }
                }
            }

            VerticalDivider()

            Column(
                modifier = Modifier.weight(0.25f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ConsumeItemsContent(
                    consumes = costs ?: emptyList(),
                    itemTemplates = viewModel.itemTemplates,
                    inventory = viewModel.playerInventory.inventory
                )

                uiState.selected?.let { selectedItem ->
                    val previews = enchantingTable.getEnchantmentPreviews(selectedItem.template.type)
                    if (previews.isNotEmpty()) {
                        EnchantmentPreviewPanel(previews = previews)
                    }
                }
            }
        }
    }
}


@Composable
private fun ConfirmStopEnchantingDialog(
    state: EnchantingTableScreenUiState.ConfirmStopEnchanting,
    i18n: I18n = koinInject(),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GameDialog(
        title = i18n.tr("Stop Enchanting"),
        onDismissRequest = onDismiss,
        onConfirmation = onConfirm,
    ) {
        Text(
            text = i18n.tr("Stop enchanting? Materials will not be refunded."),
        )
    }
}


@Composable
private fun ChooseResultDialog(
    state: EnchantingTableScreenUiState.ChoosingResult,
    i18n: I18n = koinInject(),
    onKeep: (Item) -> Unit,
    onDismiss: () -> Unit,
) {
    var chosen by remember { mutableStateOf<Item?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = i18n.tr("Enchanting Result"),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = i18n.tr("Choose the item to put in the inventory, the other one will be discarded."),
                    style = MaterialTheme.typography.bodyMedium,
                )

                ResultItemCard(
                    item = state.originalItem,
                    isSelected = chosen == state.originalItem,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = { chosen = state.originalItem }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Feather.ArrowRight,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                ResultItemCard(
                    item = state.processedItem,
                    isSelected = chosen == state.processedItem,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { chosen = state.processedItem }
                )
            }
        },
        confirmButton = {
            val target = chosen
            Button(
                onClick = { target?.let(onKeep) },
                enabled = target != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            ) {
                Icon(Feather.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(i18n.tr("Put in Inventory"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Icon(
                    Feather.Trash2,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(6.dp))
                Text(i18n.tr("Cancel"), color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
private fun ResultItemCard(
    item: Item,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.25f,
        animationSpec = tween(200),
        label = "borderAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .drawBehind {
                if (isSelected) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.18f),
                                accentColor.copy(alpha = 0.05f),
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    )
                }
            }
            .background(
                color = if (isSelected) Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = accentColor.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        ItemPanel(item = item) {
            if (isSelected) {
                Icon(
                    Feather.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


@Composable
private fun EnchantingSlotPanel(
    modifier: Modifier = Modifier,
    enchantingSlot: EnchantingSlot,
    now: () -> Long,
    onClick: () -> Unit
) {
    val state = enchantingSlot.state
    val isDone = state is EnchantingSlotState.Idle && state.processedItem != null

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    val glowAlpha by rememberInfiniteTransition(label = "doneGlow")
        .animateFloat(
            initialValue = 0.25f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

    Box(
        modifier = modifier
            .drawBehind {
                when {
                    isDone -> drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                tertiaryColor.copy(alpha = 0.22f * glowAlpha),
                                tertiaryColor.copy(alpha = 0.06f),
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    )
                    state is EnchantingSlotState.Running -> drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.14f),
                                primaryColor.copy(alpha = 0.03f),
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height)
                        )
                    )
                    else -> drawRect(color = surfaceVariantColor)
                }
            }
            .border(
                width = if (isDone) 2.dp else 1.dp,
                brush = if (isDone) {
                    Brush.linearGradient(
                        listOf(
                            tertiaryColor.copy(alpha = glowAlpha),
                            tertiaryColor.copy(alpha = 0.3f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            outlineColor.copy(alpha = 0.25f),
                            outlineColor.copy(alpha = 0.25f),
                        )
                    )
                },
                shape = RectangleShape
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        when (state) {
            is EnchantingSlotState.Idle    -> IdleSlotContent(state = state)
            is EnchantingSlotState.Running -> RunningSlotContent(state = state, now = now)
        }
    }
}

@Composable
private fun IdleSlotContent(
    i18n: I18n = koinInject(),
    state: EnchantingSlotState.Idle,
) {
    when {
        state.originalItem == null -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Feather.Plus,
                        contentDescription = i18n.tr("Select Item"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = i18n.tr("Select Item"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }

        state.processedItem != null -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactItemChip(item = state.originalItem, label = i18n.tr("Original"))
                    Icon(
                        Feather.ArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    CompactItemChip(item = state.processedItem, label = i18n.tr("New"), highlight = true)
                }
                Text(
                    text = i18n.tr("Click to Keep"),
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        else -> ItemPanel(item = state.originalItem)
    }
}

@Composable
private fun RunningSlotContent(
    i18n: I18n = koinInject(),
    state: EnchantingSlotState.Running,
    now: () -> Long
) {
    val currentTime by produceState(initialValue = now()) {
        while (true) {
            delay(1000L)
            value = now()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ItemPanel(item = state.item)

        Text(
            text = i18n.tr("Remaining") + " ${Humanizer.duration(state.remains(now = currentTime))}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        GradientProgressBar(progress = state.getProgress(currentTime))
    }
}


@Composable
private fun GradientProgressBar(progress: Float) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "progressAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(listOf(primaryColor, tertiaryColor))
                )
        )
    }
}


@Composable
private fun CompactItemChip(
    item: Item,
    label: String,
    highlight: Boolean = false,
) {
    val bgColor = if (highlight) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (highlight) MaterialTheme.colorScheme.onTertiaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        GameImage(iconName = item.template.id, modifier = Modifier.size(22.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
            Text(
                text = item.displayName,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


@Composable
private fun ItemPanel(
    modifier: Modifier = Modifier,
    item: Item,
    content: @Composable () -> Unit = { }
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameImage(
            iconName = item.template.id,
            modifier = Modifier.size(44.dp),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = item.displayName,
                fontWeight = FontWeight.SemiBold,
            )
            ItemModifiersPanel(modifiers = item.allModifiers)
        }

        content()
    }
}

@Composable
private fun EnchantmentPreviewPanel(
    modifier: Modifier = Modifier,
    previews: List<EnchantmentPreview>,
    i18n: I18n = koinInject()
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = i18n.tr("Possible Modifiers"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (preview in previews) {
                val rangeText = when (preview.type) {
                    StatModifierType.Flat -> "${preview.range.start.prettyPrint()} - ${preview.range.endInclusive.prettyPrint()}"
                    StatModifierType.Percent -> "${(preview.range.start * 100).prettyPrint()}% - ${(preview.range.endInclusive * 100).prettyPrint()}%"
                }

                val probText = "${(preview.probability * 100).prettyPrint()}%"

                HighlightTextLabel(
                    text = "${i18n.trc("stat_id", preview.statId)}: +$rangeText ($probText)",
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            }
        }

        Text(
            text = i18n.tr("Will randomly get 1-2 modifiers."),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}