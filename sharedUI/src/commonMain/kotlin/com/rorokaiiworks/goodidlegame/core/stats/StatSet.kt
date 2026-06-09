package com.rorokaiiworks.goodidlegame.core.stats

import com.rorokaiiworks.goodidlegame.core.Constants

abstract class StatSet(
    val id: String,
) {
    val stats = mutableMapOf<String, Stat>()

    protected fun addStat(stat: Stat) {
        stats[stat.id] = stat
    }

    operator fun get(statId: String): Stat? {
        return stats[statId]
    }

    protected fun add(statId: String, defaultValue: Float) {
        addStat(
            Stat(
                id = statId,
                baseValue = defaultValue,
                channelCount = Constants.ChannelsCount,
            ))
    }
}