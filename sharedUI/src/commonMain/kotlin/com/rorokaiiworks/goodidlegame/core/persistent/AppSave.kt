package com.rorokaiiworks.goodidlegame.core.persistent

import kotlinx.serialization.Serializable

@Serializable
data class AppSave(
    val slots: MutableList<SaveSlot> = mutableListOf()
) {
    companion object {
        const val APP_SAVE_VERSION = 0
    }
}