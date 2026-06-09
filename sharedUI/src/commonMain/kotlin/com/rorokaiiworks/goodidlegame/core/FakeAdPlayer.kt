package com.rorokaiiworks.goodidlegame.core

import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class FakeAdPlayer : IAdPlayer() {
    override suspend fun onPlayAd(): Resource<Unit> {
        delay(0.5.seconds)
        return Resource.Success(Unit)
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        TODO("Not yet implemented")
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        TODO("Not yet implemented")
    }
}