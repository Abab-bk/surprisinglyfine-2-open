@file:OptIn(ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.core.persistent

import kotlin.time.ExperimentalTime


data class SaveComparisonResult(
    val synced: List<SaveSlot>,
    val localOnly: List<SaveSlot>,
    val cloudOnly: List<SaveSlot>,
    val conflicts: List<SaveConflict>
) {
    companion object {
        val EMPTY = SaveComparisonResult(
            synced = emptyList(),
            localOnly = emptyList(),
            cloudOnly = emptyList(),
            conflicts = emptyList()
        )
    }

    fun all(): List<SaveSlot> = synced + localOnly + cloudOnly + conflicts.flatMap { listOf(it.local, it.remote) }

    fun getStatus(slot: SaveSlot): SaveSlotStatue = when (slot) {
        in synced -> SaveSlotStatue.Synced
        in localOnly -> SaveSlotStatue.LocalOnly
        in conflicts.flatMap { listOf(it.local, it.remote) } -> SaveSlotStatue.Conflict
        else -> SaveSlotStatue.CloudOnly
    }
}

@OptIn(ExperimentalTime::class)
fun compareSaveSlots(
    locals: List<SaveSlot>,
    remotes: List<SaveSlot>,
): SaveComparisonResult {
    val localMap = locals.associateBy { it.slotId }
    val remoteMap = remotes.associateBy { it.slotId }

    val synced = mutableListOf<SaveSlot>()
    val localOnly = mutableListOf<SaveSlot>()
    val cloudOnly = mutableListOf<SaveSlot>()
    val conflicts = mutableListOf<SaveConflict>()

    for ((id, local) in localMap) {
        val remote = remoteMap[id]

        if (remote == null) {
            localOnly += local
            continue
        }

        if (local.syncVersion >= remote.syncVersion) {
            synced += local
            continue
        }

        conflicts += SaveConflict(
            slotId = id,
            local = local,
            remote = remote
        )
    }

    for ((id, remote) in remoteMap) {
        if (id !in localMap) {
            cloudOnly += remote
        }
    }

    return SaveComparisonResult(
        synced = synced,
        localOnly = localOnly,
        cloudOnly = cloudOnly,
        conflicts = conflicts
    )
}
