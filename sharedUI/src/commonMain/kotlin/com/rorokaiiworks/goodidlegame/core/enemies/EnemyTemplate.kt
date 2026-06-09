package com.rorokaiiworks.goodidlegame.core.enemies

import com.rorokaiiworks.goodidlegame.core.GameFormulas
import com.rorokaiiworks.goodidlegame.core.data.Template
import goodidlegame.sharedui.generated.resources.Res
import goodidlegame.sharedui.generated.resources.allDrawableResources
import kotlinx.serialization.Serializable

@Serializable
enum class EnemyType {
    Slash,
    Puncture,
    Impact,
}

@Serializable
data class EnemyTemplate(
    override val id: String,
    val name: String,

    val enemyType: EnemyType,

    val level: Int = 1,
    val tier: Int = -1,

    val slashDamageMultiplier: Float = 1f,
    val punctureDamageMultiplier: Float = 1f,
    val impactDamageMultiplier: Float = 1f,

    val slashResistanceMultiplier: Float = 1f,
    val punctureResistanceMultiplier: Float = 1f,
    val impactResistanceMultiplier: Float = 1f,
) : Template {
    val attackSpeed: Float = GameFormulas.calculateEnemyAttackSpeed(level, enemyType)
    val maxHealth: Int = GameFormulas.calculateEnemyHealth(level, enemyType)

    val slashDamage: Int = (GameFormulas.calculateEnemySlashDamage(level, enemyType) * slashDamageMultiplier).toInt()
    val punctureDamage: Int = (GameFormulas.calculateEnemyPunctureDamage(level, enemyType) * punctureDamageMultiplier).toInt()
    val impactDamage: Int = (GameFormulas.calculateEnemyImpactDamage(level, enemyType) * impactDamageMultiplier).toInt()

    val slashResistance: Float = GameFormulas.calculateEnemySlashResistance(level, enemyType) * slashResistanceMultiplier
    val punctureResistance: Float = GameFormulas.calculateEnemyPunctureResistance(level, enemyType) * punctureResistanceMultiplier
    val impactResistance: Float = GameFormulas.calculateEnemyImpactResistance(level, enemyType) * impactResistanceMultiplier

    val iconName by lazy {
        when {
            Res.allDrawableResources.containsKey(id) -> id
            id.endsWith("_elite") -> id.replace("_elite", "")
            else -> "default"
        }
    }
}