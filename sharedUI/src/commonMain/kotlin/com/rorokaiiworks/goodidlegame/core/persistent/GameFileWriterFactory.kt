package com.rorokaiiworks.goodidlegame.core.persistent

class GameFileWriterFactory(
    private val gameSaveFileWriterFactory: (slotId: String) -> FileWriter<GameSave>,
) {
    fun create(slotId: String): FileWriter<GameSave> {
        return gameSaveFileWriterFactory(slotId)
    }
}