package com.rorokaiiworks.goodidlegame.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.composables.icons.feather.Check
import com.composables.icons.feather.Feather
import com.rorokaiiworks.goodidlegame.core.achievements.Achievement
import com.rorokaiiworks.goodidlegame.core.achievements.AchievementSystem
import com.rorokaiiworks.goodidlegame.core.requirements.iconName
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.commons.RequirementEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AchievementSystemScreenViewModel : ViewModel(), KoinComponent {
    val achievementSystem: AchievementSystem by inject()
}


@Composable
fun AchievementSystemScreen(
    viewModel: AchievementSystemScreenViewModel = koinViewModel(),
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        viewModel.achievementSystem.allAchievements.forEach {
            AchievementItem(
                modifier = Modifier.fillMaxWidth(),
                achievement = it
            )
        }
    }
}


@Composable
private fun AchievementItem(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    achievement: Achievement
) {
    val isCompleted = achievement.conditions.all { it.isMet() }

    BaseCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                GameImage(
                    modifier = Modifier.size(60.dp),
                    iconName = achievement.conditions.first().iconName(),
                )

                if (isCompleted) {
                    Icon(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                shape = CircleShape
                            )
                            .padding(6.dp),
                        imageVector = Feather.Check,
                        contentDescription = i18n.tr("Finished"),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = i18n.tr(achievement.name),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                text = i18n.tr("Finished"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                achievement.conditions.forEach {
                    RequirementEntry(
                        modifier = Modifier.fillMaxWidth(),
                        requirement = it
                    )
                }
            }
        }
    }
}