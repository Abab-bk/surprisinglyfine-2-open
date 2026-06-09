package com.rorokaiiworks.goodidlegame.core.skills

import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.StatSet

class SkillsStats : StatSet(StatIds.Skills.Skills) {
    init {
        add(StatIds.Skills.WoodcuttingEfficiency, 0f)
        add(StatIds.Skills.MiningEfficiency, 0f)
        add(StatIds.Skills.SmeltingEfficiency, 0f)
        add(StatIds.Skills.SmithingEfficiency, 0f)
        add(StatIds.Skills.FarmingEfficiency, 0f)
        add(StatIds.Skills.AlchemyEfficiency, 0f)
        add(StatIds.Skills.FishingEfficiency, 0f)
        add(StatIds.Skills.CookingEfficiency, 0f)
        add(StatIds.Skills.HuntingEfficiency, 0f)
        add(StatIds.Skills.ChartingEfficiency, 0f)
        add(StatIds.Skills.ArchaeologyEfficiency, 0f)
        add(StatIds.Skills.OneHandedEfficiency, 0f)
        add(StatIds.Skills.TwoHandedEfficiency, 0f)
        add(StatIds.Skills.RangedEfficiency, 0f)
        add(StatIds.Skills.DefenseEfficiency, 0f)

        add(StatIds.Skills.WoodcuttingSpeed, 1f)
        add(StatIds.Skills.MiningSpeed, 1f)
        add(StatIds.Skills.SmeltingSpeed, 1f)
        add(StatIds.Skills.SmithingSpeed, 1f)
        add(StatIds.Skills.FarmingSpeed, 1f)
        add(StatIds.Skills.AlchemySpeed, 1f)
        add(StatIds.Skills.FishingSpeed, 1f)
        add(StatIds.Skills.CookingSpeed, 1f)
        add(StatIds.Skills.HuntingSpeed, 1f)
        add(StatIds.Skills.ChartingSpeed, 1f)
        add(StatIds.Skills.ArchaeologySpeed, 1f)
        add(StatIds.Skills.OneHandedSpeed, 1f)
        add(StatIds.Skills.DefenseSpeed, 1f)
        add(StatIds.Skills.TwoHandedSpeed, 1f)
        add(StatIds.Skills.RangedSpeed, 1f)


        add(StatIds.Skills.WoodcuttingXpMultiplier, 1f)
        add(StatIds.Skills.MiningXpMultiplier, 1f)
        add(StatIds.Skills.SmeltingXpMultiplier, 1f)
        add(StatIds.Skills.SmithingXpMultiplier, 1f)
        add(StatIds.Skills.FarmingXpMultiplier, 1f)
        add(StatIds.Skills.AlchemyXpMultiplier, 1f)
        add(StatIds.Skills.FishingXpMultiplier, 1f)
        add(StatIds.Skills.CookingXpMultiplier, 1f)
        add(StatIds.Skills.HuntingXpMultiplier, 1f)
        add(StatIds.Skills.ChartingXpMultiplier, 1f)
        add(StatIds.Skills.ArchaeologyXpMultiplier, 1f)
        add(StatIds.Skills.OneHandedXpMultiplier, 1f)
        add(StatIds.Skills.TwoHandedXpMultiplier, 1f)
        add(StatIds.Skills.RangedXpMultiplier, 1f)
        add(StatIds.Skills.DefenseXpMultiplier, 1f)

        add(StatIds.Skills.WoodcuttingLootMultiplier, 1f)
        add(StatIds.Skills.MiningLootMultiplier, 1f)
        add(StatIds.Skills.SmeltingLootMultiplier, 1f)
        add(StatIds.Skills.SmithingLootMultiplier, 1f)
        add(StatIds.Skills.FarmingLootMultiplier, 1f)
        add(StatIds.Skills.AlchemyLootMultiplier, 1f)
        add(StatIds.Skills.FishingLootMultiplier, 1f)
        add(StatIds.Skills.CookingLootMultiplier, 1f)
        add(StatIds.Skills.HuntingLootMultiplier, 1f)
        add(StatIds.Skills.ChartingLootMultiplier, 1f)
        add(StatIds.Skills.ArchaeologyLootMultiplier, 1f)
        add(StatIds.Skills.OneHandedLootMultiplier, 1f)
        add(StatIds.Skills.TwoHandedLootMultiplier, 1f)
        add(StatIds.Skills.RangedLootMultiplier, 1f)
        add(StatIds.Skills.DefenseLootMultiplier, 1f)

        add(StatIds.Skills.CombatXp, 1f)
        add(StatIds.Skills.GatherXp, 1f)
        add(StatIds.Skills.CraftXp, 1f)

        add(StatIds.Skills.CombatYield, 1f)
        add(StatIds.Skills.GatherYield, 1f)
        add(StatIds.Skills.CraftYield, 1f)

        add(StatIds.Skills.GatherSpeed, 1f)
        add(StatIds.Skills.CraftSpeed, 1f)
    }
}