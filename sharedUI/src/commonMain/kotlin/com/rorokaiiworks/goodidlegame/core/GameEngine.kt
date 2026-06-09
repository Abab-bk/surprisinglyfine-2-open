package com.rorokaiiworks.goodidlegame.core

import com.rorokaiiworks.goodidlegame.DLC
import com.rorokaiiworks.goodidlegame.DLCService
import com.rorokaiiworks.goodidlegame.core.community.Community
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.quests.QuestSystem
import com.rorokaiiworks.goodidlegame.core.starStore.StarStore
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSystem
import com.rorokaiiworks.goodidlegame.core.traits.TraitSystem
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.City
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GameEngine : IGameEngine, KoinComponent {
    private val timeSystem: TimeSystem by inject()
    private val taskSystem: TaskSystem by inject()
    private val questSystem: QuestSystem by inject()
    private val player: Player by inject()
    private val starStore: StarStore by inject()
    private val traitSystem: TraitSystem by inject()
    private val city: City by inject()
    private val dlcService: DLCService by inject()
    private val community: Community by inject()

    var gameSpeed: Float = 1.0f

    override fun start() {
        questSystem.start()
    }

    override fun stop() {
    }

    override fun tick1(delta: Float, timeProvider: ITimeProvider) {
        val adjustedDelta = delta * gameSpeed
        val nowMillis = timeProvider.nowMillis()

        taskSystem.tick(nowMillis)
        player.effectManager.tick(adjustedDelta)
        starStore.tick(adjustedDelta)
        traitSystem.tick(adjustedDelta)
        timeSystem.tick()
        community.tick(
            nowMillis
        )

//        saveSystem.tick(delta) // notice: saveSystem should use raw delta
    }

    override fun tick2(delta: Float, timeProvider: ITimeProvider) {
        if (dlcService.enabled(DLC.Societal)) {
            city.tick(delta, currentMills = timeProvider.nowMillis())
        }
    }
}
