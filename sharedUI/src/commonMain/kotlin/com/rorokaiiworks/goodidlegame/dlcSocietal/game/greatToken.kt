package com.rorokaiiworks.goodidlegame.dlcSocietal.game

import androidx.compose.runtime.mutableStateMapOf
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.FormulaTag
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

@Serializable
data class ItemProgress(
    val current: Long,
    val target: Long
)


@Serializable
data class GreatTokenSave(
    val progress: Map<String, ItemProgress>
)

class GreatToken : KoinComponent {
    private val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    val itemProgresses = mutableStateMapOf<String, ItemProgress>()

    init {
        itemTemplates.all().filter {
            FormulaTag.CityItem in it.tags
        }.forEach {
            itemProgresses[it.id] = ItemProgress(
                current = 0,
                target = CityFormulas.calculateItemGreatTokenTargetProgress(it)
            )
        }
    }

    fun clear() {
        itemProgresses.clear()
    }

    fun addItem(item: Item): Boolean {
        val current = itemProgresses[item.template.id]

        current?.let {
            if (it.current >= it.target) return false
            itemProgresses[item.template.id] = current.copy(current = current.current + item.count)
            return true
        }

        return false
    }

    fun totalProgress(): ItemProgress {
        var totalCurrent = 0L
        var totalTarget = 0L

        itemProgresses.values.forEach { progress ->
            totalCurrent += progress.current
            totalTarget += progress.target
        }

        return ItemProgress(
            current = totalCurrent,
            target = totalTarget,
        )
    }

    fun toSave(): GreatTokenSave {
        return GreatTokenSave(
            progress = itemProgresses
        )
    }

    fun fromSave(save: GreatTokenSave) {
        save.progress.forEach { (id, progress) ->
            itemProgresses[id] = progress
        }
    }
}
