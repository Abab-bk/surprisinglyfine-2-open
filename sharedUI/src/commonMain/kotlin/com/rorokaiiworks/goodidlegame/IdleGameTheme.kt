package com.rorokaiiworks.goodidlegame

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.rorokaiiworks.goodidlegame.core.settings.Settings
import com.rorokaiiworks.goodidlegame.core.settings.ThemePreference
import com.rorokaiiworks.goodidlegame.core.settings.ThemeStyle

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IdleGameTheme(
    themePreference: ThemePreference = ThemePreference.System,
    themeStyle: ThemeStyle = ThemeStyle.TonalSpot,
    seedColor: Int = Settings.DEFAULT_SEED_COLOR,
    content: @Composable () -> Unit
) {

    val isDark = when (themePreference) {
        ThemePreference.System -> {
            isSystemInDarkTheme()
        }
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }

    val colorScheme = rememberDynamicColorScheme(
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = themeStyle.paletteStyle,
        seedColor = Color(seedColor),
        isDark = isDark
    )

    MaterialExpressiveTheme (
        colorScheme = colorScheme,
        content = content
    )
}
