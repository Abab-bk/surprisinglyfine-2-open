package com.rorokaiiworks.goodidlegame.androidApp

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import com.rorokaiiworks.goodidlegame.*
import com.rorokaiiworks.goodidlegame.core.I18nLoader
import com.rorokaiiworks.goodidlegame.core.achievements.AchievementSystem
import com.rorokaiiworks.goodidlegame.core.achievements.IAchievementAdapter
import com.rorokaiiworks.goodidlegame.core.di.preStartModule
import com.rorokaiiworks.goodidlegame.core.settings.Language.Companion.readBytes
import com.rorokaiiworks.goodidlegame.core.settings.Settings
import com.taptap.sdk.core.TapTapRegion
import com.taptap.sdk.core.TapTapSdk
import com.taptap.sdk.core.TapTapSdkOptions
import com.taptap.sdk.initializer.api.model.ScreenOrientation
import kotlinx.coroutines.runBlocking
import name.kropp.kotlinx.gettext.Gettext
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class AppActivity : ComponentActivity() {
    val logger = Logger(
        config = loggerConfigInit(platformLogWriter()),
        tag = "GoodIdleGame"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        TapTapSdk.init(
            this,
            TapTapSdkOptions(
                clientId = "clientId",
                clientToken = "clientToken",
                region = TapTapRegion.CN,
                screenOrientation = ScreenOrientation.PORTRAIT,
                enableLog = true,
            )
        )

        val (initialSettings, koin) = boot(this, logger, this)
        CrashReporter.setup()

        enableEdgeToEdge()
        setContent {
            AndroidAppEntry()
        }
    }
}


private fun boot(
    activity: AppActivity,
    logger: Logger,
    context: Context
): Pair<Settings, Koin> {
    val initialSettings = try {
        loadAndroidInitialSettings(context)
    } catch (e: Exception) {
        logger.e(e) { "Failed to read initial settings, fallback to defaults" }
        Settings()
    }

    val i18n = runBlocking {
        Gettext.load(
            initialSettings.language.locale,
            byteArrayToOkioSource(initialSettings.language.readBytes())
        )
    }

    logger.i { "Boot" }

    val koin = ensureKoinStarted(activity, context, initialSettings, i18n)
    return Pair(initialSettings, koin)
}


private fun ensureKoinStarted(
    activity: AppActivity,
    context: Context,
    initialSettings: Settings,
    i18n: I18n,
): Koin {
    GlobalContext.getOrNull()?.let { return it }

    return startKoin {
        modules(
            module {
                single<I18n> { I18nLoader(initialSettings.language.locale, i18n) }
                single<IAchievementAdapter> { TapTapAchievementAdapter() }
                single { AchievementSystem(get()) }
            } + androidModules(activity, context) + preStartModule(LaunchSettings(), false)
        )
    }.koin
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDark
            isAppearanceLightNavigationBars = isDark
        }
    }
}
