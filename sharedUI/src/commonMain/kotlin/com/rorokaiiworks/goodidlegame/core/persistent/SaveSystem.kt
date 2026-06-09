@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.core.persistent

import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.extensions.truncateToSeconds
import com.rorokaiiworks.goodidlegame.core.offline.OfflineReward
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.platformListFiles
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

class SaveSystem(
    private val appFileWriter: FileWriter<AppSave>,
    private val gameFileWriterFactory: GameFileWriterFactory,
    private val saveFolder: Path,
) : KoinComponent {
    private val logger: Logger by inject { parametersOf("SaveSystem") }
    private val timeProvider: ITimeProvider by inject()
    private val settingsSaver: SettingsSaver by inject()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    var cachedAppSave: AppSave = AppSave()
    private var activeSession: ActiveSession? = null

    private data class ActiveSession(
        val slotId: String,
        val saver: GameSaver
    )

    suspend fun syncMetadata() {
        cachedAppSave = appFileWriter.read() ?: AppSave()

        val discoveredSlots = discoverLocalFiles()
        val currentSlots = cachedAppSave.slots

        val newSlots = discoveredSlots.filter { new -> currentSlots.none { it.slotId == new.slotId } }
        if (newSlots.isNotEmpty()) {
            updateLocalIndex { it.copy(slots = (currentSlots + newSlots).toMutableList()) }
        }
    }

    suspend fun loadGame(slotId: String): OfflineReward? {
        val writer = gameFileWriterFactory.create(slotId)
        val saver = GameSaver(writer)

        activeSession = ActiveSession(slotId, saver)
        return saver.load(timeProvider.nowMillis())
    }

    suspend fun save() {
        if (_isSaving.value) {
            logger.i { "save() called while saving" }
            return
        }

        if (activeSession == null) {
            logger.i { "save() called while no active session, instead of fake save" }
            _isSaving.value = true
            delay(1000)
            _isSaving.value = false
            return
        }

        val session = activeSession ?: return

        _isSaving.value = true
        try {
            val gameSave = session.saver.save()
            val now = gameSave.time

            updateSlotInfo(session.slotId) { slot ->
                slot.copy(
                    lastModified = now,
                    playerName = gameSave.playerName,
                    playTime = gameSave.gameState.playTime,
                    syncVersion = slot.syncVersion + 1
                )
            }
            syncMetadata()
        } catch (e: Exception) {
            logger.e(e) { "Save failed" }
        } finally {
            _isSaving.value = false
        }
    }

    suspend fun createNewSlot(id: String, playerName: String) {
        runCatching {
            val now = timeProvider.now().truncateToSeconds()
            val initialSave = GameSave(playerName = playerName, time = now)
            val newSlot = SaveSlot(slotId = id, playerName = playerName, lastModified = now)

            gameFileWriterFactory.create(id).write(initialSave)

            updateLocalIndex { it.copy(slots = (it.slots + newSlot).toMutableList()) }

            syncMetadata()
        }.onFailure { e ->
            logger.e(e) { "Failed to create slot: $id" }
        }
    }

    suspend fun deleteSlot(targetSlot: SaveSlot) {
        runCatching {
            updateLocalIndex { appSave ->
                appSave.copy(slots = appSave.slots.filter { it.slotId != targetSlot.slotId }.toMutableList())
            }

            gameFileWriterFactory.create(targetSlot.slotId).write(null)

            syncMetadata()
        }.onFailure { e ->
            logger.e(e) { "Failed to delete slot: ${targetSlot.slotId}" }
        }
    }

    // --- Private Helpers ---

    private suspend fun discoverLocalFiles(): List<SaveSlot> {
        return platformListFiles(saveFolder, ".dat").mapNotNull { fileName ->
            val slotId = fileName.removeSuffix(".dat")
            // 优化：如果索引里已经有了，就不读文件了，IO 很贵
            if (cachedAppSave.slots.any { it.slotId == slotId }) return@mapNotNull null

            try {
                val gameData = gameFileWriterFactory.create(slotId).read() ?: return@mapNotNull null
                SaveSlot(
                    slotId = slotId,
                    playerName = gameData.playerName,
                    lastModified = gameData.time,
                    playTime = gameData.gameState.playTime
                )
            } catch (e: Exception) {
                logger.e(e) { "Failed to read slot file: $fileName" }
                null
            }
        }
    }

    private suspend fun updateSlotInfo(slotId: String, update: (SaveSlot) -> SaveSlot) {
        val currentSlots = cachedAppSave.slots
        val index = currentSlots.indexOfFirst { it.slotId == slotId }

        val newSlots = if (index != -1) {
            val updatedSlot = update(currentSlots[index])
            currentSlots.toMutableList().apply { set(index, updatedSlot) }
        } else {
            // 如果找不到，可能是一个新添加的slot
            val tempSlot = SaveSlot(slotId = slotId, playerName = "", lastModified = Instant.DISTANT_PAST)
            currentSlots.toMutableList().apply { add(update(tempSlot)) }
        }

        updateLocalIndex { it.copy(slots = newSlots) }
    }

    private suspend fun updateLocalIndex(transform: (AppSave) -> AppSave) {
        val newState = transform(cachedAppSave)
        cachedAppSave = newState
        appFileWriter.write(newState)
    }

    fun getFilePath(slotId: String): Path = "$saveFolder/$slotId.dat".toPath()
}