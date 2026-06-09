package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.feather.Feather
import com.composables.icons.feather.X
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.tasks.TaskRepeatConfig
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSession
import com.rorokaiiworks.goodidlegame.core.tasks.TaskStartResult
import com.rorokaiiworks.goodidlegame.core.tasks.getResultButtonColors
import com.rorokaiiworks.goodidlegame.ui.OnlyNumbersInputTransformation
import com.rorokaiiworks.goodidlegame.ui.commons.GameTextFieldThin
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun StartStopButton(
    modifier: Modifier = Modifier,
    selectedAction: SkillAction?,
    i18n: I18n = koinInject(),
    viewModel: SkillScreenViewModel,
    currentSession: TaskSession?,
    onStartTask: () -> Unit,
    isRunning: Boolean,
    taskRepeatConfig: TaskRepeatConfig,
    onTaskRepeatConfigChange: (TaskRepeatConfig) -> Unit,
) {
    val result = selectedAction?.canStart()
    val buttonText = if (isRunning) {
        i18n.tr("Stop")
    } else {
        i18n.tr(result?.buttonText ?: "Start")
    }

    if (result != TaskStartResult.Success && !isRunning) {
        Button(
            modifier = modifier.fillMaxWidth(),
            colors = result?.getResultButtonColors() ?: ButtonDefaults.buttonColors(),
            enabled = result?.enabled ?: true,
            shape = RoundedCornerShape(4.dp),
            onClick = { }
        ) {
            Text(buttonText)
        }
        return
    }

    if (result != null) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RepeatCountButtonGroup(
                modifier = Modifier.weight(0.4f),
                enabled = result.enabled && !isRunning,
                showCustomInput = !isRunning && taskRepeatConfig is TaskRepeatConfig.Custom,
                onTaskRepeatConfigChange = onTaskRepeatConfigChange,
                overrideCustomCount = currentSession?.repeatCountLeft,
                taskRepeatConfig = taskRepeatConfig,
            )

            StartStopMainButton(
                modifier = Modifier.weight(0.6f),
                text = buttonText,
                colors = result.getResultButtonColors(),
                enabled = result.enabled,
                isRunning = isRunning,
                selectedAction = selectedAction,
                viewModel = viewModel,
                onStartTask = onStartTask,
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun RepeatCountButtonGroup(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    overrideCustomCount: Int? = null,
    showCustomInput: Boolean,
    taskRepeatConfig: TaskRepeatConfig,
    i18n: I18n = koinInject(),
    onTaskRepeatConfigChange: (TaskRepeatConfig) -> Unit,
) {
    AnimatedContent(
        targetState = showCustomInput,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = {
            if (targetState) {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(300))
            }
        },
        label = "CustomInputAnimation"
    ) { isShowingInput ->
        if (isShowingInput) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onTaskRepeatConfigChange(TaskRepeatConfig.Infinite)
                    },
                ) {
                    Icon(
                        imageVector = Feather.X,
                        contentDescription = "Cancel",
                    )
                }

                if (taskRepeatConfig is TaskRepeatConfig.Custom) {
                    GameTextFieldThin(
                        modifier = Modifier
                            .weight(1f),
                        state = taskRepeatConfig.inputState,
                        inputTransformation = OnlyNumbersInputTransformation.then(InputTransformation.maxLength(9)),
                        placeholder = { Text(i18n.tr("Amount")) },
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (taskRepeatConfig == TaskRepeatConfig.Infinite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        contentColor = if (taskRepeatConfig == TaskRepeatConfig.Infinite) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    ),
                    onClick = {
                        onTaskRepeatConfigChange(TaskRepeatConfig.Infinite)
                    }
                ) {
                    Text(
                        text = "∞",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (taskRepeatConfig == TaskRepeatConfig.Once) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        contentColor = if (taskRepeatConfig == TaskRepeatConfig.Once) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    ),
                    onClick = {
                        onTaskRepeatConfigChange(TaskRepeatConfig.Once)
                    }
                ) {
                    Text(
                        text = "1",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (taskRepeatConfig is TaskRepeatConfig.Custom) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        contentColor = if (taskRepeatConfig is TaskRepeatConfig.Custom) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    ),
                    onClick = {
                        onTaskRepeatConfigChange(TaskRepeatConfig.Custom(
                            inputState = TextFieldState()
                        ))
                    }
                ) {
                    Text(
                        text = if (taskRepeatConfig is TaskRepeatConfig.Custom)
                            overrideCustomCount?.toString() ?: taskRepeatConfig.inputState.text.toString()
                        else "✎",

                        fontSize = if (taskRepeatConfig is TaskRepeatConfig.Custom) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun StartStopMainButton(
    modifier: Modifier = Modifier,
    text: String,
    colors: ButtonColors,
    enabled: Boolean,
    isRunning: Boolean,
    selectedAction: SkillAction?,
    viewModel: SkillScreenViewModel,
    onStartTask: () -> Unit,
) {
    Button(
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        shape = RoundedCornerShape(4.dp),
        onClick = {
            if (isRunning) {
                selectedAction?.let { viewModel.onStopTask(it.id) }
                return@Button
            }

            onStartTask()
        }
    ) {
        Text(text)
    }
}