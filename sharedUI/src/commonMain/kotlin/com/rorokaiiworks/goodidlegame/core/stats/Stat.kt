package com.rorokaiiworks.goodidlegame.core.stats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

class Stat(
    val id: String,
    baseValue: Float,
    channelCount: Int,
    minValue: Float = Float.NEGATIVE_INFINITY,
    maxValue: Float = Float.POSITIVE_INFINITY,
) {
    var baseValue = baseValue
        set(v) {
            field = v
            calculateValue()
//            onBaseValueChanged?.invoke(baseValue)
        }

    var minValue = minValue
        set(v) {
            field = v
            enforceBounds()
            calculateValue()
//            onMinValueChanged?.invoke(minValue)
        }

    var maxValue = maxValue
        set(v) {
            field = v
            enforceBounds()
            calculateValue()
//            onMaxValueChanged?.invoke(maxValue)
        }

    var value: Float by mutableFloatStateOf(baseValue)
        private set

    val ratio: Float get() = value / maxValue

    var totalModifier: Float = 0f
        private set

    var overflow: Float = 0f
        private set

//    var onBaseValueChanged: ((baseValue: Float) -> Unit)? = null
//    var onMaxValueChanged: ((maxValue: Float) -> Unit)? = null
//    var onMinValueChanged: ((minValue: Float) -> Unit)? = null
    var onValueChanged: ((value: Float) -> Unit)? = null

    val channels = List(channelCount) { ChannelData() }

    init {
        calculateValue()
    }

    fun addFlatModifier(modifier: Float, channelIndex: Int) {
        checkChannelIndex(channelIndex)
        channels[channelIndex].flatModifier += modifier
        calculateValue()
    }

    fun addPercentModifier(modifier: Float, channelIndex: Int) {
        checkChannelIndex(channelIndex)
        channels[channelIndex].percentModifier += modifier
        calculateValue()
    }

    fun executeFlatChange(change: Float) {
        baseValue += change
        baseValue = baseValue.coerceIn(minValue, maxValue)
        calculateValue()
    }

    fun executePercentChange(percent: Float) {
        baseValue *= (1 + percent)
        baseValue = baseValue.coerceIn(minValue, maxValue)
        calculateValue()
    }

    fun resetModifiers() {
        channels.forEach {
            it.flatModifier = 0f
            it.percentModifier = 0f
        }
        calculateValue()
    }

    fun setBaseValueToMax() {
        baseValue = maxValue
    }


    private fun calculateValue() {
        var evaluatedValue = baseValue

        channels.forEach { channel ->
            evaluatedValue = (evaluatedValue + channel.flatModifier) * (1 + channel.percentModifier)
        }

        overflow = when {
            evaluatedValue > maxValue -> evaluatedValue - maxValue
            evaluatedValue < minValue -> evaluatedValue - minValue
            else -> 0f
        }

        value = evaluatedValue.coerceIn(minValue, maxValue)
        totalModifier = value - baseValue
        onValueChanged?.invoke(value)
    }

    private fun enforceBounds() {
        if (minValue > maxValue) {
            val temp = minValue
            minValue = maxValue
            maxValue = temp
        }
    }

    private fun checkChannelIndex(channelIndex: Int) {
        check(channelIndex in channels.indices)
    }
}