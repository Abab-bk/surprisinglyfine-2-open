@file:OptIn(ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.core.quests

import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.NotificationType
import com.rorokaiiworks.goodidlegame.ui.quests.QuestCategory
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

class QuestSystem : AbstractQuestSystem() {
    private val logger: Logger by inject { parametersOf("QuestSystem") }

    val dailyQuests = mutableListOf<Quest>()

    private var dailyQuestSeed: Long = 0L
    private var isLoaded = false

    override val allQuests: Sequence<Quest>
        get() = sequence { yieldAll(dailyQuests) }

    override val appDestination: AppDestination
        get() = AppDestination.QuestDestination

    override val notificationType: NotificationType
        get() = NotificationType.QuestCompleted(QuestCategory.Daily)

    fun start() {
        if (isLoaded) return
        regenerate()
        logger.i { "QuestSystem already started" }
    }

    override fun onEvent(event: IEvent) {
        if (event !is IEvent.NewDayEvent) return
        regenerate()
    }

    private fun generateDailyQuests(seed: Long) {
        val generator = QuestGenerator(random = Random(seed))
        dailyQuests.clear()
        dailyQuests.addAll(generator.generateDailyQuests())
    }

    fun regenerate() {
        dailyQuestSeed = Random.nextLong()
        generateDailyQuests(dailyQuestSeed)
        checkQuests()
        logger.i { "QuestSystem regeneration complete" }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun emitCompletedToast(quest: Quest) {
        eventBus.tryEmit(IEvent.ToastMessage(
            isTop = true,
            msg = i18n.tr("Quest {0} completed", quest.tryGetName()),
        ))
    }


    override fun doSave(gameSave: GameSave, currentMills: Long) {
        val dailyProgress = dailyQuests.map { quest -> quest.toSaveData() }

        gameSave.questsSystemSaveData = QuestsSystemSaveData(
            dailyQuestSeed = dailyQuestSeed,
            dailyQuestsProgress = dailyProgress,
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        val saveData = gameSave.questsSystemSaveData

        dailyQuestSeed = saveData.dailyQuestSeed
        generateDailyQuests(dailyQuestSeed)

        saveData.dailyQuestsProgress.forEachIndexed { index, progress ->
            dailyQuests.getOrNull(index)?.loadProgress(progress)
        }

        isLoaded = true
        checkQuests()
    }
}
