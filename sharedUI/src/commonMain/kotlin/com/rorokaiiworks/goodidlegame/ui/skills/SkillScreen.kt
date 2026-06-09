@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.rorokaiiworks.goodidlegame.core.props.PropSlot
import com.rorokaiiworks.goodidlegame.core.skills.Skill
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.tasks.Task
import com.rorokaiiworks.goodidlegame.core.tasks.TaskRepeatConfig
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSystem
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.Navigator
import com.rorokaiiworks.goodidlegame.ui.combat.CombatSessionPanel
import com.rorokaiiworks.goodidlegame.ui.combat.EnemyStatsPanel
import com.rorokaiiworks.goodidlegame.ui.commons.*
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemModifiersPanel
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateDetailPanel
import com.rorokaiiworks.goodidlegame.ui.isWideScreen
import com.rorokaiiworks.goodidlegame.ui.recipes.ProductScreen
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun SkillScreen(
    cachedSkillData: CachedSkillData,
    viewModel: SkillScreenViewModel,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
) {
    val skill = cachedSkillData.skill

    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showSkillDesc) {
        SkillDescPanel(
            modifier = Modifier.width(400.dp),
            title = viewModel.i18n.tr(skill.template.name),
            desc = viewModel.i18n.tr(skill.template.desc),
            onClose = { viewModel.onSkillDescClosed() }
        )
        return
    }

    when (uiState.mode) {
        is SkillMode.Crafting -> {
            ProductScreen(
                productItemId = (uiState.mode as SkillMode.Crafting).craftProductId,
                fromInventory = viewModel.playerInventory.inventory,
                toInventory = viewModel.playerInventory.inventory,
                onClose = { viewModel.onCraftBtnCanceled() }
            )
        }
        else -> {
            SkillScreenContent(
                uiState = uiState,
                viewModel = viewModel,
                cachedSkillData = cachedSkillData,
                scrollState = viewModel.scrollerState,
                isWide = isWideScreen(windowSizeClass)
            )
        }
    }
}

@Composable
private fun SpeedupButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    viewModel: SkillScreenViewModel = koinViewModel(),
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = viewModel.playerInventory.stars >= 1 && viewModel.taskSystem.archaeologyIsRunning(),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(viewModel.i18n.tr("Speedup 10 mins: 1 star"))
            GameImage(
                iconName = "star"
            )
            Text("(${viewModel.playerInventory.stars})")
        }
    }
}

