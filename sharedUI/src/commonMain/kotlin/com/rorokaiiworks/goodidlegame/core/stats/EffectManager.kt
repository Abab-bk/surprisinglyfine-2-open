package com.rorokaiiworks.goodidlegame.core.stats

import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType.*

class EffectManager(private val actor: IActor) {
    val effects = mutableListOf<Effect>()

    fun addEffect(effect: Effect) {
        when (effect.stackPolicy) {
            EffectStackPolicy.None -> {
                applyEffect(effect, 1f)
            }

            EffectStackPolicy.DurationStack -> {
                val existing = effects.firstOrNull { it.id == effect.id }

                if (existing != null && existing.duration != null && effect.duration != null) {
                    existing.remaining += effect.duration ?: 0f
                    return
                }

                applyEffect(effect, 1f)
            }
        }
    }

    fun removeEffect(effect: Effect) = applyEffect(effect, -1f)


    fun tick(delta: Float) {
        val expired = mutableListOf<Effect>()

        effects.forEach { effect ->
            if (effect.duration != null) {
                effect.remaining -= delta
                if (effect.remaining <= 0f) {
                    expired.add(effect)
                }
            }
        }

        expired.forEach { removeEffect(it) }
    }


    private fun applyEffect(effect: Effect, sign: Float) {
        if (sign > 0) effects.add(effect) else effects.remove(effect)

        effect.modifiers.forEach { mod ->
            val stat = actor.stats[mod.statId] ?: return@forEach
            val value = mod.value * sign

            when (mod.type) {
                Flat -> stat.addFlatModifier(value, mod.channel)
                Percent -> stat.addPercentModifier(value, mod.channel)
            }
        }
    }

    fun hasEffect(effect: Effect) = effects.contains(effect)

    fun removeAllEffectsBySource(source: Any): Boolean {
        val expired = effects.filter { it.source == source }.toList()
        if (expired.isEmpty()) return false
        expired.forEach { removeEffect(it) }
        return true
    }
}
