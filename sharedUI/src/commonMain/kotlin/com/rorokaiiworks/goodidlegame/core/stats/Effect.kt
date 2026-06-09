package com.rorokaiiworks.goodidlegame.core.stats

import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
data class Effect(
    val id: String,
    val source: Any,
    val sourceName: ISourceName,
    val modifiers: List<StatModifier>,
    val stackPolicy: EffectStackPolicy = EffectStackPolicy.None,
    var duration: Float? = null,
    var remaining: Float = duration ?: 0f,
)


interface ISourceName {
    val sourceName: String
}