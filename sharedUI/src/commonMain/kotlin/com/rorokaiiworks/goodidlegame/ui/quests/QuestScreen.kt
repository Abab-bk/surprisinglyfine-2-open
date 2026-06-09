package com.rorokaiiworks.goodidlegame.ui.quests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.window.core.layout.WindowSizeClass
import com.rorokaiiworks.goodidlegame.core.Constants
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.quests.Quest
import com.rorokaiiworks.goodidlegame.core.quests.QuestSystem
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import com.rorokaiiworks.goodidlegame.ui.isWideScreen
import com.rorokaiiworks.goodidlegame.ui.skills.CategoryBtn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

enum class QuestCategory(
    val label: String
) {
    Daily(i18nWrapper("Daily Quests")),
}

data class QuestScreenUiState(
    val selectedCategory: QuestCategory = QuestCategory.Daily,
    val selectedQuest: Quest
)

class QuestScreenViewModel : ViewModel(), KoinComponent {
    val questSystem: QuestSystem by inject()
    val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())

    private val _uiState = MutableStateFlow(QuestScreenUiState(selectedQuest = questSystem.dailyQuests.first()))
    val uiState = _uiState.asStateFlow()

    fun selectCategory(category: QuestCategory) {
        _uiState.update {
            it.copy(selectedCategory = category)
        }
    }

    fun selectQuest(quest: Quest) {
        _uiState.update {
            it.copy(selectedQuest = quest)
        }
    }

    fun claimQuest() {
        questSystem.claimQuest(uiState.value.selectedQuest)
    }
}

@Composable
fun QuestScreen(
    viewModel: QuestScreenViewModel = koinViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
) {
    val uiState by viewModel.uiState.collectAsState()

    if (isWideScreen(windowSizeClass)) {
        QuestScreenWideScreen(uiState = uiState, viewModel = viewModel)
    } else {
        QuestScreenMobile(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun QuestScreenMobile(
    uiState: QuestScreenUiState,
    viewModel: QuestScreenViewModel
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuestPanel(quest = uiState.selectedQuest, showTip = uiState.selectedCategory == QuestCategory.Daily)
        RequirementsPanel(uiState.selectedQuest.requirements)
        QuestRewardsPanel(quest = uiState.selectedQuest, onClaim = viewModel::claimQuest)
        AllQuestsPanel(
            dailyQuests = viewModel.questSystem.dailyQuests,
            uiState = uiState,
            viewModel = viewModel,
            enabledScroll = false
        )
    }
}

@Composable
fun QuestScreenWideScreen(
    uiState: QuestScreenUiState,
    viewModel: QuestScreenViewModel
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuestPanel(quest = uiState.selectedQuest, showTip = uiState.selectedCategory == QuestCategory.Daily)
            RequirementsPanel(uiState.selectedQuest.requirements)
            QuestRewardsPanel(quest = uiState.selectedQuest, onClaim = viewModel::claimQuest)
        }

        AllQuestsPanel(
            modifier = Modifier
                .weight(1f),
            uiState = uiState,
            viewModel = viewModel,
            dailyQuests = viewModel.questSystem.dailyQuests,
        )
    }
}


@Composable
private fun AllQuestsPanel(
    modifier: Modifier = Modifier,
    uiState: QuestScreenUiState,
    viewModel: QuestScreenViewModel,
    i18n: I18n = koinInject(),
    dailyQuests: List<Quest>,
    enabledScroll: Boolean = true
) {
    @Composable {
        Badge(
            modifier = Modifier.size(12.dp),
            containerColor = Constants.NoticeColor
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryBtn(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                category = i18n.tr(QuestCategory.Daily.label),
                badge = {
//                    if (viewModel.questSystem.dailyQuestsShouldNotice()) {
//                        badge()
//                    }
                },
                isSelected = uiState.selectedCategory == QuestCategory.Daily,
                onClick = { viewModel.selectCategory(QuestCategory.Daily) }
            )
        }

        when (uiState.selectedCategory) {
            QuestCategory.Daily -> {
                QuestsPanel(
                    modifier = if (enabledScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                    quests = dailyQuests,
                    uiState = uiState,
                    onClick = viewModel::selectQuest
                )
            }
        }
    }
}


@Composable
private fun QuestsPanel(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    uiState: QuestScreenUiState,
    quests: List<Quest>,
    onClick: (Quest) -> Unit,
) {
    BaseCard(modifier = modifier) {
        CardTitle(title = i18n.tr(uiState.selectedCategory.label))

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (quest in quests) {
                QuestEntry(
                    quest,
                    isSelected = uiState.selectedQuest == quest,
                    onClick = { onClick(quest) }
                )
            }
        }
    }
}
