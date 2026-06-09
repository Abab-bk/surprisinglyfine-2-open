package com.rorokaiiworks.goodidlegame.core.stats

class Stats(val sets: List<StatSet>) {
    operator fun get(statId: String): Stat? {
        return sets.flatMap { it.stats.values }.firstOrNull { it.id == statId }
    }
}