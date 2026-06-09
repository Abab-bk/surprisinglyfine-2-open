package com.rorokaiiworks.goodidlegame.core.recipes

import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    override val id: String,
    val product: ItemEntry,
    val required: ItemEntry
) : Template
