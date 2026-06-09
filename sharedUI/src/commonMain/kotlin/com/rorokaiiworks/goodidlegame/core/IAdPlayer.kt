@file:OptIn(ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.core

import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import com.rorokaiiworks.goodidlegame.core.rewards.Reward
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.random.Random
import kotlin.time.ExperimentalTime

abstract class IAdPlayer : IPersistable, KoinComponent {
    private val logger: Logger by inject { parametersOf("AdPlayer") }
    private val eventBus: EventBus by inject()
    private val playerSkills: PlayerSkills by inject()
    private val taskSystem: TaskSystem by inject()
    private var showedAdCount: Int = 0

    protected abstract suspend fun onPlayAd(): Resource<Unit>

    init {
        CoroutineScope(Dispatchers.Default).launch {
            eventBus.events.collect {
                if (it is IEvent.NewDayEvent) { reset() }
            }
        }
    }

    private fun reset() {
        showedAdCount = 0
        logger.i("AdPlayer reset")
    }

    suspend fun playAd(): Resource<Unit> {
        if (showedAdCount >= MAX_AD_COUNT_EVERYDAY) {
            logger.i { "Reach MAX_AD_COUNT: $MAX_AD_COUNT_EVERYDAY" }
            return Resource.Error(
                code = 1002,
                message = "Reach MAX_AD_COUNT_EVERYDAY"
            )
        }

        when (val result = onPlayAd()) {
            is Resource.Error -> return result
            is Resource.Success<*> -> {
                showedAdCount++

                logger.i { "Ad played successfully" }

                return Resource.Success(Unit)
            }
        }
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.adPlayerSave = AdPlayerSave(showedAdCount = showedAdCount)
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        showedAdCount = gameSave.adPlayerSave.showedAdCount
    }

    fun reachMaxAdCount(): Boolean = showedAdCount >= MAX_AD_COUNT_EVERYDAY

    fun getRandomAdOpportunityRewards(): List<Reward> {
        TODO()
//        val coinsReward = Reward.ItemReward(
//            itemId = "coins",
//            count = GameFormulas.calculateItemPrice(
//                tier = playerSkills.skills.values.random().getTier(),
//                itemType = ItemType.Material
//            ) * Random.nextInt(5, 10)
//        )
//
//        val skillId = if (Random.nextFloat() > 0.5) { playerSkills.skills.keys.random() } else {
//            taskSystem.getCurrentRunningSkillId() ?: playerSkills.skills.keys.random()
//        }
//        val xpReward = Reward.XpReward(
//            skillId = skillId,
//            count = GameFormulas
//                .calculateSkillActionGetXpByTier(
//                    playerSkills.skills[skillId]!!.getTier()
//                ) * Random.nextInt(5, 10)
//        )
//
//        return listOf(
//            coinsReward,
//            xpReward
//        )
    }

    companion object {
        const val MAX_AD_COUNT_EVERYDAY = 10
    }
}


@Serializable
data class AdPlayerSave(
    val showedAdCount: Int,
)