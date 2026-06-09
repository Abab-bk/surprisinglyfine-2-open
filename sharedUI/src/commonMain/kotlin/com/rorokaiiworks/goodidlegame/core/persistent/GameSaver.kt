@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.core.persistent

import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.DLC
import com.rorokaiiworks.goodidlegame.DLCService
import com.rorokaiiworks.goodidlegame.core.GameState
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.TimeSystem
import com.rorokaiiworks.goodidlegame.core.achievements.AchievementSystem
import com.rorokaiiworks.goodidlegame.core.codex.Codex
import com.rorokaiiworks.goodidlegame.core.community.Community
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.extensions.truncateToSeconds
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.journey.JourneySystem
import com.rorokaiiworks.goodidlegame.core.loadouts.PlayerLoadouts
import com.rorokaiiworks.goodidlegame.core.mastery.MasteryLevel
import com.rorokaiiworks.goodidlegame.core.offline.OfflineReward
import com.rorokaiiworks.goodidlegame.core.offline.calculateOfflineReward
import com.rorokaiiworks.goodidlegame.core.offline.tickCityOfflineReward
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.quests.QuestSystem
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.starStore.StarStore
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSystem
import com.rorokaiiworks.goodidlegame.core.traits.TraitSystem
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.City
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

class GameSaver(val fileWriter: FileWriter<GameSave>) : KoinComponent {
    private val player: Player by inject()
    private val playerSkills: PlayerSkills by inject()
    private val playerLoadouts: PlayerLoadouts by inject()
    private val playerInventory: PlayerInventory by inject()
    private val taskSystem: TaskSystem by inject()
    private val timeProvider: ITimeProvider by inject()
    private val starStore: StarStore by inject()
    private val skillActions: DataTable<SkillAction> by inject(named<SkillAction>())
    private val journeySystem: JourneySystem by inject()
    private val questsSystem: QuestSystem by inject()
    private val gameState: GameState by inject()
    private val masteryLevel: MasteryLevel by inject()
    private val codex: Codex by inject()
    private val community: Community by inject()
    private val timeSystem: TimeSystem by inject()
    private val achievementSystem: AchievementSystem by inject()
    private val traitSystem: TraitSystem by inject()
    private val city: City by inject()
    private val dlcSaver: DLCService by inject()
    private val logger: Logger by inject { parametersOf("GameSaver") }


    @OptIn(ExperimentalTime::class)
    suspend fun load(currentMills: Long): OfflineReward? {
        val gameSave = fileWriter.read() ?: return null

        runCatching {
            gameState.doLoad(gameSave, currentMills)
            codex.doLoad(gameSave, currentMills)

            player.name = gameSave.playerName
            playerInventory.doLoad(gameSave, currentMills)

            playerLoadouts.doLoad(gameSave, currentMills)
            playerSkills.doLoad(gameSave, currentMills)

            questsSystem.doLoad(gameSave, currentMills)
            journeySystem.doLoad(gameSave, currentMills)

            starStore.doLoad(gameSave, currentMills)

            masteryLevel.doLoad(gameSave, currentMills)
            community.doLoad(gameSave, currentMills)
            timeSystem.doLoad(gameSave, currentMills)
            achievementSystem.doLoad(gameSave, currentMills)
            traitSystem.doLoad(gameSave, currentMills)
            if (dlcSaver.enabled(DLC.Societal)) {
                city.doLoad(gameSave, currentMills)
            }
        }.onFailure { e ->
            logger.e(e) { "Failed to load game state, continuing with defaults" }
        }

        val now = timeProvider.now()

        if (dlcSaver.enabled(DLC.Societal)) {
            runCatching {
                tickCityOfflineReward(city, 0.0f, gameSave.time, now)
            }.onFailure { e ->
                logger.e(e) { "Failed to tick city offline reward" }
            }
        }

        val (activeSession, passiveSessions) = gameSave.taskSessionSave?.toTaskSession(
                skillActions = skillActions,
                playerSkills = playerSkills,
                player = player,
            ) ?: (null to emptyList())

        return runCatching {
            calculateOfflineReward(
                leftTime = gameSave.time,
                currentTime = now,
                stats = player.stats,
                taskSession = activeSession,
                passiveSession = passiveSessions
            )
        }.onFailure { e ->
            logger.e(e) { "Failed to calculate offline reward" }
        }.getOrNull()
    }

    suspend fun save(): GameSave {
        val gameSave = generateGameSave()
        fileWriter.write(gameSave)
        return gameSave
    }

    @OptIn(ExperimentalTime::class)
    private fun generateGameSave(): GameSave {
        val playerName = player.name
        
        val gameSave = GameSave(
            playerName = playerName,
            time = timeProvider
                .now()
                .truncateToSeconds()
        )

        val currentMills = timeProvider.nowMillis()

        gameState.doSave(gameSave, currentMills)
        playerInventory.doSave(gameSave, currentMills)
        playerLoadouts.doSave(gameSave, currentMills)
        playerSkills.doSave(gameSave, currentMills)
        starStore.doSave(gameSave, currentMills)
        taskSystem.doSave(gameSave, currentMills)
        questsSystem.doSave(gameSave, currentMills)
        journeySystem.doSave(gameSave, currentMills)
        masteryLevel.doSave(gameSave, currentMills)
        codex.doSave(gameSave, currentMills)
        community.doSave(gameSave, currentMills)
        timeSystem.doSave(gameSave, currentMills)
        achievementSystem.doSave(gameSave, currentMills)
        traitSystem.doSave(gameSave, currentMills)
        if (dlcSaver.enabled(DLC.Societal)) city.doSave(gameSave, currentMills)

        return gameSave
    }
}
