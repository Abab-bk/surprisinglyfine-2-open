package com.rorokaiiworks.goodidlegame.core.skills

import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import kotlinx.serialization.Serializable

@Serializable
data class SkillTemplate(
    override val id: String,
    val name: String,
    val skillType: SkillType,
    val desc: String,
    val lockItemSlots: Set<String>? = null,
    val selectedCategory: String? = null,
) : Template {
    fun getPotionType() = when (skillType) {
        SkillType.Combat -> ItemType.CombatPotion
        SkillType.Gather -> ItemType.GatherPotion
        SkillType.Craft -> ItemType.CraftPotion
    }
}
