package com.rorokaiiworks.goodidlegame.core.tasks

import com.rorokaiiworks.goodidlegame.core.combat.CombatSession
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction

sealed interface Task {
    val action: SkillAction

    data class Train(
        override val action: SkillAction,
    ) : Task

    data class Combat(
        val combatSession: CombatSession,
        override val action: SkillAction,
    ) : Task
}