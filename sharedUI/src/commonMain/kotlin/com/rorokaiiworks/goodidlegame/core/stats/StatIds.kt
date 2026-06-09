package com.rorokaiiworks.goodidlegame.core.stats

import com.rorokaiiworks.goodidlegame.ui.i18nWrapperContext

object StatIds {
    object Actor {
        val Actor = i18nWrapperContext("stat_id", "actor")
        val MaxHealth = i18nWrapperContext("stat_id", "actor_maxHealth")
        val Health = i18nWrapperContext("stat_id", "actor_health")
        val AttackSpeed = i18nWrapperContext("stat_id", "actor_attackSpeed")

        val Armor = i18nWrapperContext("stat_id", "actor_armor") // Armor 直接减少受到的伤害

        val DamageMultiplier = i18nWrapperContext("stat_id", "actor_damageMultiplier")
        val DamageTakenMultiplier = i18nWrapperContext("stat_id", "actor_damageTakenMultiplier")

        val SlashDamage = i18nWrapperContext("stat_id", "actor_slashDamage")
        val PunctureDamage = i18nWrapperContext("stat_id", "actor_punctureDamage")
        val ImpactDamage = i18nWrapperContext("stat_id", "actor_impactDamage")
        val HitChanceBonus = i18nWrapperContext("stat_id", "actor_hitChanceBonus")
        val DodgeChanceBonus = i18nWrapperContext("stat_id", "actor_dodgeChanceBonus")

        val SlashResistance = i18nWrapperContext("stat_id", "actor_slashResistance")
        val PunctureResistance = i18nWrapperContext("stat_id", "actor_punctureResistance")
        val ImpactResistance = i18nWrapperContext("stat_id", "actor_impactResistance")

        val all = listOf(
            MaxHealth,
            AttackSpeed,
            Armor,
            DamageMultiplier,
            DamageTakenMultiplier,
            SlashDamage,
            PunctureDamage,
            ImpactDamage,
            HitChanceBonus,
            DodgeChanceBonus,
            SlashResistance,
            PunctureResistance,
            ImpactResistance
        )
    }

    object Player {
        val Player = i18nWrapperContext("stat_id", "player")

        val FoodEffect = i18nWrapperContext("stat_id", "player_foodEffect")

        val OfflineRewardMultiplier = i18nWrapperContext("stat_id", "player_offlineRewardMultiplier")

        val all = listOf(
            Player,
            FoodEffect,
            OfflineRewardMultiplier,
        )
    }

    object Skills {
        val Skills = i18nWrapperContext("stat_id", "skills")

        val WoodcuttingEfficiency = i18nWrapperContext("stat_id", "skill_woodcutting_efficiency")
        val MiningEfficiency = i18nWrapperContext("stat_id", "skill_mining_efficiency")
        val SmeltingEfficiency = i18nWrapperContext("stat_id", "skill_smelting_efficiency")
        val SmithingEfficiency = i18nWrapperContext("stat_id", "skill_smithing_efficiency")
        val FarmingEfficiency = i18nWrapperContext("stat_id", "skill_farming_efficiency")
        val AlchemyEfficiency = i18nWrapperContext("stat_id", "skill_alchemy_efficiency")
        val FishingEfficiency = i18nWrapperContext("stat_id", "skill_fishing_efficiency")
        val CookingEfficiency = i18nWrapperContext("stat_id", "skill_cooking_efficiency")
        val HuntingEfficiency = i18nWrapperContext("stat_id", "skill_hunting_efficiency")
        val ChartingEfficiency = i18nWrapperContext("stat_id", "skill_charting_efficiency")
        val ArchaeologyEfficiency = i18nWrapperContext("stat_id", "skill_archaeology_efficiency")
        val OneHandedEfficiency = i18nWrapperContext("stat_id", "skill_oneHanded_efficiency")
        val TwoHandedEfficiency = i18nWrapperContext("stat_id", "skill_twoHanded_efficiency")
        val RangedEfficiency = i18nWrapperContext("stat_id", "skill_ranged_efficiency")
        val DefenseEfficiency = i18nWrapperContext("stat_id", "skill_defense_efficiency")


