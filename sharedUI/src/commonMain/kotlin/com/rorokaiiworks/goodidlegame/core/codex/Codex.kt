package com.rorokaiiworks.goodidlegame.core.codex

import androidx.compose.runtime.mutableStateMapOf
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.mastery.MasteryLevel
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import kotlin.collections.forEach

class Codex(
    val eventBus: EventBus,
    val itemTemplates: DataTable<ItemTemplate>,
    val masteryLevel: MasteryLevel
) : KoinComponent, IPersistable {
    val progress = mutableStateMapOf<String, CodexItemProgress>()

    val totalMaxProgress: Int

    private var masteredItems = mutableSetOf<String>()

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        itemTemplates.data.forEach {
            if (it.value.tier <= -2) return@forEach
            progress[it.key] = CodexItemProgress(it.key, 0)
        }
        totalMaxProgress = itemTemplates.data.filter { it.value.tier > -2 }.size

        scope.launch {
            eventBus.events.collect {
                if (it is IEvent.ItemCollected) {
                    addItem(it.itemId, it.count)
                }
            }
        }
    }

    fun addItem(itemId: String, count: Long) {
        if (masteredItems.contains(itemId)) return

        val currentEntry = progress[itemId] ?: CodexItemProgress(itemId, 0)
        val newProgressValue = (currentEntry.progress + count).coerceAtMost(CodexItemProgress.MAX_PROGRESS)

        if (newProgressValue == CodexItemProgress.MAX_PROGRESS) {
            masteredItems.add(itemId)
            masteryLevel.addXp(100)
            scope.launch {
                eventBus.emit(IEvent.ItemMastered(itemTemplates.find(itemId)))
            }
        }

        progress[itemId] = currentEntry.copy(progress = newProgressValue.toInt())
    }

    fun tryGetProgress(itemId: String): CodexItemProgress {
        if (progress.containsKey(itemId)) {
            return progress[itemId]!!
        }
        progress[itemId] = CodexItemProgress(itemId, 0)
        return progress[itemId]!!
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.codexSaveData = CodexSaveData(progress.toMap())
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        val savedData = gameSave.codexSaveData ?: return
        progress.clear()
        progress.putAll(savedData.progress)

        masteredItems = savedData.progress.values
            .filter { it.progress >= CodexItemProgress.MAX_PROGRESS }
            .map { it.itemId }
            .toMutableSet()
    }
}


@Serializable
data class CodexSaveData(
    val progress: Map<String, CodexItemProgress>,
)


@Serializable
data class CodexItemProgress(
    val itemId: String,
    val progress: Int,
) {
    companion object {
        const val MAX_PROGRESS = 100L
    }
}