package com.rorokaiiworks.goodidlegame.core.persistent

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class SaveConflict(
    val slotId: String,
    val local: SaveSlot,
    val remote: SaveSlot,
) {
    val localTime: Instant? get() = local.lastModified
    val remoteTime: Instant? get() = remote.lastModified
}