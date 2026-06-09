package com.rorokaiiworks.goodidlegame.core.community

import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import org.koin.core.component.KoinComponent

class Community : IPersistable, KoinComponent {
    val enchantingTable = EnchantingTable()
    val altar = Altar()
    val square = Square()

    fun tick(nowMills: Long) {
        enchantingTable.tick(nowMills)
        square.tick(nowMills)
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.communitySaveData = CommunitySaveData(
            enchantingTable = enchantingTable.toSaveData(),
            square = square.toSaveData(),
            altar = altar.toSaveData()
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        val saveData = gameSave.communitySaveData ?: return
        square.fromSaveData(saveData.square)
        altar.fromSaveData(saveData.altar)
        enchantingTable.fromSaveData(saveData.enchantingTable)
    }
}
