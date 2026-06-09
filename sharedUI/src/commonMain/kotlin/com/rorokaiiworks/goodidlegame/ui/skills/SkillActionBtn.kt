package com.rorokaiiworks.goodidlegame.ui.skills

 import androidx.compose.foundation.layout.*
 import androidx.compose.foundation.shape.RoundedCornerShape
 import androidx.compose.material3.MaterialTheme
 import androidx.compose.material3.Text
 import androidx.compose.runtime.Composable
 import androidx.compose.ui.Alignment
 import androidx.compose.ui.Modifier
 import androidx.compose.ui.text.font.FontWeight
 import androidx.compose.ui.text.style.TextAlign
 import androidx.compose.ui.text.style.TextOverflow
 import androidx.compose.ui.unit.dp
 import com.rorokaiiworks.goodidlegame.core.data.DataTable
 import com.rorokaiiworks.goodidlegame.core.enemies.EnemyTemplate
 import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
 import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
 import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
 import com.rorokaiiworks.goodidlegame.core.tasks.TaskSystem
 import com.rorokaiiworks.goodidlegame.ui.commons.ActionButtonBase
 import com.rorokaiiworks.goodidlegame.ui.commons.AnimatedProgressIndicator
 import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
 import name.kropp.kotlinx.gettext.I18n
 import org.koin.compose.koinInject
 import org.koin.core.qualifier.named
 import kotlin.time.DurationUnit
 import kotlin.time.toDuration

@Composable
fun CombatSkillActionBtn(
    modifier: Modifier = Modifier,
    skillAction: SkillAction.CombatSkillAction,
    isSelected: Boolean = false,
    isRunning: Boolean = false,
    enemyTemplates: DataTable<EnemyTemplate> = koinInject(named<EnemyTemplate>()),
    onClick: (SkillAction) -> Unit,
) {
    SkillActionBtn(
        modifier = modifier,
        skillAction = skillAction,
        isSelected = isSelected,
        isRunning = isRunning,
        onClick = onClick,
    ) {
        val enemy = enemyTemplates.find(skillAction.enemyIds.first())
        Text(
            textAlign = TextAlign.Right,
            text = "Lv. ${enemy.level}",
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SkillActionBtn(
    modifier: Modifier = Modifier,
    skillAction: SkillAction,
    isSelected: Boolean = false,
    isRunning: Boolean = false,
    i18n: I18n = koinInject(),
    playerSkills: PlayerSkills = koinInject(),
    taskSystem: TaskSystem = koinInject(),
    onClick: (SkillAction) -> Unit,
    content: @Composable () -> Unit = {},
) {
    val currentLevel = playerSkills.skills[skillAction.skillId]?.level ?: 0
    val isLocked = currentLevel < skillAction.requiredLevel

    ActionButtonBase(
        isRemarkable = isRunning,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        isSelected = isSelected,
        onClick = { onClick(skillAction) },
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    GameImage(
                        modifier = Modifier.size(32.dp),
                        iconName = skillAction.getIconName()
                    )

                    if (skillAction.requiredLevel > 0) {
                        Text(
                            text = "Lv.${skillAction.requiredLevel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isLocked) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column {
                    Text(
                        text = i18n.tr(skillAction.name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    content()

                    Text(
                        text = "${Humanizer.abbreviation(skillAction.getXp)} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    if (skillAction.duration > 0) {
                        Text(
                            text = Humanizer.duration(
                                skillAction.duration.toDouble().toDuration(DurationUnit.SECONDS)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1
                        )
                    }
                }
            }

            if (taskSystem.skillActionIsRunning(skillAction = skillAction)) {
                val session = taskSystem.findSessionBySkillId(skillAction.skillId)

                AnimatedProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    targetValue = session?.progress ?: 0f,
                )
            }
        }
    }
}
