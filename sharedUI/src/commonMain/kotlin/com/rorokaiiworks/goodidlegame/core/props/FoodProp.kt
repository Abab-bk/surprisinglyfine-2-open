package com.rorokaiiworks.goodidlegame.core.props

import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.stats.StatIds

class FoodProp(
    template: ItemTemplate,
) : Prop(template) {
    override fun tick(currentMills: Long, actor: IActor) {
        isFinished = count <= 0

        if (actor.healthRatio >= 0.2) return

        if (template.modifiers == null) return

        for (modifier in template.modifiers) {
            actor.executeModifier(
                modifier = modifier,
                multiplier = actor.stats[StatIds.Player.FoodEffect]?.value ?: 1f
            )
        }
        count -= 1
        isFinished = count <= 0
    }

    override fun onStart(actor: IActor, currentMills: Long) {

    }

    override fun onEnd(actor: IActor) {
    }

    override fun trySave(currentMills: Long): PropSave {
        return PropSave(
            itemTemplateId = template.id,
            itemData = mapOf(
                "count" to count.toString(),
            )
        )
    }

    override fun tryLoad(data: PropSave, currentMills: Long) {
        count = data.itemData["count"]?.toIntOrNull() ?: 0
    }

    override val propType: String get() = PROP_TYPE

    companion object {
        const val PROP_TYPE = "FOOD_PROP_TYPE"
    }
}