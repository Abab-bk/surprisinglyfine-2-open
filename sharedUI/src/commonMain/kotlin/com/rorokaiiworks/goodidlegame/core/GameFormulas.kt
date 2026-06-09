package com.rorokaiiworks.goodidlegame.core

import com.rorokaiiworks.goodidlegame.core.enemies.EnemyType
import com.rorokaiiworks.goodidlegame.core.items.FormulaTag
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import com.rorokaiiworks.goodidlegame.core.skills.Skill
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import com.rorokaiiworks.goodidlegame.core.skills.SkillType
import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType
import com.rorokaiiworks.goodidlegame.core.stats.Stats
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

object GameFormulas {
    // Tier 默认标签分配（1~8）
    // 每个 Tier 只给一个默认倾向，避免自动叠出过高收益。
    private val tierFormulaTags: Map<Int, Set<FormulaTag>> = mapOf(
        1 to emptySet(),
        2 to setOf(FormulaTag.FastAction),
        3 to setOf(FormulaTag.XpFocus),
        4 to setOf(FormulaTag.FastAction),
        5 to setOf(FormulaTag.HighValue),
        6 to setOf(FormulaTag.FastAction),
        7 to setOf(FormulaTag.XpFocus),
        8 to setOf(FormulaTag.HighValue),
    )

    fun getTierFormulaTags(tier: Int): Set<FormulaTag> = tierFormulaTags[tier] ?: emptySet()

    fun getItemFormulaTags(
        tier: Int,
        predefinedTags: Set<FormulaTag> = emptySet()
    ): Set<FormulaTag> = getTierFormulaTags(tier) + predefinedTags

    fun getSkillActionFormulaTags(
        tier: Int,
        predefinedTags: Set<FormulaTag> = emptySet()
    ): Set<FormulaTag> = getTierFormulaTags(tier) + predefinedTags

    private fun normalizedTier(tier: Int): Int = tier.coerceIn(1, 8)

    private fun interpolateAnchors(level: Int, anchors: List<Pair<Int, Float>>): Float {
        require(anchors.isNotEmpty()) { "anchors must not be empty" }
        val sorted = anchors.sortedBy { it.first }
        val clampedLevel = level.coerceAtLeast(1)
        val first = sorted.first()
        val last = sorted.last()

        if (clampedLevel <= first.first) return first.second
        if (clampedLevel >= last.first) {
            val growthSegments = (clampedLevel - last.first) / 5f
            return last.second * (1.04f).pow(growthSegments)
        }

        val upperIndex = sorted.indexOfFirst { clampedLevel <= it.first }
        val lower = sorted[upperIndex - 1]
        val upper = sorted[upperIndex]
        val ratio = (clampedLevel - lower.first).toFloat() / (upper.first - lower.first).toFloat()
        return lower.second + (upper.second - lower.second) * ratio
    }

