package com.rorokaiiworks.goodidlegame.core.props

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType

class PotionProp(
    template: ItemTemplate,
) : Prop(template) {

    var duration by mutableStateOf(template.potionDuration * 1000L)
    var timeLeft by mutableStateOf(duration)

    private var elapsed = 0L
    private var lastTickTime = 0L

    override fun tick(currentMills: Long, actor: IActor) {
        if (isFinished) return

        if (lastTickTime == 0L) {
            lastTickTime = currentMills
            return
        }

        val delta = currentMills - lastTickTime
        lastTickTime = currentMills

        elapsed += delta
        timeLeft = duration - elapsed

        if (elapsed >= duration) {
            onEnd(actor)
            isFinished = true
        }
    }

    override fun onStart(actor: IActor, currentMills: Long) {
        if (template.modifiers == null) return

        if (template.potionDuration <= 0) {
            for (mod in template.modifiers!!) {
                val stat = actor.stats[mod.statId] ?: continue
                when (mod.type) {
                    StatModifierType.Percent -> stat.executeFlatChange(stat.maxValue * mod.value)
                    StatModifierType.Flat -> stat.executeFlatChange(mod.value)
                }
            }
            isFinished = true
            return
        }

        lastTickTime = currentMills

        val effect = Effect(
            id = template.id,
            source = template.id,
            sourceName = template,
            modifiers = template.modifiers!!
        )
        actor.effectManager.addEffect(effect)
    }

    override fun onEnd(actor: IActor) {
        actor.effectManager.removeAllEffectsBySource(template.id)
    }

    override fun trySave(currentMills: Long): PropSave {
        return PropSave(
            itemTemplateId = template.id,
            itemData = mapOf(
                "elapsed" to elapsed.toString(),
                "lastTickTime" to lastTickTime.toString()
            )
        )
    }

    override fun tryLoad(data: PropSave, currentMills: Long) {
        elapsed = data.itemData["elapsed"]?.toLongOrNull() ?: 0L

        lastTickTime = currentMills

        timeLeft = duration - elapsed

        if (elapsed >= duration) {
            isFinished = true
        }
    }

    override val propType: String
        get() = PROP_TYPE

    companion object {
        const val PROP_TYPE = "POTION_PROP_TYPE"
    }
}