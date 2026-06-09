package com.rorokaiiworks.goodidlegame.core.skills

import com.rorokaiiworks.goodidlegame.core.GameFormulas
import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.drops.DropTable
import com.rorokaiiworks.goodidlegame.core.items.FormulaTag
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.tasks.TaskStartResult
import goodidlegame.sharedui.generated.resources.Res
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import goodidlegame.sharedui.generated.resources.allDrawableResources

@Serializable
sealed interface SkillAction : Template {
    val name: String
    val skillId: String

    val tier: Int
    val requiredLevel: Int
        get() = GameFormulas.getSkillActionRequiredLevel(tier)
    val effectiveTags: Set<FormulaTag>
        get() = GameFormulas.getSkillActionFormulaTags(tier, tags)
    val duration: Float
        get() = GameFormulas.calculateSkillActionDurationByTier(tier, effectiveTags)
    val getXp: Long
        get() = GameFormulas.calculateSkillActionGetXpByTier(tier, effectiveTags)

    val tags: Set<FormulaTag>

    val category: String?
    val subCategory: String?
    val dropTable: DropTable?
    val consumeItems: List<ItemEntry>?
    val isPassive: Boolean

    @Transient
    val playerInventory: PlayerInventory

    @Transient
    val playerSkills: PlayerSkills

    fun getIconName(): String {
        if (Res.allDrawableResources.containsKey(id)) {
            return id
        }

        if (dropTable?.entries?.firstOrNull { it.isAlways } != null) {
            return dropTable?.entries?.firstOrNull { it.isAlways }!!.itemId
        }
        return id
    }

    fun getAlwaysDropItemId(): String? =
        dropTable?.entries?.firstOrNull { it.isAlways }?.itemId

    fun canStart(): TaskStartResult {
        if (playerInventory.inventory.isFull()) return TaskStartResult.InventoryFull
        if (consumeItems != null && !playerInventory.inventory.canConsume(consumeItems!!))
            return TaskStartResult.ConsumeItemsNotMet
        val skill = playerSkills.skills[skillId]
        if ((skill?.level ?: 0) < requiredLevel) return TaskStartResult.SkillLevelNotMet
        return TaskStartResult.Success
    }

    @Serializable
    @SerialName("NormalSkillAction")
    data class NormalSkillAction(
        override val name: String,
        override val skillId: String,
        override val tier: Int,
        override val category: String? = null,
        override val subCategory: String? = null,
        override val dropTable: DropTable? = null,
        override val consumeItems: List<ItemEntry>? = null,
        override val tags: Set<FormulaTag> = setOf(),
        override val id: String
    ) : SkillAction, KoinComponent {
        override val isPassive: Boolean = false

        override val playerInventory: PlayerInventory by inject()
        override val playerSkills: PlayerSkills by inject()
    }

    @Serializable
    @SerialName("CombatSkillAction")
    data class CombatSkillAction(
        val enemyIds: List<String>,
        val minWaves: Int,
        val maxWaves: Int,
        val isLastWaveBoss: Boolean,
        override val name: String,
        override val skillId: String,
        override val tier: Int,
        override val category: String? = null,
        override val subCategory: String? = null,
        override val dropTable: DropTable? = null,
        override val consumeItems: List<ItemEntry>? = null,
        override val tags: Set<FormulaTag> = setOf(),
        override val id: String,
    ) : SkillAction, KoinComponent {
        override val playerInventory: PlayerInventory by inject()
        override val playerSkills: PlayerSkills by inject()

        override val isPassive: Boolean = false

        override val requiredLevel: Int
            get() = 0
        override val duration: Float
            get() = 0f

        override fun getIconName(): String {
            val enemyId = enemyIds.first()

            if (Res.allDrawableResources.containsKey(enemyId)) {
                return enemyId
            }

            if (enemyId.endsWith("_elite")) {
                return enemyId.replace("_elite", "")
            }

            return "default"
        }
    }


    @Serializable
    @SerialName("ArchaeologySkillAction")
    data class ArchaeologySkillAction(
        override val name: String,
        override val skillId: String,
        override val tier: Int,
        override val category: String? = null,
        override val subCategory: String? = null,
        override val dropTable: DropTable? = null,
        override val consumeItems: List<ItemEntry>? = null,
        override val id: String,
        override val tags: Set<FormulaTag> = setOf(),
        val neededMapId: String,
    ) : SkillAction, KoinComponent {
        override val playerInventory: PlayerInventory by inject()
        override val playerSkills: PlayerSkills by inject()

        override val isPassive: Boolean = true

        override val getXp: Long
            get() = GameFormulas.calculateArchaeologySkillActionGetXpByTier(tier, effectiveTags)

        override val duration: Float
            get() = GameFormulas.getArchaeologyTaskDuration(tier, effectiveTags)

        override fun getIconName(): String = neededMapId

    }
}