    private fun calculateWeaponModifiers(itemType: ItemType, tier: Int): List<StatModifier> {
        val normalizedTier = normalizedTier(tier)

        val baseDamage = when (normalizedTier) {
            1 -> 35f
            2 -> 45f
            3 -> 55f
            4 -> 65f
            5 -> 75f
            6 -> 85f
            7 -> 95f
            8 -> 115f
            else -> 1f
        }

        val damagePercent = 0.22f + normalizedTier * 0.12f

        val attackSpeed = when (normalizedTier) {
            1 -> 1.1f
            2 -> 1.25f
            3 -> 1.45f
            4 -> 1.75f
            5 -> 2.1f
            6 -> 2.6f
            7 -> 3.3f
            8 -> 4.4f
            else -> 1f
        }

        val xpBonus = 0.14f + normalizedTier * 0.04f
        val resistanceBonus = 0.12f + normalizedTier * 0.07f

        when (itemType) {
            ItemType.Sword -> return listOf(
                StatModifier(StatIds.Actor.SlashDamage, baseDamage, StatModifierType.Flat),
                StatModifier(StatIds.Actor.SlashDamage, damagePercent, StatModifierType.Percent),
                StatModifier(StatIds.Actor.AttackSpeed, attackSpeed, StatModifierType.Flat),
                StatModifier(StatIds.Skills.CombatXp, xpBonus, StatModifierType.Percent),
            )

            ItemType.Hammer -> return listOf(
                StatModifier(StatIds.Actor.SlashDamage, baseDamage, StatModifierType.Flat),
                StatModifier(StatIds.Actor.SlashResistance, resistanceBonus, StatModifierType.Percent),
                StatModifier(StatIds.Actor.AttackSpeed, attackSpeed, StatModifierType.Flat),
                StatModifier(StatIds.Skills.CombatXp, xpBonus, StatModifierType.Percent),
            )

            ItemType.Scythe -> return listOf(
                StatModifier(StatIds.Actor.PunctureDamage, baseDamage, StatModifierType.Flat),
                StatModifier(StatIds.Actor.PunctureDamage, damagePercent, StatModifierType.Percent),
                StatModifier(StatIds.Actor.AttackSpeed, attackSpeed, StatModifierType.Flat),
                StatModifier(StatIds.Skills.CombatXp, xpBonus, StatModifierType.Percent),
            )

            ItemType.Spear -> return listOf(
                StatModifier(StatIds.Actor.PunctureDamage, baseDamage, StatModifierType.Flat),
                StatModifier(StatIds.Actor.PunctureResistance, resistanceBonus, StatModifierType.Percent),
                StatModifier(StatIds.Actor.AttackSpeed, attackSpeed, StatModifierType.Flat),
                StatModifier(StatIds.Skills.CombatXp, xpBonus, StatModifierType.Percent),
            )

            ItemType.Bow -> return listOf(
                StatModifier(StatIds.Actor.ImpactDamage, baseDamage, StatModifierType.Flat),
                StatModifier(StatIds.Actor.ImpactDamage, damagePercent, StatModifierType.Percent),
                StatModifier(StatIds.Actor.AttackSpeed, attackSpeed, StatModifierType.Flat),
                StatModifier(StatIds.Skills.CombatXp, xpBonus, StatModifierType.Percent),
            )

            ItemType.Dart -> return listOf(
                StatModifier(StatIds.Actor.ImpactDamage, baseDamage, StatModifierType.Flat),
                StatModifier(StatIds.Actor.ImpactResistance, resistanceBonus, StatModifierType.Percent),
                StatModifier(StatIds.Actor.AttackSpeed, attackSpeed, StatModifierType.Flat),
                StatModifier(StatIds.Skills.CombatXp, xpBonus, StatModifierType.Percent),
            )

            else -> return listOf()
        }
    }

    private fun calculateArmorModifiers(itemType: ItemType, tier: Int): List<StatModifier> {
        val normalizedTier = normalizedTier(tier)
        val healthPerTier = 10f * (1.55f).pow(normalizedTier - 1)
        // 护甲：5 * 1.5^(tier-1)，Tier 8单件 = 85护甲
        val armorPerTier = 5f * (1.45f).pow(normalizedTier - 1)

        return when (itemType) {
            ItemType.Shield -> listOf(
                StatModifier("actor_maxHealth", healthPerTier * 1.5f, StatModifierType.Flat),
                StatModifier("actor_armor", armorPerTier * 2f, StatModifierType.Flat),
            )

            ItemType.Helmet, ItemType.Armor,
            ItemType.LegArmor, ItemType.Boots -> listOf(
                StatModifier("actor_maxHealth", healthPerTier, StatModifierType.Flat),
                StatModifier("actor_armor", armorPerTier, StatModifierType.Flat),
            )

            else -> listOf()
        }
    }

    private fun calculateAccessoryModifiers(itemType: ItemType, tier: Int): List<StatModifier> {
        val normalizedTier = normalizedTier(tier)
        val gatherBonus = 0.03f + normalizedTier * 0.03f
        val craftBonus = 0.03f + normalizedTier * 0.03f
        val speedBonus = normalizedTier * 0.02f

        when (itemType) {
            ItemType.Cape -> return listOf(
                StatModifier(StatIds.Actor.AttackSpeed, speedBonus, StatModifierType.Percent, channel = 3),
                StatModifier("skill_gatherSpeed", 0.04f + normalizedTier * 0.018f, StatModifierType.Percent),
            )

            ItemType.Necklace -> return listOf(
                StatModifier("skill_craftYield", craftBonus, StatModifierType.Flat),
                StatModifier("skill_craftXp", 0.025f + normalizedTier * 0.012f, StatModifierType.Percent),
            )

            ItemType.Ring -> return listOf(
                StatModifier("skill_gatherYield", gatherBonus, StatModifierType.Flat),
                StatModifier("skill_gatherXp", 0.025f + normalizedTier * 0.012f, StatModifierType.Percent),
            )

            ItemType.Bracelet -> return listOf(
                StatModifier("skill_gatherYield", gatherBonus * 0.6f, StatModifierType.Flat),
                StatModifier("skill_craftYield", craftBonus * 0.6f, StatModifierType.Flat),
            )

            else -> return listOf()
        }
    }

