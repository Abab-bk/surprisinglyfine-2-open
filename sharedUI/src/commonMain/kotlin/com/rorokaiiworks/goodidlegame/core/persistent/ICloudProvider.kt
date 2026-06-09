package com.rorokaiiworks.goodidlegame.core.persistent

import com.rorokaiiworks.goodidlegame.core.Resource
import okio.Path

interface ICloudProvider {
    fun isValidSlotId(slotId: String): Boolean {
        return slotId.isNotBlank() && slotId.all { it.code in 33..126 }
    }
    suspend fun createArchive(saveSlot: SaveSlot, gameSave: GameSave, filePath: Path): Resource<SaveSlot>
    suspend fun deleteArchive(saveSlot: SaveSlot): Resource<SaveSlot>
    suspend fun updateArchive(saveSlot: SaveSlot, gameSave: GameSave, filePath: Path): Resource<SaveSlot>
    suspend fun downloadArchive(saveSlot: SaveSlot): Resource<GameSave>
    suspend fun listArchives(): Resource<List<SaveSlot>>
}
