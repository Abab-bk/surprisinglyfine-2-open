package com.rorokaiiworks.goodidlegame.core.skills

import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import kotlinx.serialization.Serializable

@Serializable
data class PlayerSkillsSaveData(
    val skills: Map<String, SkillSaveData> = emptyMap()
)


class PlayerSkills(
    skillTemplatesTable: DataTable<SkillTemplate>
) : IPersistable {
    val skills: MutableMap<String, Skill> = skillTemplatesTable
        .all()
        .associateBy(
            keySelector = { it.id },
            valueTransform = { Skill(it) }
        )
        .toMutableMap()

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.playerSkills = PlayerSkillsSaveData(
            skills = skills.mapValues { (_, skill) ->
                SkillSaveData(
                    level = skill.level,
                    currentXp = skill.currentXp,
                    totalXp = skill.totalXp,
                    propsContainerSave = skill.propsContainer?.trySave(currentMills),
                )
            }
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        gameSave.playerSkills.skills.forEach { (skillId, saveData) ->
            val skill = skills[skillId] ?: return@forEach

            skill.level = saveData.level
            skill.currentXp = saveData.currentXp
            skill.totalXp = saveData.totalXp

            saveData.propsContainerSave?.let { skill.propsContainer?.tryLoad(it, currentMills) }
        }
    }
}
