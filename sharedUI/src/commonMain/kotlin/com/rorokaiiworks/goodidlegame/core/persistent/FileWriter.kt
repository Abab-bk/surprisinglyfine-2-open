package com.rorokaiiworks.goodidlegame.core.persistent

import io.github.xxfast.kstore.KStore
import kotlinx.serialization.Serializable

abstract class FileWriter<T> {
    abstract suspend fun write(save: T?)
    abstract suspend fun read(): T?

    class KStoreFileWriter<T : @Serializable Any>(
        private val kstore: KStore<T>
    ) : FileWriter<T>() {
        override suspend fun write(save: T?) {
            kstore.set(save)
        }

        override suspend fun read(): T? {
            return kstore.get()
        }
    }

    class FakeFileWriter : FileWriter<GameSave>() {
        private var savedGameSave: GameSave? = null

        override suspend fun write(save: GameSave?) {
            savedGameSave = save
        }

        override suspend fun read(): GameSave? {
            return savedGameSave
        }
    }
}