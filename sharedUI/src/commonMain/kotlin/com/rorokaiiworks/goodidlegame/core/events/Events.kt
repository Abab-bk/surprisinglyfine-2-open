package com.rorokaiiworks.goodidlegame.core.events

import com.rorokaiiworks.goodidlegame.core.persistent.SaveSlot
import com.rorokaiiworks.goodidlegame.core.quests.Quest
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.TradeMode
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface IEvent {
    data object RequestReviewDialog : IEvent

    data class StartGame(val slot: SaveSlot) : IEvent
    data class EnemyKilled(val enemyId: String) : IEvent
    data class ItemCollected(val itemId: String, val count: Long) : IEvent
    data class SkillLevelUp(val skillId: String) : IEvent
    data class FinishSkillAction(val skillId: String, val actionId: String) : IEvent
    data class QuestCompleted(val quest: Quest) : IEvent

    data class ItemMastered(val itemTemplate: ItemTemplate) : IEvent
    data class MasteryLevelUp(val level: Int) : IEvent

    data class NewDayEvent(val date: LocalDate) : IEvent

    data object StarDropped : IEvent

    data object SteamOverlayOpened : IEvent

    data class ToastMessage @OptIn(ExperimentalUuidApi::class) constructor(
        // should be translated
        val msg: String,
        val iconId: String? = null,
        val isTop: Boolean = false,
        val uuid: Uuid = Uuid.random(),
        val toastType: ToastType = ToastType.Info
    ) : IEvent

    data class TraitTriggered(
        val name: String,
    ) : IEvent

    data class BuildingBuilt(
        val buildingId: String,
        val count: Int,
    ) : IEvent

    data class CityItemTradeModeChanged(
        val itemId: String,
        val tradeMode: TradeMode,
    ) : IEvent

    data class TutorialFinished(val tutorialId: String) : IEvent
}

enum class ToastType {
    Info,
    Error,
}
