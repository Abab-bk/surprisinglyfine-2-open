package com.rorokaiiworks.goodidlegame.core.achievements

import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.requirements.Requirement
import kotlinx.serialization.Serializable

@Serializable
data class Achievement(
    override val id: String,
    val name: String,
    val conditions: List<Requirement>,
) : Template


