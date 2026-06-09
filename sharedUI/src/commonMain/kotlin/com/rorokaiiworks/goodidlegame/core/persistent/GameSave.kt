package com.rorokaiiworks.goodidlegame.core.persistent

import com.rorokaiiworks.goodidlegame.core.AdPlayerSave
import com.rorokaiiworks.goodidlegame.core.GameStateSaveData
import com.rorokaiiworks.goodidlegame.core.mastery.MasteryLevelSaveData
import com.rorokaiiworks.goodidlegame.core.TimeSystemSave
import com.rorokaiiworks.goodidlegame.core.achievements.AchievementSystemSaveData
import com.rorokaiiworks.goodidlegame.core.codex.CodexSaveData
import com.rorokaiiworks.goodidlegame.core.community.CommunitySaveData
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventorySaveData
import com.rorokaiiworks.goodidlegame.core.journey.JourneySaveData
import com.rorokaiiworks.goodidlegame.core.loadouts.PlayerLoadoutsSaveData
import com.rorokaiiworks.goodidlegame.core.quests.QuestsSystemSaveData
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkillsSaveData
import com.rorokaiiworks.goodidlegame.core.starStore.StarBuffState
import com.rorokaiiworks.goodidlegame.core.talents.TalentTreeSaveData
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSessionSave
import com.rorokaiiworks.goodidlegame.core.traits.TraitSystemSaveData
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CitySaveData
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
data class GameSave(
    var version: GameSaveVersion = GameSaveVersion.V2_COMMUNITY_REWORK,
    var playerName: String = "",
    var inventory: PlayerInventorySaveData = PlayerInventorySaveData(),
    var playerLoadouts: PlayerLoadoutsSaveData = PlayerLoadoutsSaveData(),
    var playerSkills: PlayerSkillsSaveData = PlayerSkillsSaveData(),
    var taskSessionSave: TaskSessionSave? = null,
    var starBuffState: StarBuffState = StarBuffState(),
    var questsSystemSaveData: QuestsSystemSaveData = QuestsSystemSaveData(
        dailyQuestSeed = Random.nextLong(),
    ),
    var journeySaveData: JourneySaveData? = null,
    var gameState: GameStateSaveData = GameStateSaveData(),
    var codexSaveData: CodexSaveData? = null,
    var masteryLevelSaveData: MasteryLevelSaveData? = null,

    var communitySaveData: CommunitySaveData? = null,
    var timeSystemSave: TimeSystemSave = TimeSystemSave(),
    var adPlayerSave: AdPlayerSave = AdPlayerSave(showedAdCount = 0),
    var achievementSystemSaveData: AchievementSystemSaveData? = null,
    var talentTreeSaveData: TalentTreeSaveData? = null,
    var traitSystemSaveData: TraitSystemSaveData? = null,
    var citySaveData: CitySaveData? = null,
    var time: Instant
)