        val WoodcuttingSpeed = i18nWrapperContext("stat_id", "skill_woodcutting_speed")
        val MiningSpeed = i18nWrapperContext("stat_id", "skill_mining_speed")
        val SmeltingSpeed = i18nWrapperContext("stat_id", "skill_smelting_speed")
        val SmithingSpeed = i18nWrapperContext("stat_id", "skill_smithing_speed")
        val FarmingSpeed = i18nWrapperContext("stat_id", "skill_farming_speed")
        val AlchemySpeed = i18nWrapperContext("stat_id", "skill_alchemy_speed")
        val FishingSpeed = i18nWrapperContext("stat_id", "skill_fishing_speed")
        val CookingSpeed = i18nWrapperContext("stat_id", "skill_cooking_speed")
        val HuntingSpeed = i18nWrapperContext("stat_id", "skill_hunting_speed")
        val ChartingSpeed = i18nWrapperContext("stat_id", "skill_charting_speed")
        val ArchaeologySpeed = i18nWrapperContext("stat_id", "skill_archaeology_speed")
        val OneHandedSpeed = i18nWrapperContext("stat_id", "skill_oneHanded_speed")
        val TwoHandedSpeed = i18nWrapperContext("stat_id", "skill_twoHanded_speed")
        val RangedSpeed = i18nWrapperContext("stat_id", "skill_ranged_speed")
        val DefenseSpeed = i18nWrapperContext("stat_id", "skill_defense_speed")



        val WoodcuttingXpMultiplier = i18nWrapperContext("stat_id", "skill_woodcutting_XpMultiplier")
        val MiningXpMultiplier = i18nWrapperContext("stat_id", "skill_mining_XpMultiplier")
        val SmeltingXpMultiplier = i18nWrapperContext("stat_id", "skill_smelting_XpMultiplier")
        val SmithingXpMultiplier = i18nWrapperContext("stat_id", "skill_smithing_XpMultiplier")
        val FarmingXpMultiplier = i18nWrapperContext("stat_id", "skill_farming_XpMultiplier")
        val AlchemyXpMultiplier = i18nWrapperContext("stat_id", "skill_alchemy_XpMultiplier")
        val FishingXpMultiplier = i18nWrapperContext("stat_id", "skill_fishing_XpMultiplier")
        val CookingXpMultiplier = i18nWrapperContext("stat_id", "skill_cooking_XpMultiplier")
        val HuntingXpMultiplier = i18nWrapperContext("stat_id", "skill_hunting_XpMultiplier")
        val ChartingXpMultiplier = i18nWrapperContext("stat_id", "skill_charting_XpMultiplier")
        val ArchaeologyXpMultiplier = i18nWrapperContext("stat_id", "skill_archaeology_XpMultiplier")
        val OneHandedXpMultiplier = i18nWrapperContext("stat_id", "skill_oneHanded_XpMultiplier")
        val TwoHandedXpMultiplier = i18nWrapperContext("stat_id", "skill_twoHanded_XpMultiplier")
        val RangedXpMultiplier = i18nWrapperContext("stat_id", "skill_ranged_XpMultiplier")
        val DefenseXpMultiplier = i18nWrapperContext("stat_id", "skill_defense_XpMultiplier")


        val WoodcuttingLootMultiplier = i18nWrapperContext("stat_id", "skill_woodcutting_lootMultiplier")
        val MiningLootMultiplier = i18nWrapperContext("stat_id", "skill_mining_lootMultiplier")
        val SmeltingLootMultiplier = i18nWrapperContext("stat_id", "skill_smelting_lootMultiplier")
        val SmithingLootMultiplier = i18nWrapperContext("stat_id", "skill_smithing_lootMultiplier")
        val FarmingLootMultiplier = i18nWrapperContext("stat_id", "skill_farming_lootMultiplier")
        val AlchemyLootMultiplier = i18nWrapperContext("stat_id", "skill_alchemy_lootMultiplier")
        val FishingLootMultiplier = i18nWrapperContext("stat_id", "skill_fishing_lootMultiplier")
        val CookingLootMultiplier = i18nWrapperContext("stat_id", "skill_cooking_lootMultiplier")
        val HuntingLootMultiplier = i18nWrapperContext("stat_id", "skill_hunting_lootMultiplier")
        val ChartingLootMultiplier = i18nWrapperContext("stat_id", "skill_charting_lootMultiplier")
        val ArchaeologyLootMultiplier = i18nWrapperContext("stat_id", "skill_archaeology_lootMultiplier")
        val OneHandedLootMultiplier = i18nWrapperContext("stat_id", "skill_oneHanded_lootMultiplier")
        val TwoHandedLootMultiplier = i18nWrapperContext("stat_id", "skill_twoHanded_lootMultiplier")
        val RangedLootMultiplier = i18nWrapperContext("stat_id", "skill_ranged_lootMultiplier")
        val DefenseLootMultiplier = i18nWrapperContext("stat_id", "skill_defense_lootMultiplier")


