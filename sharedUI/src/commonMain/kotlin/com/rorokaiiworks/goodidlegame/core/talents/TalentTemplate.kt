package com.rorokaiiworks.goodidlegame.core.talents

import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.stats.ISourceName
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import kotlinx.serialization.Serializable

@Serializable
data class TalentTemplate(
    override val id: String,
    val name: String,
    val effects: List<TalentLevel>,
    val connections: List<String> = emptyList(),
    val initialUnlock: Boolean = false
) : Template, ISourceName {
    override val sourceName: String get() = name
}


@Serializable
data class TalentLevel(
    val cost: Long,
    val effects: List<StatModifier>,
)