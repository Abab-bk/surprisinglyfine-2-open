package com.rorokaiiworks.goodidlegame.ui.skills

import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.props.PropSlot
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction

data class SkillUiState(
    val mode: SkillMode = SkillMode.Viewing,

    val selectedAction: SkillAction,
    val selectedCategory: String,
    val selectedSubCategory: String?,

    val showSkillDesc: Boolean = false,

    val autoEquipSlotId: String? = null,
)


sealed class SkillMode {
    data object Viewing : SkillMode()

    data class Crafting(
        val craftProductId: String,
    ) : SkillMode()

    data class SelectingProp(val propSlot: PropSlot) : SkillMode()
    data class SelectingItem(
        val selectedItem: Item?
    ) : SkillMode()
}
