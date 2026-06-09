package com.rorokaiiworks.goodidlegame.core.achievements

import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import com.rorokaiiworks.goodidlegame.core.requirements.handleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

class AchievementSystem(val adapter: IAchievementAdapter?) : KoinComponent, IPersistable {
    private val logger: Logger by inject { parametersOf("AchievementSystem") }

    val finishedAchievements = mutableListOf<String>() // store id

    private val scope = CoroutineScope(Dispatchers.Default)
    private val eventBus: EventBus by inject()
    private val achievements: DataTable<Achievement> by inject(named<Achievement>())
    val allAchievements = achievements.all()

    fun clearAllAchievements() {
        allAchievements.forEach {
            clearAchievement(it.id)
        }
    }

    fun clearAchievement(achievementId: String) {
        finishedAchievements.remove(achievementId)
        adapter?.clearAchievement(achievementId)
        logger.i { "achievement $achievementId is cleared" }
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.achievementSystemSaveData = AchievementSystemSaveData(
            finishedAchievements,
            achievements = allAchievements.associateBy(
                { it.id },
                { it.conditions.map { condition -> condition.currentCount } }
            )
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        gameSave.achievementSystemSaveData?.let { saveData ->
            finishedAchievements.addAll(saveData.finishedAchievements)
            saveData.achievements.forEach { (id, counts) ->
                val achievement = allAchievements.find { it.id == id } ?: return@forEach
                achievement.conditions.forEachIndexed { index, condition ->
                    condition.currentCount = counts.getOrElse(index) { 0L }
                }
                if (achievement.id !in finishedAchievements && achievement.conditions.all { it.isMet() }) {
                    finishedAchievements.add(achievement.id)
                    adapter?.setAchievement(achievement.id)
                }
            }
        }
    }

    init {
        scope.launch {
            eventBus.events.collect { event ->
                allAchievements.forEach {
                    if (it.id in finishedAchievements) {
                        adapter?.setAchievement(it.id)
                        return@forEach
                    }

                    it.conditions.handleEvent(event)

                    if (it.conditions.all { condition -> condition.isMet() }) {
                        logger.i { "achievement ${it.name} is met" }
                        finishedAchievements.add(it.id)
                        adapter?.setAchievement(it.id)
                    }
                }
            }
        }
    }
}

@Serializable
data class AchievementSystemSaveData(
    val finishedAchievements: List<String>,
    val achievements: Map<String, List<Long>> = mapOf()
)