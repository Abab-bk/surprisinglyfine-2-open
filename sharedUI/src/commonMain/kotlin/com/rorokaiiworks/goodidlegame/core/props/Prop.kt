package com.rorokaiiworks.goodidlegame.core.props

import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import kotlinx.serialization.Serializable

@Serializable
data class PropSave(
    val itemTemplateId: String,
    val itemData: Map<String, String>,
)

abstract class Prop(val template: ItemTemplate) {
    var isFinished: Boolean = false
        protected set

    var count: Int = 0
        protected set

    var started: Boolean = false

    abstract fun tick(currentMills: Long, actor: IActor)

    fun start(actor: IActor, currentMills: Long) {
        if (started) return
        started = true
        onStart(actor, currentMills)
    }

    fun end(actor: IActor) {
        if (!started) return
        started = false
        onEnd(actor)
    }

    protected abstract fun onStart(actor: IActor, currentMills: Long)
    protected abstract fun onEnd(actor: IActor)

    abstract fun trySave(currentMills: Long): PropSave
    abstract fun tryLoad(data: PropSave, currentMills: Long)

    abstract val propType: String
}