package com.rorokaiiworks.goodidlegame.core.tasks

import com.rorokaiiworks.goodidlegame.core.combat.CombatSession
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class TaskSessionSave(
    val skillActionId: String,
    val passiveSkillActionIds: List<String> = emptyList()
) {
    @OptIn(ExperimentalTime::class)
    fun toTaskSession(
        skillActions: DataTable<SkillAction>,
        playerSkills: PlayerSkills,
        player: Player,
    ): Pair<TaskSession?, List<TaskSession>?> {
        val skillAction = skillActions.findOrNull(skillActionId)
        val skill = playerSkills.skills[skillAction?.skillId] ?: return (null to emptyList())

        val activeSession = when (skillAction) {
            null -> null
            else -> getTaskByAction(
                action = skillAction,
                player = player,
                playerSkills = playerSkills
            )?.let { task ->
                TaskSession(
                    subTitle = "",
                    repeatCount = Int.MAX_VALUE,
                    title = "",
                    skill = skill,
                    task = task,
                    executionMode = TaskExecutionMode.OfflineSimulation
                )
            }
        }

        val passiveSessions = passiveSkillActionIds
            .mapNotNull { id -> skillActions.findOrNull(id) }
            .mapNotNull { action ->
                val passiveSkill = playerSkills.skills[action.skillId] ?: return@mapNotNull null
                val task = getTaskByAction(
                    action = action,
                    player = player,
                    playerSkills = playerSkills
                ) ?: return@mapNotNull null

                TaskSession(
                    subTitle = "",
                    repeatCount = Int.MAX_VALUE,
                    title = "",
                    skill = passiveSkill,
                    task = task,
                    executionMode = TaskExecutionMode.OfflineSimulation
                )
            }

        return (activeSession to passiveSessions)
    }

    private fun getTaskByAction(
        action: SkillAction,
        player: Player,
        playerSkills: PlayerSkills
    ): Task? {
        return when (action) {
            is SkillAction.CombatSkillAction -> {
                val skill = playerSkills.skills[action.skillId] ?: return null
                Task.Combat(
                    combatSession = CombatSession(
                        player = player,
                        action = action,
                        skill = skill,
                    ),
                    action = action
                )
            }

            is SkillAction.ArchaeologySkillAction,
            is SkillAction.NormalSkillAction -> {
                Task.Train(action)
            }
        }
    }
}
