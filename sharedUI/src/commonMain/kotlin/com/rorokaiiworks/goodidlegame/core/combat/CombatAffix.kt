package com.rorokaiiworks.goodidlegame.core.combat

import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper

data class CombatAffix(
    val name: String,
    val modifiers: List<StatModifier>
) {
    companion object {
        val Frenzy = CombatAffix(
            name = i18nWrapper("Frenzy"),
            modifiers = listOf(
                StatModifier(StatIds.Actor.AttackSpeed, 0.5f, StatModifierType.Percent)
            )
        )

        val Tanky = CombatAffix(
            name = i18nWrapper("Tanky"),
            modifiers = listOf(
                StatModifier(StatIds.Actor.MaxHealth, 1.0f, StatModifierType.Percent),
                StatModifier(StatIds.Actor.Armor, 50f, StatModifierType.Flat)
            )
        )

        val Deadly = CombatAffix(
            name = i18nWrapper("Deadly"),
            modifiers = listOf(
                StatModifier(StatIds.Actor.DamageMultiplier, 0.3f, StatModifierType.Percent)
            )
        )

        val Swift = CombatAffix(
            name = i18nWrapper("Swift"),
            modifiers = listOf(
                StatModifier(StatIds.Actor.DodgeChanceBonus, 0.2f, StatModifierType.Flat)
            )
        )

        val Precise = CombatAffix(
            name = i18nWrapper("Precise"),
            modifiers = listOf(
                StatModifier(StatIds.Actor.HitChanceBonus, 0.3f, StatModifierType.Flat)
            )
        )

        val Hardened = CombatAffix(
            name = i18nWrapper("Hardened"),
            modifiers = listOf(
                StatModifier(StatIds.Actor.SlashResistance, 0.2f, StatModifierType.Flat),
                StatModifier(StatIds.Actor.PunctureResistance, 0.2f, StatModifierType.Flat),
                StatModifier(StatIds.Actor.ImpactResistance, 0.2f, StatModifierType.Flat)
            )
        )

        val pool = listOf(Frenzy, Tanky, Deadly, Swift, Precise, Hardened)

        fun getRandom(count: Int): List<CombatAffix> {
            return pool.shuffled().take(count)
        }
    }
}
