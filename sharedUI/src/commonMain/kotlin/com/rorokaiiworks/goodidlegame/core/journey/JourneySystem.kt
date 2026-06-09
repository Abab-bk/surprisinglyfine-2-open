package com.rorokaiiworks.goodidlegame.core.journey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.quests.AbstractQuestSystem
import com.rorokaiiworks.goodidlegame.core.quests.Quest
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.NotificationType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class JourneySystem : AbstractQuestSystem(), KoinComponent {
    private val journalQuests: DataTable<Quest> by inject(named<Quest>())

    var currentQuest: Quest? by mutableStateOf(null)
        private set

    init {
        updateQuest()
    }

    override val allQuests: Sequence<Quest>
        get() = sequence { currentQuest?.let { yield(it) } }
    override val appDestination: AppDestination
        get() = AppDestination.JourneyDestination
    override val notificationType: NotificationType
        get() = NotificationType.JourneyCompleted


    override fun emitCompletedToast(quest: Quest) {
        eventBus.tryEmit(
            IEvent.ToastMessage(
                isTop = true,
                msg = i18n
                    .tr("{0} Completed",
                        quest.tryGetName()
                    ),
            )
        )
    }

    override fun onQuestClaimed(quest: Quest) {
        super.onQuestClaimed(quest)
        updateQuest()
        checkQuests()
    }

    override fun onEvent(event: IEvent) {

    }

    fun claimQuest() {
        currentQuest?.let { super.claimQuest(it) }
    }

    private fun updateQuest() {
        currentQuest = journalQuests
            .all()
            .firstOrNull { quest -> !gameState.finishedQuests.contains(quest.id) }
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.journeySaveData = JourneySaveData(
            currentQuestId = currentQuest?.id,
            progress = currentQuest?.toSaveData()
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        val journeySaveData = gameSave.journeySaveData ?: return

        if (journeySaveData.currentQuestId == null) {
            updateQuest()
            return
        }

        currentQuest = journalQuests.find(journeySaveData.currentQuestId)

        journeySaveData.progress?.let {
            currentQuest?.loadProgress(it)
        }
        checkQuests()
    }
}