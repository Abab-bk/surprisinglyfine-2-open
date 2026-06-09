package com.rorokaiiworks.goodidlegame.ui.onBoarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.settings.Settings
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.settings.SettingsEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    settingsSaver: SettingsSaver = koinInject(),
    onConfirm: (Settings) -> Unit,
) {
    val settings by settingsSaver.settings.collectAsState()

    BaseCard(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardTitle(title = i18n.tr("Onboarding"))

        SettingsEditor(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            settings = settings,
            onSettingsChange = { newSettings ->
                CoroutineScope(Dispatchers.IO).launch {
                    settingsSaver.updateData(newSettings)
                }
            },
            showConfirmButton = true,
            onConfirm = { onConfirm(settings) }
        )
    }
}