package com.rorokaiiworks.goodidlegame.core

import co.touchlab.kermit.Logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

class RandomSource(val tag: String) : KoinComponent {
    private val logger: Logger by inject { parametersOf("RandomSource") }

    var nextFloat: Float? = null

    private val random = Random.Default

    fun nextFloat(): Float {
        if (nextFloat != null) {
            logger.d("$tag: nextFloat() = $nextFloat")

            val result = nextFloat!!
            nextFloat = null
            return result
        }
        return random.nextFloat()
    }

    fun nextFloat(min: Float, max: Float): Float {
        require(max > min) { "Max must be greater than min (min: $min, max: $max)" }

        val range = max - min
        val result = min + (nextFloat() * range)

        return result
    }

    fun nextInt(min: Int, max: Int): Int {
        val result = if (nextFloat != null) {
            val offset = (nextFloat!! * (max - min)).toInt().coerceIn(0, (max - min).coerceAtLeast(1) - 1)
            nextFloat = null
            min + offset
        } else {
            random.nextInt(min, max)
        }

        logger.d("$tag: nextInt($min, $max) = $result")
        return result
    }

    companion object {
        const val TAG_STAR_DROP = "starDrop"
        const val TAG_PERK = "perk"
        const val TAG_ENCHANTING_TABLE = "enchantingTable"
    }
}