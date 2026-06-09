package com.rorokaiiworks.goodidlegame.core.stats

import kotlinx.serialization.Serializable

@Serializable
enum class EffectStackPolicy {
    None,
    DurationStack
}