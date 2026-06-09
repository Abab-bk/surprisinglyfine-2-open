package com.rorokaiiworks.goodidlegame.core.props

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.actors.IActor
import kotlinx.serialization.Serializable

@Serializable
data class PropsContainerSave(
    val propSlots: List<PropSlotSave>,
    val propSlotsAutoEquip: Map<String, String>, // propSlotId -> itemId
    val propsAutoEquipEnabled: Boolean = false
)


class PropsContainer(
    val propSlots: List<PropSlot>
) {
    var propsAutoEquipEnabled: Boolean by mutableStateOf(false)
    val propSlotsAutoEquip = mutableStateMapOf<String, String>().apply {
        putAll(propSlots.associate { it.id to "" })
    }

    fun trySave(currentMills: Long): PropsContainerSave {
        return PropsContainerSave(
            propSlots = propSlots.map {
                PropSlotSave(
                    id = it.id,
                    propSave = it.item?.trySave(currentMills = currentMills),
                    propType = it.item?.propType ?: "",
                )
            },
            propSlotsAutoEquip = propSlotsAutoEquip,
            propsAutoEquipEnabled = propsAutoEquipEnabled,
        )
    }

    fun tryLoad(data: PropsContainerSave, currentMills: Long) {
        for (propSlotSave in data.propSlots) {
            val propSlot = propSlots.find { it.id == propSlotSave.id } ?: continue
            propSlot.load(propSlotSave, currentMills)
        }

        propSlotsAutoEquip.putAll(data.propSlotsAutoEquip)
        propsAutoEquipEnabled = data.propsAutoEquipEnabled
    }

    fun canAddItem(item: Prop): Boolean {
        for (propSlot in propSlots) {
            if (!propSlot.canAddItem(item)) continue
            return true
        }
        return false
    }

    fun stop(actor: IActor) {
        propSlots.forEach {
            it.item?.end(actor)
        }
    }

    fun start(actor: IActor, currentMills: Long) {
        propSlots.forEach {
            it.item?.start(actor, currentMills)
        }
    }

    fun addItem(prop: Prop, propSlot: PropSlot): Boolean {
        if (!propSlot.canAddItem(prop)) return false
        propSlot.addItem(item = prop)
        return true
    }

    fun tick(currentMills: Long, actor: IActor) {
        propSlots.forEach {
            val prop = it.item ?: return@forEach

            if (!prop.started) prop.start(actor, currentMills)

            prop.tick(currentMills, actor)
            if (prop.isFinished) {
                it.clearItem()
            }
        }
    }
}