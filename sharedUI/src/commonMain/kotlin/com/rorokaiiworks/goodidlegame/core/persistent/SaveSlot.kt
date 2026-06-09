package com.rorokaiiworks.goodidlegame.core.persistent

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
data class SaveSlot(
    val slotId: String,
    val playerName: String,
    val playTime: Long = 0L,

    val archiveUuid: String? = null, // for cloud sync, taptap sdk
    val fileId: String? = null, // for cloud sync, taptap sdk

    val syncVersion: Int = 0,
    val lastModified: Instant? = null,
)

enum class SaveSlotStatue(val text: String) {
    LocalOnly("Local Only"),
    CloudOnly("Cloud Only"),
    Synced("Synced"),
    Conflict("Conflict"),
}