    private fun calculateToolModifiers(itemType: ItemType, tier: Int): List<StatModifier> {
        val normalizedTier = normalizedTier(tier)
        // 速度：Tier 1 = 42%, Tier 8 = 126%（避免后期过快）
        val speedBonus = 0.24f + normalizedTier * 0.09f

        // 经验：Tier 1 = 5%, Tier 8 = 22.5%
        val xpBonus = 0.025f + normalizedTier * 0.025f

        return when (itemType) {
            ItemType.Axe -> listOf(
                StatModifier("skill_woodcutting_speed", speedBonus, StatModifierType.Percent),
                StatModifier("skill_woodcutting_XpMultiplier", xpBonus, StatModifierType.Percent),
            )

            ItemType.Pickaxe -> listOf(
                StatModifier("skill_mining_speed", speedBonus, StatModifierType.Percent),
                StatModifier("skill_mining_XpMultiplier", xpBonus, StatModifierType.Percent),
            )

            ItemType.Spade -> listOf(
                StatModifier("skill_farming_speed", speedBonus, StatModifierType.Percent),
                StatModifier("skill_farming_XpMultiplier", xpBonus, StatModifierType.Percent),
            )

            ItemType.Rod -> listOf(
                StatModifier("skill_fishing_speed", speedBonus, StatModifierType.Percent),
                StatModifier("skill_fishing_XpMultiplier", xpBonus, StatModifierType.Percent),
            )

            ItemType.Trap -> listOf(
                // 陷阱属于被动收益，增幅略低但基础要高
                StatModifier("skill_hunting_speed", 0.40f + normalizedTier * 0.10f, StatModifierType.Percent),
            )

            else -> listOf()
        }
    }

    fun calculateItemModifiers(
        itemType: ItemType,
        tier: Int,
        tags: Set<FormulaTag> = emptySet()
    ): List<StatModifier> {
        if (tier <= 0) return listOf()
        val baseModifiers = when (itemType) {
            in ItemType.Weapons -> calculateWeaponModifiers(itemType, tier)
            in ItemType.Armors -> calculateArmorModifiers(itemType, tier)
            in ItemType.Accessories -> calculateAccessoryModifiers(itemType, tier)
            in ItemType.Tools -> calculateToolModifiers(itemType, tier)

            else -> listOf()
        }
        return applyItemModifierTags(baseModifiers, tags, tier)
    }