@Composable
private fun SkillScreenContent(
    uiState: SkillUiState,
    cachedSkillData: CachedSkillData,
    scrollState: ScrollState,
    viewModel: SkillScreenViewModel,
    isWide: Boolean
) {
    val skill = cachedSkillData.skill

    if (isWide) {
        when (uiState.mode) {
            is SkillMode.SelectingProp -> {
                val propMode = uiState.mode
                AddPropPanel(
                    skill = skill,
                    propSlot = propMode.propSlot,
                    viewModel = viewModel,
                )
            }

            is SkillMode.SelectingItem -> {
                val itemMode = uiState.mode

                SelectItemModePanel(
                    uiState = uiState,
                    itemMode = itemMode,
                    viewModel = viewModel
                )
            }

            else -> {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SkillProgressPanel(
                            skill = skill,
                            onInfoClick = { viewModel.onInfoClicked() }
                        )

                        viewModel.taskSystem.currentSession?.let {
                            if (it.task is Task.Combat) {
                                CombatSessionPanel(
                                    combatSession = it.task.combatSession
                                )
                            }
                        }

                        SkillActionsListPanel(
                            onClick = { viewModel.onActionSelected(it) },
                            uiState = uiState,
                            cachedSkillData = cachedSkillData,
                            viewModel = viewModel
                        )

                        if (viewModel.navigator.currentDestination.route == "skill_archaeology") {
                            SpeedupButton(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (!viewModel.taskSystem.archaeologyIsRunning()) return@SpeedupButton
                                    if (viewModel.playerInventory.stars < 1) return@SpeedupButton

                                    viewModel.taskSystem.getArchaeologySession()?.nextTickAddition += 600f
                                    viewModel.playerInventory.spendStars(1)
                                }
                            )
                        }

                        when (uiState.selectedAction) {
                            is SkillAction.CombatSkillAction -> {
                                val action = uiState.selectedAction
                                val enemies = action.enemyIds.map { viewModel.enemyTemplates.find(it) }
                                
                                BaseCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = viewModel.i18n.tr("Dungeon: {0} - {1} Waves", action.minWaves, action.maxWaves),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        if (action.isLastWaveBoss) {
                                            Text(
                                                text = viewModel.i18n.tr("Last wave is BOSS!"),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        HorizontalDivider()

                                        Text(
                                            text = viewModel.i18n.tr("Monster Pool:"),
                                            style = MaterialTheme.typography.labelMedium
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            enemies.forEach { enemy ->
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    GameImage(
                                                        modifier = Modifier.size(50.dp),
                                                        iconName = enemy.iconName
                                                    )

                                                    Column {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        ) {
                                                            Text(
                                                                text = "Lv.${enemy.level}",
                                                            )

                                                            Text(
                                                                text = viewModel.i18n.tr(enemy.name)
                                                            )
                                                        }

                                                        EnemyStatsPanel(
                                                            enemyTemplate = enemy,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is SkillAction.ArchaeologySkillAction,
                            is SkillAction.NormalSkillAction -> {
                                uiState.selectedAction.getAlwaysDropItemId()?.let { itemId ->
                                    val template = viewModel.itemTemplates.find(itemId)

                                    template.modifiers?.let { modifiers ->
                                        if (modifiers.isNotEmpty()) {
                                            BaseCard(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    GameImage(
                                                        modifier = Modifier.size(48.dp),
                                                        iconName = template.id
                                                    )

                                                    ItemModifiersPanel(
                                                        modifiers = modifiers
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ActionAndPropContent(skill, uiState, viewModel)

                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    TaskStartStopButton(
                        selectedAction = uiState.selectedAction,
                        skill = skill,
                        taskSystem = viewModel.taskSystem,
                        viewModel = viewModel
                    )
                }
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (uiState.mode) {
                is SkillMode.SelectingItem -> {
                    val itemMode = uiState.mode
                    SelectItemModePanel(
                        uiState = uiState,
                        itemMode = itemMode,
                        viewModel = viewModel
                    )
                }

                is SkillMode.SelectingProp -> {
                    val propMode = uiState.mode
                    AddPropPanel(
                        skill = skill,
                        propSlot = propMode.propSlot,
                        viewModel = viewModel,
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier.weight(0.9f).verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SkillProgressPanel(
                            skill = skill,
                            onInfoClick = { viewModel.onInfoClicked() }
                        )
                        SkillActionsListPanel(
                            onClick = { viewModel.onActionSelected(it) },
                            uiState = uiState,
                            cachedSkillData = cachedSkillData,
                            viewModel = viewModel
                        )
                        ActionAndPropContent(skill, uiState, viewModel)
                    }

                    TaskStartStopButton(
                        selectedAction = uiState.selectedAction,
                        skill = skill,
                        taskSystem = viewModel.taskSystem,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    if (uiState.autoEquipSlotId != null) {
        GameDialog(
            title = viewModel.i18n.tr("Auto Equip"),
            onDismissRequest = { viewModel.onAutoEquipCanceled() },
        ) {
            val propsContainer = skill.propsContainer ?: return@GameDialog
            val propSlot = propsContainer.propSlots.first { it.id == uiState.autoEquipSlotId }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val itemTemplate = if (propsContainer.propSlotsAutoEquip[propSlot.id]?.isNotEmpty() == true) {
                    propsContainer.propSlotsAutoEquip[propSlot.id]!!
                } else {
                    null
                }?.let { viewModel.itemTemplates.find(it) }
                
                ItemTemplateDetailPanel(
                    item = itemTemplate
                )
                
                SelectTemplateFromPanel(
                    // TODO: 缓存过滤列表
                    selections = viewModel.itemTemplates.all().filter {
                        it.type in propSlot.acceptType
                    },
                    onSelect = { viewModel.onAutoEquipSlotSelected(
                        slotId = propSlot.id,
                        itemId = it.id,
                        propsContainer = propsContainer,
                    ) }
                )
            }
        }
    }
}


@Composable
private fun TaskStartStopButton(
    modifier: Modifier = Modifier,
    selectedAction: SkillAction,
    skill: Skill,
    navigator: Navigator = koinInject(),
    taskSystem: TaskSystem,
    viewModel: SkillScreenViewModel,
) {
    val isRunning = taskSystem.currentSession?.task?.action?.id == selectedAction.id ||
        taskSystem.passiveSessions.any {
            it.task.action.id == selectedAction.id
        }
    val currentSession = if (taskSystem.currentSession?.task?.action?.id == selectedAction.id) {
        taskSystem.currentSession
    } else {
        taskSystem.passiveSessions.find {
            it.task.action.id == selectedAction.id
        }
    }

    StartStopButton(
        modifier = modifier,
        isRunning = isRunning,
        selectedAction = selectedAction,
        viewModel = viewModel,
        onStartTask = { viewModel.onStartTask(
            skill = skill,
            action = selectedAction,
            repeatCount = when (val config = viewModel.taskRepeatConfig) {
                is TaskRepeatConfig.Custom -> config.count
                TaskRepeatConfig.Once -> 1
                TaskRepeatConfig.Infinite -> Int.MAX_VALUE
            },
            destination = navigator.currentDestination,
        ) },
        taskRepeatConfig = viewModel.taskRepeatConfig,
        onTaskRepeatConfigChange = viewModel::onTaskRepeatConfigChange,
        currentSession = currentSession,
    )
}

@Composable
private fun SelectItemModePanel(
    i18n: I18n = koinInject(),
    uiState: SkillUiState,
    itemMode: SkillMode.SelectingItem,
    viewModel: SkillScreenViewModel
) {
    val filteredItems = remember(uiState.selectedAction, itemMode) {
        when (uiState.selectedAction) {
            is SkillAction.ArchaeologySkillAction -> {
                viewModel
                    .playerInventory
                    .inventory
                    .filterItemsByItemId(uiState.selectedAction.neededMapId)
            }
            else -> emptyList()
        }
    }

    SelectItemPanel(
        selections = filteredItems,
        onSelect = { viewModel.onItemSelected(it) },
        onClose = { viewModel.onItemSelectCanceled() },
        selectedItem = itemMode.selectedItem
    ) {
        SelectFromPanel(
            title = i18n.tr("Equipped"),
            selections = viewModel.playerLoadouts.equippedItems.filter {
                    item -> item != itemMode.selectedItem
            }.toList(),
            onSelect = { viewModel.onItemSelected(it) }
        )
    }
}

@Composable
private fun ActionAndPropContent(
    skill: Skill,
    uiState: SkillUiState,
    viewModel: SkillScreenViewModel
) {
    val isRunning = viewModel.taskSystem.skillActionIsRunning(skillAction = uiState.selectedAction)
    val isWideScreen = isWideScreen()

    FlowRow(
        maxItemsInEachRow = if (isWideScreen) 2 else 1,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        uiState.selectedAction.let { action ->
            action.dropTable?.let { dropTable ->
                if (dropTable.entries.isNotEmpty()) DropTablePanel(
                    modifier = Modifier.weight(1f),
                    dropTable = dropTable
                ) }

            action.consumeItems?.let {
                if (it.isEmpty()) return@let
                ConsumeItemsPanel(
                    modifier = Modifier.weight(1f),
                    inventory = viewModel.playerInventory.inventory,
                    consumeItems = it,
                    onCraftBtnClick = { productId ->
                        viewModel.onCraftBtnClicked(productId = productId)
                    }
                )
            }
        }

        skill.propsContainer?.let { propsContainer ->
             PropsContainerPanel(
                 modifier = Modifier.weight(1f),
                 propsContainer = propsContainer,
                 autoEquip = propsContainer.propsAutoEquipEnabled,
                 onAutoEquipChange = {
                     propsContainer.propsAutoEquipEnabled = it
                 },
                 onClick = {
                     if (!isRunning) viewModel.onPropSlotClicked(it)
                 },
                 onAutoEquipSlotClicked = viewModel::onAutoEquipSlotClicked,
             )
        }

        if (isRunning) {
            val session = viewModel.taskSystem.findSessionBySkillActionId(uiState.selectedAction.id)

            if (viewModel.navigator.currentDestination.route == session?.skill?.template?.id) {
                LootPanel(
                    modifier = Modifier.weight(1f),
                    titleContent = { },
                    items = session.lootResult.items
                )
            }
        }
    }
}

@Composable
private fun AddPropPanel(
    i18n: I18n = koinInject(),
    skill: Skill,
    propSlot: PropSlot,
    viewModel: SkillScreenViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BaseCard {
            CardTitleWithCloseBtn(
                title = i18n.tr("Equipped"),
                onClose = { viewModel.onPropPanelCanceled() }
            )

            propSlot.item?.let { item ->
                GameImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    iconName = item.template.id
                )
            } ?: Text(text = i18n.tr("No Prop Selected"))
        }

        val propsContainer = skill.propsContainer ?: return
        val selections = remember(propSlot) {
            viewModel.playerInventory.inventory.items
                .filter { propSlot.canAddItem(it) }
                .toList()
        }

        SelectFromPanel(
            selections = selections,
            onSelect = { viewModel.onPropAdded(it, propsContainer) }
        )
    }
}