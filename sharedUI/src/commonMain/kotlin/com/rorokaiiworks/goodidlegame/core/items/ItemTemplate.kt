package com.rorokaiiworks.goodidlegame.core.items

import com.rorokaiiworks.goodidlegame.core.GameFormulas
import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.drops.DropTable
import com.rorokaiiworks.goodidlegame.core.stats.ISourceName
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.traits.Perk
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.CityFormulas
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations.PopulationTier
import kotlinx.serialization.Serializable

@Serializable
enum class FormulaTag {
    FastAction, // 动作更快
    HighValue, // 更贵
    XpFocus, // XP 更多，但便宜


    // ==== 对于装备 ====
    HighSlashDamage, // Slash Damage 伤害更高
    HighPunctureDamage, // Puncture Damage 伤害更高
    HighImpactDamage, // Impact Damage 伤害更高

    HighMaxHealth, // 最大生命值更高
    HighArmor, // 护甲更高
    HighDamageMultiplier, // 伤害倍率更高
    HighHitChance, // 命中几率更高
    HighDodgeChance, // 闪避几率更高

    HighSlashResistance, // 劈砍抗性更高
    HighPunctureResistance, // 穿刺抗性更高
    HighImpactResistance, // 冲击抗性更高

    HighGatherYield, // 采集产量更高
    HighCraftYield, // 制作产量更高
    HighLootMultiplier, // 战利品倍率更高


    CityItem,
}

@Serializable
data class ItemTemplate(
    override val id: String,
    val name: String,
    val type: ItemType = ItemType.Misc,
    var modifiers: List<StatModifier>? = null,
    val canSell: Boolean = true,
    val tier: Int = -1, // currently used by quest generation
    val potionDuration: Int = 0,
    val cityTier: Int = -1,
    val populationTier: PopulationTier? = null,
    val perk: Perk? = null,
    val tags: Set<FormulaTag> = setOf(),
    val dropTable: DropTable? = null
) : Template, ISourceName {
    val rarity: ItemRarity get() = calculateRarity()
    override val sourceName: String get() = name

    fun calculateRarity(): ItemRarity {
        if (perk != null) return ItemRarity.Legendary
        return when (tier) {
            0, 1, 2, 3 -> ItemRarity.Common
            6, 5, 4 -> ItemRarity.Uncommon
            7 -> ItemRarity.Rare
            8 -> ItemRarity.Epic
            else -> ItemRarity.Common
        }
    }

    val effectiveTags: Set<FormulaTag> = GameFormulas.getItemFormulaTags(
        tier = tier,
        predefinedTags = tags,
    )

    init {
        modifiers = GameFormulas.calculateItemModifiers(
            itemType = type,
            tier = tier,
            tags = effectiveTags,
        ).toMutableList().apply { addAll(modifiers ?: listOf()) }
    }

    val price: Long =
        if (!canSell) 0L
        else if (cityTier > 0) GameFormulas.applyItemPriceTags(
            basePrice = CityFormulas.calculateCityItemPrice(cityTier, populationTier!!),
            tags = effectiveTags,
        )
        else GameFormulas.calculateItemPrice(
            tier = tier,
            itemType = type,
            tags = effectiveTags,
        )
}
