package com.rorokaiiworks.goodidlegame.core.actors

import com.rorokaiiworks.goodidlegame.core.combat.Attack
import com.rorokaiiworks.goodidlegame.core.combat.ProcessedAttack
import com.rorokaiiworks.goodidlegame.core.stats.EffectManager
import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType.*
import com.rorokaiiworks.goodidlegame.core.stats.Stats

interface IActor {
    val id: String
    val name: String
    val stats: Stats
    val effectManager: EffectManager
    val iconName: String

    val healthRatio: Float
        get() = stats[StatIds.Actor.Health]!!.value / stats[StatIds.Actor.MaxHealth]!!.value

    val isDead: Boolean
        get() = stats[StatIds.Actor.Health]!!.value <= 0

    fun die() {

    }

    fun takeDamage(processedAttack: ProcessedAttack) {
        stats[StatIds.Actor.Health]!!.executeFlatChange(-processedAttack.totalDamage)
    }

    fun executeModifiers(modifiers: List<StatModifier>) {
        for (modifier in modifiers) {
            executeModifier(modifier)
        }
    }

    fun executeModifier(modifier: StatModifier, multiplier: Float = 1f) {
        when(modifier.type) {
            Flat -> stats[modifier.statId]!!.executeFlatChange(modifier.value * multiplier)
            Percent -> stats[modifier.statId]!!.executePercentChange(modifier.value * multiplier)
        }
    }
    
    fun getAttack() : Attack {
        return Attack(
            attacker = this,
            slashDamage = stats[StatIds.Actor.SlashDamage]!!.value,
            punctureDamage = stats[StatIds.Actor.PunctureDamage]!!.value,
            impactDamage = stats[StatIds.Actor.ImpactDamage]!!.value,
        )
    }

    fun revive() {
        stats[StatIds.Actor.Health]!!.setBaseValueToMax()
    }
}