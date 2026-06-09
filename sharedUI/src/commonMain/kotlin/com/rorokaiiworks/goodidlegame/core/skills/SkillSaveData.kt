package com.rorokaiiworks.goodidlegame.core.skills

import com.rorokaiiworks.goodidlegame.core.props.PropsContainerSave
import kotlinx.serialization.Serializable

@Serializable
data class SkillSaveData(
    val level: Int,
    val currentXp: Long,
    val totalXp: Long,
    val propsContainerSave: PropsContainerSave?,
)