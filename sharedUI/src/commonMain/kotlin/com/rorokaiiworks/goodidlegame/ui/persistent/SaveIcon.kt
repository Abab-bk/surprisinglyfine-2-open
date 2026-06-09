package com.rorokaiiworks.goodidlegame.ui.persistent

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.feather.CheckCircle
import com.composables.icons.feather.Circle
import com.composables.icons.feather.Feather
import com.rorokaiiworks.goodidlegame.core.persistent.SaveSystem
import com.rorokaiiworks.goodidlegame.ui.commons.TopBarLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun SaveIcon(
    modifier: Modifier = Modifier,
    saveSystem: SaveSystem = koinInject(),
    i18n: I18n = koinInject(),
    minDisplayMillis: Long = 500L
) {
    val coroutineScope = rememberCoroutineScope()
    val externalSaving by saveSystem.isSaving.collectAsState()
    var minLock by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        saveSystem.isSaving.collect { isSaving ->
            if (isSaving) {
                minLock = true
                delay(minDisplayMillis)
                minLock = false
            }
        }
    }

    val uiSaving = externalSaving || minLock

    TopBarLabel(
        modifier = modifier.clickable(
            enabled = !externalSaving
        ) {
            coroutineScope.launch { saveSystem.save() }
        }
    ) {
        AnimatedContent(
            targetState = uiSaving,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400)))
                    .togetherWith(fadeOut(animationSpec = tween(400)))
                    .using(SizeTransform(clip = false))
            },
            label = "SaveStatusAnimation"
        ) { isSaving ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (isSaving) Feather.Circle else Feather.CheckCircle,
                    contentDescription = null,
                )

                Text(
                    text = if (isSaving) i18n.tr("Saving...") else i18n.tr("Saved"),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    }
}