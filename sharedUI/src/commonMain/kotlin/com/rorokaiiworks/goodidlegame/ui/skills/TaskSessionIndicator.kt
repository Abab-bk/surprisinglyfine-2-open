package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSession
import com.rorokaiiworks.goodidlegame.ui.Navigator
import com.rorokaiiworks.goodidlegame.ui.commons.AnimatedCircularWavyProgressIndicator
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TaskSessionIndicator(
    taskSession: TaskSession,
    i18n: I18n = koinInject(),
    navigator: Navigator = koinInject()
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        onClick = {
            if (taskSession.destination != null) {
                navigator.navigateTo(taskSession.destination)
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = i18n.tr(taskSession.title),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    fontSize = 12.sp,
                    text = i18n.tr(taskSession.subTitle),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(4.dp)
            ) {
                AnimatedCircularWavyProgressIndicator(
                    modifier = Modifier.padding(4.dp),
                    targetValue = taskSession.progress,
                )
            }
        }
    }
}
