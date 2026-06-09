package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun SkillActionsListPanel(
    modifier: Modifier = Modifier,
    cachedSkillData: CachedSkillData,
    uiState: SkillUiState,
    viewModel: SkillScreenViewModel,
    i18n: I18n = koinInject(),
    onClick: (SkillAction) -> Unit,
) {
    val actions = cachedSkillData.actions
    val categories = cachedSkillData.categories
    val subCategories = cachedSkillData.subCategories

    val filteredActions by remember(
        cachedSkillData,
        uiState.selectedCategory,
        uiState.selectedSubCategory,
    ) {
        derivedStateOf {
            actions.filter { action ->
                (action.category == uiState.selectedCategory) && (action.subCategory == uiState.selectedSubCategory)
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4,
        ) {
            for (category in categories) {
                CategoryBtn(
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                    category = i18n.tr(category),
                    isSelected = category == uiState.selectedCategory,
                    onClick = { viewModel.onCategorySelected(category) },
                )
            }
        }

        BaseCard {
            CardTitle(title = i18n.tr("Actions"))

            if (subCategories.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(
                        top = 8.dp,
                        bottom = 8.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (subCategory in subCategories) {
                        SubCategoryBtn(
                            subCategory = i18n.tr(subCategory),
                            isSelected = subCategory == uiState.selectedSubCategory,
                            onClick = { viewModel.onSubCategorySelected(subCategory) },
                        )
                    }
                }
            }

            FlowRow(
                maxItemsInEachRow = 3,
                modifier = Modifier.widthIn(min = 300.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredActions.forEach { action ->
                    val isSelected = action == uiState.selectedAction
                    val isRunning = viewModel.taskSystem.skillActionIsRunning(action)

                    when (action) {
                        is SkillAction.CombatSkillAction -> {
                            CombatSkillActionBtn(
                                modifier = Modifier.height(100.dp).weight(1f),
                                skillAction = action,
                                isSelected = isSelected,
                                isRunning = isRunning,
                                onClick = {
                                    onClick(action)
                                },
                            )
                        }
                        else -> {
                            SkillActionBtn(
                                modifier = Modifier.height(100.dp).weight(1f),
                                skillAction = action,
                                isSelected = isSelected,
                                isRunning = isRunning,
                                onClick = {
                                    onClick(action)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
