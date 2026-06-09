package com.rorokaiiworks.goodidlegame.core.requirements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.GameState
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.enemies.EnemyTemplate
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.quests.Quest
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.TradeMode
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.trc
import com.rorokaiiworks.goodidlegame.ui.i18nWrapperContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

@Serializable
sealed class Requirement : KoinComponent {
    abstract fun isMet(): Boolean
    abstract fun toText(i18n: I18n): String

    open val progressText: String? get() = null

    var countState by mutableStateOf(0L)
        private set

    var currentCount: Long = 0
        set(value) {
            field = value
            countState = value
        }

    @SerialName("noRequirement")
    @Serializable
    class NoRequirement : Requirement() {
        override fun isMet(): Boolean = true
        override fun toText(i18n: I18n): String = i18n.tr("No requirement")
    }

    @SerialName("skillRequirement")
    @Serializable
    class SkillRequirement(
        val skillId: String,
        val level: Int,
    ) : Requirement() {
        private val playerSkills: PlayerSkills by inject()

        override val progressText: String get() = "${playerSkills.skills[skillId]?.level} / $level"

        override fun isMet(): Boolean {
            val skill = playerSkills.skills[skillId]
            return (skill?.level ?: 0) >= level
        }

        override fun toText(i18n: I18n): String {
            return i18n.tr(
                "{0} reach level {1}",
                i18n.tr(playerSkills.skills[skillId]?.template?.name ?: "Unknown skill"),
                level,
            )
        }
    }

    @SerialName("questCompleted")
    @Serializable
    class QuestCompleted(
        val questId: String,
    ) : Requirement() {
        private val gameState: GameState by inject()
        private val journalQuests: DataTable<Quest> by inject(named<Quest>())

        override fun isMet(): Boolean = questId in gameState.finishedQuests

        override fun toText(i18n: I18n): String {
            return i18n.tr(
                "{0} completed",
                i18n.tr(journalQuests.find(questId).name ?: "Error Quest"),
            )
        }
    }


    @SerialName("enemyKilled")
    @Serializable
    data class EnemyKilled(
        val enemyId: String,
        var need: Int = 0,
    ) : Requirement() {
        private val enemyTemplates: DataTable<EnemyTemplate> by inject(named<EnemyTemplate>())
        private val enemyTemplate get() = enemyTemplates.find(enemyId)

        override val progressText: String get() = "$countState / $need"
        override fun isMet(): Boolean = currentCount >= need

        override fun toText(i18n: I18n): String {
            return i18n.tr("Kill {0} {1}", need, i18n.tr(enemyTemplate.name))
        }
    }

    @SerialName("itemCollected")
    @Serializable
    data class ItemCollected(
        val itemId: String,
        var need: Int = 0,
    ) : Requirement() {
        private val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
        private val itemTemplate get() = itemTemplates.find(itemId)

        override val progressText: String get() = "$countState / $need"
        override fun isMet(): Boolean = currentCount >= need

        override fun toText(i18n: I18n): String {
            return i18n.tr("Collect {0} {1}", need, i18n.tr(itemTemplate.name))
        }
    }

    @SerialName("finishSkillAction")
    @Serializable
    data class FinishSkillAction(
        val skillId: String,
        var need: Int = 0,
    ) : Requirement() {
        private val skillTemplates: DataTable<SkillTemplate> by inject(named<SkillTemplate>())
        private val skillTemplate get() = skillTemplates.find(skillId)

        override val progressText: String get() = "$countState / $need"
        override fun isMet(): Boolean = currentCount >= need

        override fun toText(i18n: I18n): String {
            return i18n.tr("Finish {0} {1}", need, i18n.tr(skillTemplate.name))
        }
    }


    @SerialName("cityBuiltBuilding")
    @Serializable
    data class CityBuiltBuilding(
        val buildingId: String,
        var need: Int = 0,
    ) : Requirement() {
        private val buildingTemplates: DataTable<BuildingTemplate> by inject(named<BuildingTemplate>())
        private val buildingTemplate get() = buildingTemplates.find(buildingId)

        private val wrapper = i18nWrapperContext("Build buildings", "Build {0} {1}")

        override val progressText: String get() = "$countState / $need"
        override fun isMet(): Boolean = currentCount >= need

        override fun toText(i18n: I18n): String {
            return i18n.trc("Build buildings", "Build {0} {1}", need, i18n.tr(buildingTemplate.name))
        }
    }

    @SerialName("cityItemTradeMode")
    @Serializable
    data class CityItemTradeMode(
        val itemId: String,
        val tradeMode: TradeMode
    ) : Requirement() {
        var finished: Boolean = false

        override val progressText: String get() = ""
        override fun isMet(): Boolean = finished

        override fun toText(i18n: I18n): String {
            return ""
        }
    }

}

object RequirementProcessor {
    fun processEvent(event: IEvent, requirements: List<Requirement>) {
        requirements.forEach { requirement ->
            updateRequirement(requirement, event)
        }
    }

    private fun updateRequirement(requirement: Requirement, event: IEvent) {
        when (requirement) {
            is Requirement.EnemyKilled -> {
                if (event is IEvent.EnemyKilled && event.enemyId == requirement.enemyId) {
                    requirement.currentCount++
                }
            }

            is Requirement.ItemCollected -> {
                if (event is IEvent.ItemCollected && event.itemId == requirement.itemId) {
                    requirement.currentCount += event.count
                }
            }

            is Requirement.FinishSkillAction -> {
                if (event is IEvent.FinishSkillAction && event.skillId == requirement.skillId) {
                    requirement.currentCount++
                }
            }

            is Requirement.NoRequirement,
            is Requirement.QuestCompleted -> { }
            is Requirement.SkillRequirement -> {

            }

            is Requirement.CityBuiltBuilding -> {
                if (event is IEvent.BuildingBuilt && event.buildingId == requirement.buildingId) {
                    requirement.currentCount += event.count
                }
            }

            is Requirement.CityItemTradeMode -> {
                if (event is IEvent.CityItemTradeModeChanged && event.itemId == requirement.itemId) {
                    requirement.finished = event.tradeMode == requirement.tradeMode
                }
            }
        }
    }
}


fun List<Requirement>.handleEvent(event: IEvent) {
    RequirementProcessor.processEvent(event, this)
}

@Composable
fun Requirement.iconName(
    enemyTemplates: DataTable<EnemyTemplate> = koinInject(named<EnemyTemplate>()),
    itemTemplates: DataTable<ItemTemplate> = koinInject(named<ItemTemplate>()),
    skillTemplates: DataTable<SkillTemplate> = koinInject(named<SkillTemplate>()),
    buildingTemplates: DataTable<BuildingTemplate> = koinInject(named<BuildingTemplate>()),
): String {
    return when (this) {
        is Requirement.EnemyKilled -> enemyTemplates.find(this.enemyId).id
        is Requirement.ItemCollected -> itemTemplates.find(this.itemId).id
        is Requirement.FinishSkillAction -> skillTemplates.find(this.skillId).id
        is Requirement.NoRequirement -> "default"
        is Requirement.QuestCompleted -> "quests"
        is Requirement.SkillRequirement -> skillTemplates.find(this.skillId).id
        is Requirement.CityBuiltBuilding -> buildingTemplates.find(this.buildingId).id
        is Requirement.CityItemTradeMode -> this.itemId
    }
}