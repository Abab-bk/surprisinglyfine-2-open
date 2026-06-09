package com.rorokaiiworks.goodidlegame.core.quests

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.requirements.Requirement
import com.rorokaiiworks.goodidlegame.core.requirements.iconName
import com.rorokaiiworks.goodidlegame.core.rewards.Reward
import kotlinx.serialization.Serializable
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
data class Quest(
    override val id: String,
    val name: String? = null,
    val rewards: List<Reward>,
    val conditions: List<Requirement>,
    val requirements: List<Requirement> = emptyList(),
    var initialStatus: QuestStatus = QuestStatus.InProgress
) : Template, KoinComponent {
    private val i18n: I18n by inject()

    var status: QuestStatus by mutableStateOf(initialStatus)
        private set

    fun changeStatus(newStatus: QuestStatus) {
        status = newStatus
        initialStatus = newStatus
    }

    @Composable
    fun getIconName(
    ): String {
        val condition = conditions.firstOrNull() ?: return ""
        return condition.iconName()
    }

    fun tryGetName(): String {
        if (name != null) return i18n.tr(name)
        return conditions.firstOrNull()?.toText(i18n = i18n) ?: return ""
    }
    
    fun toSaveData(): QuestProgress = QuestProgress(
        status = status,
        conditionCounts = conditions.map { it.currentCount }
    )

    fun loadProgress(progress: QuestProgress) {
        changeStatus(progress.status)
        progress.conditionCounts.forEachIndexed { condIndex, count ->
            conditions.getOrNull(condIndex)?.currentCount = count
        }
    }
}