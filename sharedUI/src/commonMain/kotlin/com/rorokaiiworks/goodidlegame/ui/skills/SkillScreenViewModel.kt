package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.combat.CombatSession
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.enemies.Enemy
import com.rorokaiiworks.goodidlegame.core.enemies.EnemyTemplate
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.events.ToastType
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.loadouts.PlayerLoadouts
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.props.PropSlot
import com.rorokaiiworks.goodidlegame.core.props.PropsContainer
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.Skill
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.tasks.*
import com.rorokaiiworks.goodidlegame.ui.Navigator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class SkillScreenViewModel(val skillData: CachedSkillData) : ViewModel(), KoinComponent {
    val taskSystem: TaskSystem by inject()
    val player: Player by inject()
    val playerInventory: PlayerInventory by inject()
    val playerLoadouts: PlayerLoadouts by inject()
    val playerSkills: PlayerSkills by inject()
    val enemyTemplates: DataTable<EnemyTemplate> by inject(named<EnemyTemplate>())
    val itemService: ItemService by inject()
    val eventBus: EventBus by inject()
    val i18n: I18n by inject()
    val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    val navigator: Navigator by inject()
    val timeProvider: ITimeProvider by inject()

    var taskRepeatConfig: TaskRepeatConfig by mutableStateOf(TaskRepeatConfig.Infinite)
    val scrollerState = ScrollState(initial = 0)

    private val _uiState = MutableStateFlow(SkillUiState(
        selectedAction = getInitialAction(),
        selectedCategory = getInitialCategory(),
        selectedSubCategory = skillData.subCategories.firstOrNull()
    ))
    val uiState = _uiState.asStateFlow()

    private fun getInitialCategory(): String {
        return skillData.categories.first()
    }

    private fun getInitialAction(): SkillAction {
        return skillData.actions.first()
    }

    fun onTaskRepeatConfigChange(config: TaskRepeatConfig) {
        taskRepeatConfig = config
    }

    fun onAutoEquipSlotSelected(
        propsContainer: PropsContainer,
        slotId: String,
        itemId: String
    ) {
        propsContainer.propSlotsAutoEquip[slotId] = itemId
        updateState { it.copy(autoEquipSlotId = null) }
    }

    fun onAutoEquipCanceled() {
        updateState { it.copy(autoEquipSlotId = null) }
    }

    fun onAutoEquipSlotClicked(
        slotId: String
    ) {
        updateState { it.copy(autoEquipSlotId = slotId) }
    }

    fun onInfoClicked() {
        updateState { it.copy(showSkillDesc = true) }
    }

    fun onSkillDescClosed() {
        updateState { it.copy(showSkillDesc = false) }
    }

    fun onPropSlotClicked(slot: PropSlot) {
        updateState { it.copy(mode = SkillMode.SelectingProp(slot)) }
    }

    fun onPropPanelCanceled() {
        updateState { it.copy(mode = SkillMode.Viewing) }
    }

    fun onStartTask(skill: Skill, action: SkillAction, repeatCount: Int = Int.MAX_VALUE, destination: AppDestination?) {
        startTask(skill, action, repeatCount, destination)
    }

    fun onStopTask(skillActionId: String) {
        taskSystem.stopTaskBySkillActionId(skillActionId)
    }

    fun onActionSelected(action: SkillAction) {
        updateState { it.copy(selectedAction = action) }
    }

    fun onCraftBtnClicked(productId: String) {
        updateState { it.copy(mode = SkillMode.Crafting(productId)) }
    }

    fun onItemSelectCanceled() {
        updateState { it.copy(mode = SkillMode.SelectingItem(null)) }
    }

    fun onItemSelected(item: Item) {
        updateState { it.copy(mode = SkillMode.SelectingItem(item)) }
    }

    fun onPropAdded(item: Item, propsContainer: PropsContainer) {
        addProp(item, propsContainer)
    }

    fun onSelectingItem() {
        updateState { it.copy(mode = SkillMode.SelectingItem(null)) }
    }

    fun onCraftBtnCanceled() {
        updateState { it.copy(mode = SkillMode.Viewing) }
    }

    fun onCategorySelected(category: String) {
        updateState { it.copy(selectedCategory = category) }
    }

    fun onSubCategorySelected(subCategory: String?) {
        updateState { it.copy(selectedSubCategory = subCategory) }
    }

    private fun addProp(item: Item, propsContainer: PropsContainer) {
        if (uiState.value.mode !is SkillMode.SelectingProp) return
        val selectingProp = uiState.value.mode as SkillMode.SelectingProp

        val prop = itemService.itemToProp(item)
        if (prop == null) {
            viewModelScope.launch {
                eventBus.emit(IEvent.ToastMessage(
                    msg = i18n.tr("Item is not a prop"),
                    isTop = true,
                    toastType = ToastType.Error
                ))
            }
            return
        }

        playerInventory.inventory.removeItem(item.copy(count = 1))
        selectingProp.propSlot.clearItem()
        propsContainer.addItem(prop, selectingProp.propSlot)
        updateState { it.copy(mode = SkillMode.Viewing) }
    }

    private fun updateState(block: (SkillUiState) -> SkillUiState) {
        _uiState.update(block)
    }

    @OptIn(ExperimentalTime::class)
    private fun startTask(
        skill: Skill,
        action: SkillAction,
        repeatCount: Int,
        destination: AppDestination?,
    ) {
        val task = when (action) {
            is SkillAction.CombatSkillAction -> {
                Task.Combat(
                    combatSession = CombatSession(
                        player = player,
                        action = action,
                        skill = skill,
                    ),
                    action = action,
                )
            }
            is SkillAction.NormalSkillAction  -> {
                Task.Train(
                    action = action,
                )
            }
            is SkillAction.ArchaeologySkillAction -> {
                Task.Train(
                    action = action,
                )
            }
        }

        taskSystem.startSession(
            session = TaskSession(
                title = action.name,
                subTitle = destination?.title ?: "",
                repeatCount = repeatCount,
                destination = destination,
                task = task,
                skill = skill,
                isPassive = action is SkillAction.ArchaeologySkillAction,
            ),
            currentMills = timeProvider.nowMillis()
        )
    }
}