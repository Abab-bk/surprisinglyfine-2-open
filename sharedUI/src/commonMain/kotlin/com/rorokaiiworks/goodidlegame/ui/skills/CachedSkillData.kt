package com.rorokaiiworks.goodidlegame.ui.skills

import com.rorokaiiworks.goodidlegame.core.skills.Skill
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction

data class CachedSkillData(
    val skill: Skill,
    val actions: List<SkillAction>,
    val categories: List<String> = actions.mapNotNull { it.category }.distinct(),
    val subCategories: List<String> = actions.mapNotNull { it.subCategory }.distinct(),
)
