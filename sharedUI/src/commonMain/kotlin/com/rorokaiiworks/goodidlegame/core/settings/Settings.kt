package com.rorokaiiworks.goodidlegame.core.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.materialkolor.PaletteStyle
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import com.rorokaiiworks.goodidlegame.ui.i18nWrapperContext
import kotlinx.serialization.Serializable

enum class ThemePreference(val text: String, val key: String) {
    System(i18nWrapperContext("Theme Preference", "System"), "system"),
    Light(i18nWrapperContext("Theme Preference", "Light"), "light"),
    Dark(i18nWrapperContext("Theme Preference", "Dark"), "dark"),;

    companion object {
        const val I18N_CONTEXT = "Theme Preference"

        @Composable
        fun ThemePreference.isDark(): Boolean {
            return when (this) {
                System -> isSystemInDarkTheme()
                Light -> false
                Dark -> true
            }
        }

        fun findThemePreference(id: String): ThemePreference {
            when (id) {
                "system" -> return System
                "light" -> return Light
                "dark" -> return Dark
            }

            return System
        }
    }
}

enum class ThemeStyle(val id: String, val text: String, val paletteStyle: PaletteStyle) {
    TonalSpot("tonal_spot", i18nWrapperContext("Theme Style", "TonalSpot"), PaletteStyle.TonalSpot),
    Neutral("neutral", i18nWrapperContext("Theme Style", "Neutral"), PaletteStyle.Neutral),
    Vibrant("vibrant", i18nWrapperContext("Theme Style", "Vibrant"), PaletteStyle.Vibrant),
    Expressive("expressive", i18nWrapperContext("Theme Style", "Expressive"), PaletteStyle.Expressive),
    Rainbow("rainbow", i18nWrapperContext("Theme Style", "Rainbow"), PaletteStyle.Rainbow),
    FruitSalad("fruit_salad", i18nWrapperContext("Theme Style", "FruitSalad"), PaletteStyle.FruitSalad),
    Monochrome("monochrome", i18nWrapperContext("Theme Style", "Monochrome"), PaletteStyle.Monochrome),
    Fidelity("fidelity", i18nWrapperContext("Theme Style", "Fidelity"), PaletteStyle.Fidelity),
    Content("content", i18nWrapperContext("Theme Style", "Content"), PaletteStyle.Content);

    companion object {
        const val I18N_CONTEXT = "Theme Style"
    }
}

enum class WindowSetting(val key: String, val text: String) {
    Windowed("windowed", "Windowed"),
    Fullscreen("fullscreen", "Fullscreen"),;

    companion object {
        fun find(key: String): WindowSetting {
            return when (key) {
                Fullscreen.key -> Fullscreen
                Windowed.key -> Windowed
                else -> Windowed
            }
        }
    }
}

@Serializable
data class Settings(
    val isFinishedInitialSetup: Boolean = false,
    val isCloudSaveEnabled: Boolean = false,
    val themePreference: ThemePreference = ThemePreference.System,
    val themeStyle: ThemeStyle = ThemeStyle.TonalSpot,
    val language: Language = Language.getDefaultLanguage(),
    val uiScale: Float = 1f,
    val seedColor: Int = DEFAULT_SEED_COLOR,
    val soundVolume: Float = 1f,
    val musicVolume: Float = 1f,
    val windowSetting: WindowSetting = WindowSetting.Windowed,
    val acceptedPrivacyPolicy: Boolean = false,
) {
    companion object {
        val ACCEPTED_PRIVACY_POLICY_KEY = booleanPreferencesKey("accepted_privacy_policy")
        val THEME_PREFERENCE_KEY = stringPreferencesKey("theme_preference")
        val CLOUD_SAVE_ENABLED = booleanPreferencesKey("cloud_save_enabled")
        val FINISHED_INITIAL_SETUP = booleanPreferencesKey("finished_setup")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val SEED_COLOR_KEY = intPreferencesKey("seed_color")
        val UI_SCALE_KEY = floatPreferencesKey("ui_scale")
        val SOUND_VOLUME_KEY = floatPreferencesKey("sound_volume")
        val MUSIC_VOLUME_KEY = floatPreferencesKey("music_volume")
        val WINDOW_KEY = stringPreferencesKey(WindowSetting.Windowed.key)
        val THEME_STYLE_KEY = stringPreferencesKey("theme_style")

        const val DEFAULT_SEED_COLOR = 0xFF3AD2A4.toInt()
    }
}