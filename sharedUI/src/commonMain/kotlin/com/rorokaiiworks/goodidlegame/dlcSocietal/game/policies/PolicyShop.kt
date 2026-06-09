package com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies

import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

@Serializable
sealed interface PolicyUnlockCondition {
    fun title(i18n: I18n): String
    fun iconName(): String
    fun progressText(i18n: I18n): String

    @Serializable
    @SerialName("skillLevel")
    data class SkillLevel(
        val skillId: String,
        val level: Int,
    ) : PolicyUnlockCondition, KoinComponent {
        private val skillTemplates: DataTable<SkillTemplate> by inject(named<SkillTemplate>())
        private val playerSkills: PlayerSkills by inject()

        override fun title(i18n: I18n): String = i18n.tr(skillTemplates.find(skillId).name)
        override fun iconName(): String = skillId
        override fun progressText(i18n: I18n): String {
            return "Lv. ${level}/${playerSkills.skills[skillId]?.level}"
        }

    }
}
