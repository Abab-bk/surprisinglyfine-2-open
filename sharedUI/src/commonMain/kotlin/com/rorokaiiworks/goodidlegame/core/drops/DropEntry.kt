package com.rorokaiiworks.goodidlegame.core.drops

import kotlinx.serialization.Serializable

@Serializable
data class DropEntry(
    val itemId: String,
    val chance: Int,
    val min: Int = 1,
    val max: Int = 1,
) {
    val isAlways get() = chance == 0
    val inCommon get() = chance in 41..60
    val inUncommon get() = chance in 20..40
    val inRare get() = chance in 6..21
    val inLegendary get() = chance in 1..5

    val label by lazy {
        when {
            isAlways -> "100%"
            else -> "${chance}%"
        }
    }
}