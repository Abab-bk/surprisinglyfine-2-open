package com.rorokaiiworks.goodidlegame.core.stats

import kotlinx.serialization.Serializable

@Serializable
enum class StatModifierType {
    Flat,
    Percent
}

@Serializable
data class StatModifier(
    val statId: String,
    val value: Float,
    val type: StatModifierType,
    val channel: Int = 0,
    var isAdditional: Boolean = false,
) {
    fun getOperationString(): String {
        val typeOperation = when (type) {
            StatModifierType.Flat -> ""
            StatModifierType.Percent -> "%"
        }
        return typeOperation
    }
}