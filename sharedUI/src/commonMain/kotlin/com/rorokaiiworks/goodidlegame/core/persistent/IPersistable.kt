package com.rorokaiiworks.goodidlegame.core.persistent

interface IPersistable {
    fun doSave(gameSave: GameSave, currentMills: Long)
    fun doLoad(gameSave: GameSave, currentMills: Long)
}