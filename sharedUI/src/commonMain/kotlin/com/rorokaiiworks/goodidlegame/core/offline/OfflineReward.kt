package com.rorokaiiworks.goodidlegame.core.offline

import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.Stats
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSession
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.City
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi


fun tickCityOfflineReward(city: City, delta: Float, leftTime: Instant, currentTime: Instant) {
    city.tick(delta = delta, currentMills = currentTime.toEpochMilliseconds())
}


@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
fun calculateOfflineReward(
    leftTime: Instant,
    currentTime: Instant,
    stats: Stats,
    taskSession: TaskSession?,
    passiveSession: List<TaskSession>? = null
): OfflineReward {
    val rawOfflineTime = currentTime - leftTime
    val cappedOfflineTime = if (rawOfflineTime > 24.hours) 24.hours else rawOfflineTime

    val multiplier = stats[StatIds.Player.OfflineRewardMultiplier]?.value ?: 1.0f
    val totalSimulatedSeconds = cappedOfflineTime.inWholeSeconds * multiplier

    val endMillis = currentTime.toEpochMilliseconds()
    val simulatedDurationMillis = (totalSimulatedSeconds * 1000.0).toLong().coerceAtLeast(0L)
    val startMillis = endMillis - simulatedDurationMillis

    taskSession?.start(startMillis)
    passiveSession?.forEach { it.start(startMillis) }

    // Single-shot settlement. TaskSession/CombatSession are responsible for handling time jumps internally.
    taskSession?.tick(endMillis)
    passiveSession?.forEach { it.tick(endMillis) }

    val allEntries = mutableListOf<OfflineRewardEntry>()

    taskSession?.let {
        allEntries.add(it.toRewardEntry())
    }

    passiveSession?.forEach { session ->
        allEntries.add(session.toRewardEntry())
    }

    return OfflineReward(
        offlineTime = cappedOfflineTime,
        entries = allEntries.mergeById()
    )
}

private fun TaskSession.toRewardEntry() = OfflineRewardEntry(
    skillId = this.task.action.skillId,
    items = this.lootResult.items,
    skillXp = this.totalLootXp
)

private fun List<OfflineRewardEntry>.mergeById(): List<OfflineRewardEntry> {
    return this.groupBy { it.skillId }.map { (skillId, entries) ->
        OfflineRewardEntry(
            skillId = skillId,
            items = entries.flatMap { it.items },
            skillXp = entries.sumOf { it.skillXp }
        )
    }
}

data class OfflineReward(
    val offlineTime: Duration,
    val entries: List<OfflineRewardEntry>,
)

data class OfflineRewardEntry(
    val skillId: String,
    val items: List<Item>,
    val skillXp: Long = 0L,
)
