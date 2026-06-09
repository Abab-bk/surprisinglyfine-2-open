package com.rorokaiiworks.goodidlegame.dlcSocietal.ui.policy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.compose.dnd.DragAndDropContainer
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.drag.DraggableItem
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.mohamedrejeb.compose.dnd.rememberDragAndDropState
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicyApplyResult
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicyCard
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicySlot
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicySlotType
import com.rorokaiiworks.goodidlegame.tr
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun PolicyLoadoutTab(
    viewModel: PolicyScreenViewModel,
    isDark: Boolean,
    wideScreen: Boolean,
) {
    val dragState = rememberDragAndDropState<String>()
    val slots = viewModel.policySystem.slots
    val cards = viewModel.unlockedCards()

    DragAndDropContainer(
        state = dragState,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(
                viewModel = viewModel,
                isDark = isDark,
                compactMode = !wideScreen,
                unlockedCardCount = cards.size,
            )

            if (wideScreen) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SlotGrid(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight(),
                        slots = slots,
                        dragState = dragState,
                        viewModel = viewModel,
                        isDark = isDark,
                    )

                    CardsPanel(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        cards = cards,
                        slots = slots,
                        dragState = dragState,
                        viewModel = viewModel,
                        isDark = isDark,
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SlotGrid(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f),
                        slots = slots,
                        dragState = dragState,
                        viewModel = viewModel,
                        isDark = isDark,
                    )

                    CardsPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.55f),
                        cards = cards,
                        slots = slots,
                        dragState = dragState,
                        viewModel = viewModel,
                        isDark = isDark,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    viewModel: PolicyScreenViewModel,
    isDark: Boolean,
    compactMode: Boolean,
    unlockedCardCount: Int,
    i18n: I18n = koinInject()
) {
    val cs = MaterialTheme.colorScheme
    val pending = viewModel.hasPendingChanges()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDark) cs.surfaceContainer else cs.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = i18n.tr("Drag cards to slots, then press Confirm to apply. Only unlocked cards can be drafted."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PolicySlotType.entries.forEach { type ->
                        TypeChip(type = type, palette = typePalette(type = type, isDark = isDark))
                    }

                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = i18n.tr("Fee: {0}", Humanizer.abbreviation(viewModel.policySystem.getPolicyChangeUnlockCost())),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                    )

                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = i18n.tr("Unlocked: {0}", unlockedCardCount),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                    )
                }

                if (compactMode) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.resetDraft() },
                            enabled = pending,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(i18n.tr("Reset"))
                        }

                        Button(
                            onClick = { viewModel.confirmDraft() },
                            enabled = pending,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(i18n.tr("Confirm"))
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.resetDraft() }, enabled = pending) {
                            Text(i18n.tr("Reset"))
                        }

                        Button(onClick = { viewModel.confirmDraft() }, enabled = pending) {
                            Text(i18n.tr("Confirm"))
                        }
                    }
                }
            }
        }

        viewModel.lastApplyResult?.let { result ->
            Text(
                text = viewModel.i18n.tr(result.label),
                color = if (result == PolicyApplyResult.Success || result == PolicyApplyResult.NoChanges) cs.primary else cs.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SlotGrid(
    modifier: Modifier,
    slots: List<PolicySlot>,
    dragState: DragAndDropState<String>,
    viewModel: PolicyScreenViewModel,
    isDark: Boolean,
    i18n: I18n = koinInject()
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
        ) {
            Text(
                text = i18n.tr("Slots"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CARD_SPACING),
                verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
                maxItemsInEachRow = 2,
                modifier = Modifier.fillMaxWidth(),
            ) {
                slots.forEach { slot ->
                    val palette = typePalette(slot.type, isDark = isDark)
                    val draftCard = viewModel.draftCard(slot.id)

                    PolicyCardSurface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 200.dp)
                            .dropTarget(
                                key = slot.id,
                                state = dragState,
                                onDrop = { state ->
                                    viewModel.assignDraft(slot.id, state.data)
                                },
                            ),
                        palette = palette,
                        isDark = isDark,
                        padding = 10.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = slot.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            TypeChip(slot.type, palette)
                        }

                        if (draftCard == null) {
                            Text(
                                text = i18n.tr("Empty"),
                            )
                        } else {
                            Text(
                                text = draftCard.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            Text(
                                text = draftCard.displayText(i18n),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )

                            OutlinedButton(
                                onClick = { viewModel.unequipDraft(slot.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(i18n.tr("Unequip"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardsPanel(
    modifier: Modifier,
    cards: List<PolicyCard>,
    slots: List<PolicySlot>,
    dragState: DragAndDropState<String>,
    viewModel: PolicyScreenViewModel,
    isDark: Boolean,
    i18n: I18n = koinInject()
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = i18n.tr("Cards"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            PolicySlotType.entries.forEach { type ->
                val groupCards = cards.filter { it.slotType == type }
                if (groupCards.isEmpty()) return@forEach

                val palette = typePalette(type, isDark = isDark)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(palette.accent, RoundedCornerShape(CARD_RADIUS))
                    )

                    Text(
                        text = i18n.tr("{0} Policies", type.name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(CARD_SPACING),
                    verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
                    maxItemsInEachRow = 3,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    groupCards.forEach { card ->
                        val equippedSlot = viewModel.equippedSlotForDraft(card.id)
                        val validTargets = slots.mapNotNull { slot ->
                            if (viewModel.slotAcceptsCard(slot, card)) slot.id else null
                        }

                        DraggableItem(
                            state = dragState,
                            key = card.id,
                            data = card.id,
                            dropTargets = validTargets,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 200.dp),
                        ) {
                            PolicyCardSurface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = if (isDragging) 0.28f else 1f },
                                palette = palette,
                                isDark = isDark,
                                padding = 10.dp,
                            ) {
                                Text(
                                    text = card.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                Text(
                                    modifier = Modifier.fillMaxHeight(),
                                    text = card.displayText(i18n)
                                )

                                Text(
                                    text = equippedSlot?.let { i18n.tr("Equipped: {0}", it.title) } ?: i18n.tr("Not equipped"),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                Button(
                                    onClick = {
                                        val slotId = validTargets.firstOrNull() ?: return@Button
                                        viewModel.assignDraft(slotId, card.id)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(i18n.tr("Equip"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeChip(
    type: PolicySlotType,
    palette: TypePalette,
    i18n: I18n = koinInject()
) {
    Box(
        modifier = Modifier
            .background(palette.accent, RoundedCornerShape(CARD_RADIUS))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = i18n.tr(type.label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = palette.onAccent,
        )
    }
}

