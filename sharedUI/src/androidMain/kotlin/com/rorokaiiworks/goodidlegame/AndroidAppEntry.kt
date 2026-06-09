package com.rorokaiiworks.goodidlegame

import androidx.compose.runtime.*
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.ui.PrivacyPolicyScreen
import goodidlegame.sharedui.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AndroidAppEntry(
    settingsSaver: SettingsSaver = koinInject()
) {
    val settings by settingsSaver.settings.collectAsState()
    var privacyText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val bytes = Res.readBytes("files/privacyPolicy.txt")
            privacyText = bytes.decodeToString()
        } catch (e: Exception) {
            privacyText = "加载隐私政策内容失败"
        }
    }

    if (!settings.acceptedPrivacyPolicy) {
        PrivacyPolicyScreen(
            content = privacyText,
            onAccept = {
                if (privacyText.isNotEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        settingsSaver.updateData(settings.copy(acceptedPrivacyPolicy = true))
                    }
                }
            },
            onDecline = { exit(status = 0, info = {}, error = { throwable, string -> }) }
        )
        return
    }

    App()
}