@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.core.persistent

import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okio.Path
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class)
class FakeCloudProvider(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val config: Config = Config()
) : ICloudProvider, KoinComponent {
    private val timeProvider: ITimeProvider by inject()

    data class Config(
        val artificialDelayMs: Long = 1,
        val randomFailureRate: Double = 0.0,
        val deterministic: Boolean = true
    )

    private val random = if (config.deterministic) Random(42) else Random.Default

    private data class FakeMetadata(
        val name: String,
        val summary: String,
        val playtime: Int,
        val syncVersion: Int,
    )

    private data class FakeArchive(
        val uuid: String,
        var fileId: String,
        var metadata: FakeMetadata,
        var data: GameSave,
        var lastModified: Instant
    )

    private val archives = mutableMapOf<String, FakeArchive>()

    private suspend fun <T> simulate(block: () -> T): Resource<T> {
        delay(config.artificialDelayMs)

        if (random.nextDouble() < config.randomFailureRate) {
            return Resource.Error(500, "Fake TapTap Cloud: random failure")
        }

        return try {
            Resource.Success(block())
        } catch (e: Throwable) {
            Resource.Error(500, e.message ?: "unknown error")
        }
    }


    private fun SaveSlot.toFakeMetadata(): FakeMetadata = FakeMetadata(
        name = slotId,
        summary = playerName,
        playtime = playTime.toInt(),
        syncVersion = syncVersion,
    )


    override suspend fun createArchive(
        saveSlot: SaveSlot,
        gameSave: GameSave,
        filePath: Path
    ): Resource<SaveSlot> = withContext(dispatcher) {
        simulate {
            val uuid = Uuid.random().toString()
            val fileId = "file_${uuid.take(8)}"

            archives[uuid] = FakeArchive(
                uuid = uuid,
                fileId = fileId,
                metadata = saveSlot.toFakeMetadata(),
                data = gameSave,
                lastModified = saveSlot.lastModified ?: timeProvider.now()
            )

            saveSlot.copy(
                archiveUuid = uuid,
                fileId = fileId
            )
        }
    }


    override suspend fun deleteArchive(saveSlot: SaveSlot): Resource<SaveSlot> = withContext(dispatcher) {
        val uuid = saveSlot.archiveUuid
            ?: return@withContext Resource.Error(400, "archiveUuid is null")

        simulate {
            archives.remove(uuid) ?: error("archive not found")

            saveSlot.copy(
                archiveUuid = null,
                fileId = null
            )
        }
    }


    override suspend fun updateArchive(
        saveSlot: SaveSlot,
        gameSave: GameSave,
        filePath: Path
    ): Resource<SaveSlot> = withContext(dispatcher) {
        val uuid = saveSlot.archiveUuid
            ?: return@withContext Resource.Error(400, "archiveUuid is null")

        simulate {
            val archive = archives[uuid] ?: error("archive not found")

            archive.data = gameSave
            archive.metadata = saveSlot.toFakeMetadata()
            archive.lastModified = saveSlot.lastModified ?: timeProvider.now()

            saveSlot.copy(fileId = archive.fileId)
        }
    }


    override suspend fun downloadArchive(saveSlot: SaveSlot): Resource<GameSave> = withContext(dispatcher) {
        val uuid = saveSlot.archiveUuid
            ?: return@withContext Resource.Error(400, "archiveUuid is null")

        simulate {
            archives[uuid]?.data ?: error("archive not found")
        }
    }


    override suspend fun listArchives(): Resource<List<SaveSlot>> = withContext(dispatcher) {
        simulate {
            archives.values.map {
                SaveSlot(
                    slotId = it.metadata.name,
                    playerName = it.metadata.summary,
                    playTime = it.metadata.playtime.toLong(),
                    archiveUuid = it.uuid,
                    fileId = it.fileId,
                    lastModified = it.lastModified,
                    syncVersion = it.metadata.syncVersion,
                )
            }
        }
    }
}