    private fun applyItemModifierTags(
        baseModifiers: List<StatModifier>,
        tags: Set<FormulaTag>,
        tier: Int
    ): List<StatModifier> {
        if (tags.isEmpty()) return baseModifiers

        val result = baseModifiers.toMutableList()
        val normalizedTier = normalizedTier(tier)
        val tierMultiplier = 1f + normalizedTier * 0.15f

        tags.forEach {
            when (it) {
                FormulaTag.FastAction,
                FormulaTag.HighValue,
                FormulaTag.XpFocus -> {}

                FormulaTag.HighSlashDamage -> {
                    result.add(StatModifier(StatIds.Actor.SlashDamage, 0.20f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighPunctureDamage -> {
                    result.add(StatModifier(StatIds.Actor.PunctureDamage, 0.20f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighImpactDamage -> {
                    result.add(StatModifier(StatIds.Actor.ImpactDamage, 0.20f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighMaxHealth -> {
                    result.add(StatModifier(StatIds.Actor.MaxHealth, 0.20f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighArmor -> {
                    result.add(StatModifier(StatIds.Actor.Armor, 0.20f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighDamageMultiplier -> {
                    result.add(StatModifier(StatIds.Actor.DamageMultiplier, 0.18f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighHitChance -> {
                    result.add(StatModifier(StatIds.Actor.HitChanceBonus, 0.08f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighDodgeChance -> {
                    result.add(StatModifier(StatIds.Actor.DodgeChanceBonus, 0.08f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighSlashResistance -> {
                    result.add(StatModifier(StatIds.Actor.SlashResistance, 0.12f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighPunctureResistance -> {
                    result.add(StatModifier(StatIds.Actor.PunctureResistance, 0.12f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighImpactResistance -> {
                    result.add(StatModifier(StatIds.Actor.ImpactResistance, 0.12f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighGatherYield -> {
                    result.add(StatModifier(StatIds.Skills.GatherYield, 0.15f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighCraftYield -> {
                    result.add(StatModifier(StatIds.Skills.CraftYield, 0.15f * tierMultiplier, StatModifierType.Percent))
                }
                FormulaTag.HighLootMultiplier -> {
                    result.add(StatModifier(StatIds.Skills.CombatYield, 0.15f * tierMultiplier, StatModifierType.Percent))
                }
                else -> {}
            }
        }

        return result
    }


    fun calculateEnemyHealth(level: Int, enemyType: EnemyType): Int {
        val baseHealth = interpolateAnchors(
            level,
            listOf(
                1 to 400f,
                10 to 600f,
                25 to 700f,
                40 to 1000f,
                55 to 1200f,
                70 to 1400f,
                85 to 1600f,
                100 to 1800f
            )
        )
        val typeMultiplier = when (enemyType) {
            EnemyType.Slash -> 1.05f
            EnemyType.Puncture -> 0.95f
            EnemyType.Impact -> 1.0f
        }
        return (baseHealth * typeMultiplier).roundToInt().coerceAtLeast(1)
    }

    fun calculateEnemyAttackSpeed(level: Int, enemyType: EnemyType): Float {
        val normalizedLevel = level.coerceAtLeast(1)
        val baseAps = 0.95f
        val growthRate = 0.012f
        val rawAps = baseAps * (1 + (normalizedLevel - 1) * growthRate)
        val typeMultiplier = when (enemyType) {
            EnemyType.Slash -> 1.0f
            EnemyType.Puncture -> 1.06f
            EnemyType.Impact -> 0.94f
        }
        val clampedAps = (rawAps * typeMultiplier).coerceIn(0.7f, 3.2f)
        return round(clampedAps * 100f) / 100f
    }

    fun calculateEnemySlashDamage(level: Int, enemyType: EnemyType): Int {
        if (enemyType != EnemyType.Slash) return 0
        return calculateEnemyDamage(level, enemyType)
    }

    fun calculateEnemyPunctureDamage(level: Int, enemyType: EnemyType): Int {
        if (enemyType != EnemyType.Puncture) return 0
        return calculateEnemyDamage(level, enemyType)
    }

    fun calculateEnemyImpactDamage(level: Int, enemyType: EnemyType): Int {
        if (enemyType != EnemyType.Impact) return 0
        return calculateEnemyDamage(level, enemyType)
    }

    private fun calculateEnemyDamage(level: Int, enemyType: EnemyType): Int {
        val baseDamage = interpolateAnchors(
            level,
            listOf(
                1 to 10f,
                10 to 15f,
                25 to 25f,
                40 to 35f,
                55 to 55f,
                70 to 65f,
                85 to 95f,
                100 to 145f
            )
        )
        val typeMultiplier = when (enemyType) {
            EnemyType.Slash -> 1.0f
            EnemyType.Puncture -> 1.07f
            EnemyType.Impact -> 1.12f
        }
        return (baseDamage * typeMultiplier).roundToInt().coerceAtLeast(1)
    }


    fun calculateEnemySlashResistance(level: Int, enemyType: EnemyType): Float {
        val base = calculateEnemyBaseResistance(level)
        return when (enemyType) {
            EnemyType.Slash -> base
            EnemyType.Puncture -> base + calculateEnemyWeaknessModifier(level)
            EnemyType.Impact -> base + calculateEnemyStrengthModifier(level)
        }
    }

    fun calculateEnemyPunctureResistance(level: Int, enemyType: EnemyType): Float {
        val base = calculateEnemyBaseResistance(level)
        return when (enemyType) {
            EnemyType.Slash -> base + calculateEnemyStrengthModifier(level)
            EnemyType.Puncture -> base
            EnemyType.Impact -> base + calculateEnemyWeaknessModifier(level)
        }
    }

    fun calculateEnemyImpactResistance(level: Int, enemyType: EnemyType): Float {
        val base = calculateEnemyBaseResistance(level)
        return when (enemyType) {
            EnemyType.Slash -> base + calculateEnemyWeaknessModifier(level)
            EnemyType.Puncture -> base + calculateEnemyStrengthModifier(level)
            EnemyType.Impact -> base
        }
    }

    private fun calculateEnemyBaseResistance(level: Int): Float {
        return interpolateAnchors(
            level,
            listOf(
                1 to 0.02f,
                20 to 0.05f,
                40 to 0.08f,
                60 to 0.12f,
                80 to 0.16f,
                100 to 0.20f
            )
        )
    }

    private fun calculateEnemyWeaknessModifier(level: Int): Float {
        // 随着等级提升，弱点带来的负面抗性逐渐减弱
        return interpolateAnchors(
            level,
            listOf(
                1 to -0.25f,
                100 to -0.10f
            )
        )
    }

    private fun calculateEnemyStrengthModifier(level: Int): Float {
        // 随着等级提升，优势属性的抗性奖励逐渐增强
        return interpolateAnchors(
            level,
            listOf(
                1 to 0.10f,
                100 to 0.25f
            )
        )
    }

    // 基础售价（玩家卖给商店）
    fun calculateItemPrice(
        tier: Int,
        itemType: ItemType,
        tags: Set<FormulaTag> = emptySet()
    ): Long {
        if (tier <= 0) return 10

        val basePrice = 10f
        val tierOffset = (tier - 1) * 5f

        val growth = when (itemType) {
            ItemType.Material, ItemType.Misc -> 1.25f

            ItemType.Flower, ItemType.Vegetable,
            ItemType.Fruit, ItemType.Fish -> 1.20f

            ItemType.Food,
            ItemType.CombatPotion,
            ItemType.GatherPotion,
            ItemType.CraftPotion -> 1.40f

            in ItemType.Weapons -> 1.65f
            in ItemType.Armors -> 1.60f
            in ItemType.Accessories -> 1.70f

            in ItemType.Tools -> 1.50f

            ItemType.Relic -> 2.10f

            else -> 1.20f
        }

        // 公式修改：(基础价 + 线性偏移) * 指数增长
        val basePriceValue = ((basePrice + tierOffset) * growth.pow(tier - 1)).roundToInt()
        return applyItemPriceTags(basePriceValue, tags)
    }

    fun applyItemPriceTags(basePrice: Int, tags: Set<FormulaTag> = emptySet()): Long {
        var multiplier = 1f
        if (FormulaTag.HighValue in tags) multiplier *= 1.25f
        if (FormulaTag.XpFocus in tags) multiplier *= 0.85f
        return (basePrice * multiplier).roundToLong().coerceAtLeast(1)
    }

    // 购买价格（从商店购买）
    fun calculateItemPriceForPurchase(tier: Int, itemType: ItemType): Long {
        if (itemType == ItemType.Relic) return Long.MAX_VALUE // 遗物不可购买

        val sellPrice = calculateItemPrice(tier, itemType)

        val markup = when (itemType) {
            ItemType.Material, ItemType.Misc -> 2.0f
            ItemType.Flower, ItemType.Vegetable,
            ItemType.Fruit, ItemType.Fish -> 2.2f
            ItemType.Food,
            ItemType.CombatPotion,
            ItemType.GatherPotion,
            ItemType.CraftPotion -> 2.5f
            in ItemType.Weapons,
            in ItemType.Armors,
            in ItemType.Accessories -> 3.5f // 极高倍率鼓励自制
            in ItemType.Tools -> 2.2f
            else -> 2.5f
        }

        // 提高固定手续费，确保 Tier 8 购买时有明显溢价
        val handlingFee = tier * 20
        return (sellPrice * markup + handlingFee).roundToLong()
    }


    fun calculateSkillSpeedMultiplier(skill: Skill, stats: Stats): Float {
        val personalSpeed =
            stats["${skill.template.id}_speed"]?.value ?: 1f

        val typeSpeed = when (skill.template.skillType) {
            SkillType.Combat -> 1f
            SkillType.Gather -> stats[StatIds.Skills.GatherSpeed]?.value ?: 1f
            SkillType.Craft -> stats[StatIds.Skills.CraftSpeed]?.value ?: 1f
        }

        return (personalSpeed * typeSpeed).coerceIn(0.2f, 5f)
    }

    // 每个技能都有一系列行动，每个行动都有一个持续时间
    // 就像 Melvor Idle 一样，比如说每种树木砍伐时间不同
    fun calculateSkillActionDurationByTier(tier: Int, tags: Set<FormulaTag> = emptySet()): Float {
        // 行动时长保持在较短区间，兼顾节奏与升级速度控制
        val normalizedTier = normalizedTier(tier)
        val baseDuration = when(normalizedTier) {
            1 -> 4.2f
            2 -> 4.8f
            3 -> 5.4f
            4 -> 6.0f
            5 -> 6.6f
            6 -> 7.2f
            7 -> 7.8f
            8 -> 8.4f
            else -> 9.0f
        }
        val speedMultiplier = if (FormulaTag.FastAction in tags) 0.9f else 1f
        return (baseDuration * speedMultiplier * 100).roundToInt() / 100f
    }

    fun calculateSkillActionGetXpByTier(tier: Int, tags: Set<FormulaTag> = emptySet()): Long {
        val duration = calculateSkillActionDurationByTier(tier, tags)
        val normalizedTier = normalizedTier(tier)
        // 使用较平滑的每秒 XP 成长，避免高 Tier 爆发
        val xpPerSecond = 4f * (1.25f).pow(normalizedTier - 1)
        val xpMultiplier = if (FormulaTag.XpFocus in tags) 1.16f else 1f
        return (xpPerSecond * duration * xpMultiplier).roundToLong()
    }

    fun calculateSkillLootMultiplier(skill: Skill, stats: Stats): Float {
        return stats["${skill.template.id}_lootMultiplier"]?.value ?: 1f
    }

    fun calculateSkillXpMultiplier(skill: Skill, stats: Stats): Float {
        val typeBonus = stats[skill.template.getSkillTypeXpMultiplierStat()]?.value ?: 1f
        val personalBonus = stats["${skill.template.id}_XpMultiplier"]?.value ?: 1f

        return (typeBonus * personalBonus).coerceIn(0.1f, 6f)
    }

    // 这里算出来的是从 Lv.(level-1) 到 Lv.level 所需 XP
    fun calculateSkillXpNeededForLevel(level: Int): Long {
        val base = level - 1
        return (0.3f * (base + 250 * 2f.pow(base.toFloat() / 7f))).roundToLong()
    }

    fun checkSkillEfficiency(skill: Skill, stats: Stats): Boolean {
        val levelBonus = (skill.level - 1) * 0.0025f
        val statBonus = stats["${skill.template.id}_efficiency"]?.value ?: 0f
        val efficiency = (levelBonus + statBonus).coerceIn(0f, 0.25f) // 上限 25%
        return Random.nextFloat() < efficiency
    }

    fun calculateSkillYieldMultiplier(skill: Skill, stats: Stats): Float {
        return (stats[skill.template.getYieldMultiplierStat()]?.value ?: 1f).coerceIn(0.1f, 6f)
    }

    fun calculateArchaeologySkillActionGetXpByTier(
        tier: Int,
        tags: Set<FormulaTag> = emptySet()
    ): Long {
        val duration = getArchaeologyTaskDuration(tier, tags)
        val normalizedTier = normalizedTier(tier)
        val baseXpPerSecond = 0.08f
        val tierMultiplier = (1.12f).pow(normalizedTier - 1)
        val xpMultiplier = if (FormulaTag.XpFocus in tags) 1.16f else 1f
        return (duration * baseXpPerSecond * tierMultiplier * xpMultiplier).roundToLong()
    }

    fun getArchaeologyTaskDuration(tier: Int, tags: Set<FormulaTag> = emptySet()): Float {
        val baseDuration = normalizedTier(tier) * 3600f
        val speedMultiplier = if (FormulaTag.FastAction in tags) 0.9f else 1f
        return (baseDuration * speedMultiplier).coerceAtLeast(1f)
    }

    fun getSkillActionRequiredLevel(tier: Int): Int {
        return when(tier) {
            1 -> 1
            2 -> 10
            3 -> 20
            4 -> 32
            5 -> 45
            6 -> 60
            7 -> 77
            8 -> 95
            else -> 100
        }
    }

    private fun SkillTemplate.getSkillTypeXpMultiplierStat() = when (skillType) {
        SkillType.Combat -> StatIds.Skills.CombatXp
        SkillType.Gather -> StatIds.Skills.GatherXp
        SkillType.Craft -> StatIds.Skills.CraftXp
    }

    private fun SkillTemplate.getYieldMultiplierStat() = when (skillType) {
        SkillType.Combat -> StatIds.Skills.CombatYield
        SkillType.Gather -> StatIds.Skills.GatherYield
        SkillType.Craft -> StatIds.Skills.CraftYield
    }
}