        val WoodcuttingLucky = i18nWrapperContext("stat_id", "skill_woodcutting_lucky")
        val MiningLucky = i18nWrapperContext("stat_id", "skill_mining_lucky")
        val SmeltingLucky = i18nWrapperContext("stat_id", "skill_smelting_lucky")
        val SmithingLucky = i18nWrapperContext("stat_id", "skill_smithing_lucky")
        val FarmingLucky = i18nWrapperContext("stat_id", "skill_farming_lucky")
        val AlchemyLucky = i18nWrapperContext("stat_id", "skill_alchemy_lucky")
        val FishingLucky = i18nWrapperContext("stat_id", "skill_fishing_lucky")
        val CookingLucky = i18nWrapperContext("stat_id", "skill_cooking_lucky")
        val HuntingLucky = i18nWrapperContext("stat_id", "skill_hunting_lucky")
        val ChartingLucky = i18nWrapperContext("stat_id", "skill_charting_lucky")
        val ArchaeologyLucky = i18nWrapperContext("stat_id", "skill_archaeology_lucky")

        // Skill 被分为 3 类：Combat, Gather, Craft
        val CombatXp = i18nWrapperContext("stat_id", "skill_combatXp")
        val CombatYield = i18nWrapperContext("stat_id", "skill_combatYield")

        val GatherXp = i18nWrapperContext("stat_id", "skill_gatherXp")
        val GatherYield = i18nWrapperContext("stat_id", "skill_gatherYield")
        val GatherSpeed = i18nWrapperContext("stat_id", "skill_gatherSpeed")

        val CraftXp = i18nWrapperContext("stat_id", "skill_craftXp")
        val CraftYield = i18nWrapperContext("stat_id", "skill_craftYield")
        val CraftSpeed = i18nWrapperContext("stat_id", "skill_craftSpeed")

        val all = listOf(
            // Efficiency
            WoodcuttingEfficiency,
            MiningEfficiency,
            SmeltingEfficiency,
            SmithingEfficiency,
            FarmingEfficiency,
            AlchemyEfficiency,
            FishingEfficiency,
            CookingEfficiency,
            HuntingEfficiency,
            ChartingEfficiency,
            ArchaeologyEfficiency,
            OneHandedEfficiency,
            TwoHandedEfficiency,
            RangedEfficiency,
            DefenseEfficiency,

            // Speed
            WoodcuttingSpeed,
            MiningSpeed,
            SmeltingSpeed,
            SmithingSpeed,
            FarmingSpeed,
            AlchemySpeed,
            FishingSpeed,
            CookingSpeed,
            HuntingSpeed,
            ChartingSpeed,
            ArchaeologySpeed,
            OneHandedSpeed,
            TwoHandedSpeed,
            RangedSpeed,
            DefenseSpeed,

            // XP Multipliers
            WoodcuttingXpMultiplier,
            MiningXpMultiplier,
            SmeltingXpMultiplier,
            SmithingXpMultiplier,
            FarmingXpMultiplier,
            AlchemyXpMultiplier,
            FishingXpMultiplier,
            CookingXpMultiplier,
            HuntingXpMultiplier,
            ChartingXpMultiplier,
            ArchaeologyXpMultiplier,
            OneHandedXpMultiplier,
            TwoHandedXpMultiplier,
            RangedXpMultiplier,
            DefenseXpMultiplier,

            // Loot Multipliers
            WoodcuttingLootMultiplier,
            MiningLootMultiplier,
            SmeltingLootMultiplier,
            SmithingLootMultiplier,
            FarmingLootMultiplier,
            AlchemyLootMultiplier,
            FishingLootMultiplier,
            CookingLootMultiplier,
            HuntingLootMultiplier,
            ChartingLootMultiplier,
            ArchaeologyLootMultiplier,
            OneHandedLootMultiplier,
            TwoHandedLootMultiplier,
            RangedLootMultiplier,
            DefenseLootMultiplier,

            // Categorized Stats
            CombatXp,
            CombatYield,
            GatherXp,
            GatherYield,
            GatherSpeed,
            CraftXp,
            CraftYield,
            CraftSpeed
        )
    }
}
