package com.rorokaiiworks.goodidlegame.core.combat

import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.enemies.Enemy
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.stats.StatIds

object DamageCalculator {
    fun process(attack: Attack, defender: IActor, playerSkills: PlayerSkills): ProcessedAttack {
        val slashResistance = defender.stats[StatIds.Actor.SlashResistance]!!.value
        val punctureResistance = defender.stats[StatIds.Actor.PunctureResistance]!!.value
        val impactResistance = defender.stats[StatIds.Actor.ImpactResistance]!!.value
        val armor = defender.stats[StatIds.Actor.Armor]!!.value

        val attackerDamageMultiplier = attack.attacker.stats[StatIds.Actor.DamageMultiplier]!!.value
        val defenderDamageTakenMultiplier = defender.stats[StatIds.Actor.DamageTakenMultiplier]!!.value
        var totalMultiplier = attackerDamageMultiplier * defenderDamageTakenMultiplier

        // 顶级对战做轻微保底：玩家打 Tier 8 怪物更稳，避免满装仍被时间墙卡住。
        if (attack.attacker !is Enemy && defender is Enemy && defender.template.tier >= 8) {
            totalMultiplier *= 1.15f
        }
        if (attack.attacker is Enemy && attack.attacker.template.tier >= 8 && defender !is Enemy) {
            totalMultiplier *= 0.92f
        }

        // 1. 应用技能与 buff 乘区
        var slashDamage = attack.slashDamage * totalMultiplier
        var punctureDamage = attack.punctureDamage * totalMultiplier
        var impactDamage = attack.impactDamage * totalMultiplier

        // 2. 应用伤害类型抗性（最高90%减免）
        slashDamage = calculateDamageAfterResistance(
            slashDamage,
            slashResistance.coerceIn(-0.6f, 0.9f)
        )
        punctureDamage = calculateDamageAfterResistance(
            punctureDamage,
            punctureResistance.coerceIn(-0.6f, 0.9f)
        )
        impactDamage = calculateDamageAfterResistance(
            impactDamage,
            impactResistance.coerceIn(-0.6f, 0.9f)
        )

        var totalDamage = slashDamage + punctureDamage + impactDamage
        totalDamage = calculateDamageAfterArmor(totalDamage, armor)

        // 3. 重新分配伤害到各类型（保持比例）
        val damageRatio = if (totalDamage > 0f) {
            totalDamage / (slashDamage + punctureDamage + impactDamage).coerceAtLeast(0.01f)
        } else 0f

        slashDamage *= damageRatio
        punctureDamage *= damageRatio
        impactDamage *= damageRatio

        // 4. 如果总伤害低于1，按比例提升到1
        if (slashDamage + punctureDamage + impactDamage < 1f) {
            val scaleFactor = 1f / (slashDamage + punctureDamage + impactDamage).coerceAtLeast(0.01f)
            slashDamage *= scaleFactor
            punctureDamage *= scaleFactor
            impactDamage *= scaleFactor
        }

        return ProcessedAttack(
            attacker = attack.attacker,
            defender = defender,
            slashDamage = slashDamage.coerceAtLeast(0.1f),
            punctureDamage = punctureDamage.coerceAtLeast(0.1f),
            impactDamage = impactDamage.coerceAtLeast(0.1f),
            isOneHanded = attack.isOneHanded,
            isTwoHanded = attack.isTwoHanded,
            isRanged = attack.isRanged,
        )
    }

    /**
     * 计算应用抗性后的伤害
     * @param damage 原始伤害
     * @param resistance 抗性值 (0.0 到 0.9)
     * @return 减伤后的伤害
     */
    private fun calculateDamageAfterResistance(damage: Float, resistance: Float): Float {
        return damage * (1f - resistance)
    }

    /**
     * 计算应用护甲后的伤害
     * 护甲公式：减伤率 = 护甲 / (护甲 + 100)
     * 这样护甲永远不会达到100%减伤，但会越来越有效
     *
     * 示例：
     * - 护甲 0: 0% 减伤
     * - 护甲 50: 33.3% 减伤
     * - 护甲 100: 50% 减伤
     * - 护甲 200: 66.7% 减伤
     * - 护甲 400: 80% 减伤
     *
     * @param damage 原始伤害
     * @param armor 护甲值
     * @return 减伤后的伤害
     */
    private fun calculateDamageAfterArmor(damage: Float, armor: Float): Float {
        val damageReduction = armor / (armor + 130f)
        return damage * (1f - damageReduction)
    }


    /**
     * 计算命中率
     * 基础命中率：75%
     * 每高出对方一级：+0.5%
     * 每低于对方一级：-0.5%
     * 上限：90%
     * 下限：5%
     */
    fun calculateHitChance(attackerLevel: Int, defenderLevel: Int): Float {
        val levelDifference = attackerLevel - defenderLevel
        val baseAccuracy = 0.75f
        val accuracyModifier = levelDifference * 0.005f  // 0.5% per level

        return (baseAccuracy + accuracyModifier).coerceIn(0.05f, 0.90f)
    }

    fun getAttackDuration(actor: IActor): Float =
        5f / ((actor.stats[StatIds.Actor.AttackSpeed]?.value ?: 1f).coerceAtLeast(0.01f))

}
