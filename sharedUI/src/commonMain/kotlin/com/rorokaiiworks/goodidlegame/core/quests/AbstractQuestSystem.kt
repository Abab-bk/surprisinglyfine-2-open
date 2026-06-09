package com.rorokaiiworks.goodidlegame.core.quests

import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.core.GameState
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import com.rorokaiiworks.goodidlegame.core.requirements.handleEvent
import com.rorokaiiworks.goodidlegame.ui.NotificationType
import com.rorokaiiworks.goodidlegame.ui.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi

abstract class AbstractQuestSystem : KoinComponent, IPersistable {
    protected val gameState: GameState by inject()
    protected val notifier: Notifier by inject()
    protected val eventBus: EventBus by inject()
    protected val i18n: I18n by inject()

    protected val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    protected abstract val allQuests: Sequence<Quest>

    protected abstract val appDestination: AppDestination
    protected abstract val notificationType: NotificationType

    protected open fun onQuestClaimed(quest: Quest) {}

    init {
        coroutineScope.launch {
            eventBus.events.collect { event ->
                allQuests
                    .filter { it.status == QuestStatus.InProgress && it.requirements.all { r -> r.isMet() } }
                    .forEach { quest -> quest.conditions.handleEvent(event) }

                onEvent(event)

                checkQuests()
            }
        }
    }

    protected abstract fun onEvent(event: IEvent)

    @OptIn(ExperimentalUuidApi::class)
    protected fun checkQuests() {
        allQuests
            .filter { quest ->
                quest.status == QuestStatus.InProgress && quest.conditions.all { it.isMet() }
            }
            .forEach { quest ->
                quest.changeStatus(QuestStatus.Completed)

                eventBus.tryEmit(IEvent.QuestCompleted(quest))
                emitCompletedToast(quest)
            }

        checkNotification()
    }

    abstract fun emitCompletedToast(quest: Quest)

    protected fun shouldNotice(): Boolean =
        allQuests.any { it.status == QuestStatus.Completed }

    fun checkNotification() {
        when {
            shouldNotice() -> {
                notifier.updateBadge(
                    route = appDestination.route,
                    type = notificationType
                )
            }
            else -> {
                notifier.updateBadge(
                    route = appDestination.route,
                    type = null
                )
            }
        }
    }

    fun claimQuest(quest: Quest) {
        if (quest.status != QuestStatus.Completed) return

        quest.rewards.forEach { it.grant() }
        quest.changeStatus(QuestStatus.RewardClaimed)
        gameState.finishedQuests.add(quest.id)

        onQuestClaimed(quest)
        checkNotification()
    }
}