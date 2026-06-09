package com.rorokaiiworks.goodidlegame.core.props

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

@Serializable
data class PropSlotSave(
    val id: String,
    val propType: String,
    val propSave: PropSave?,
)

class PropSlot(
    val id: String,
    val name: String,
    val acceptType: Set<ItemType>
): KoinComponent {
    private val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())

    var item by mutableStateOf<Prop?>(null)
        private set

    fun load(data: PropSlotSave, currentMills: Long) {
        val slotSave = data.propSave ?: return
        val itemTemplate = itemTemplates.find(slotSave.itemTemplateId)

        when (data.propType) {
            PotionProp.PROP_TYPE -> {
                item = PotionProp(itemTemplate)
            }
            else -> return
        }

        item?.tryLoad(slotSave, currentMills)
    }

    fun canAddItem(item: Item): Boolean {
        return item.template.type in acceptType
    }

    fun canAddItem(item: Prop): Boolean {
        if (item.template.type !in acceptType) return false
        val current = this.item ?: return true
        return current.template.id == item.template.id
    }

    fun addItem(item: Prop) {
        if (!canAddItem(item)) return

        if (this.item == null) {
            this.item = item
        }
    }

    fun clearItem() {
        item = null
    }
}