package com.rorokaiiworks.goodidlegame.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsSaver : KoinComponent {
    private val dataStore: DataStore<Preferences> by inject()

    companion object {
        fun toSettings(preferences: Preferences): Settings {
            return Settings(
                isFinishedInitialSetup = preferences[Settings.FINISHED_INITIAL_SETUP] ?: false,
                language = Language.findByCode(preferences[Settings.LANGUAGE_KEY] ?: Language.getDefaultLanguage().code),
                themePreference = ThemePreference.findThemePreference(
                    preferences[Settings.THEME_PREFERENCE_KEY]
                        ?: ThemePreference.System.key
                ),
                uiScale = preferences[Settings.UI_SCALE_KEY] ?: 1f,
                seedColor = preferences[Settings.SEED_COLOR_KEY] ?: Settings.DEFAULT_SEED_COLOR,
                soundVolume = preferences[Settings.SOUND_VOLUME_KEY] ?: 1f,
                musicVolume = preferences[Settings.MUSIC_VOLUME_KEY] ?: 1f,
                themeStyle = ThemeStyle.entries.firstOrNull { it.id == preferences[Settings.THEME_STYLE_KEY] } ?: ThemeStyle.TonalSpot,
                windowSetting = WindowSetting.find(
                    preferences[Settings.WINDOW_KEY] ?: WindowSetting.Windowed.key
                ),
                acceptedPrivacyPolicy = preferences[Settings.ACCEPTED_PRIVACY_POLICY_KEY] ?: false,
            )
        }
    }

    suspend fun updateData(newSettings: Settings) {
        dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[Settings.FINISHED_INITIAL_SETUP] = newSettings.isFinishedInitialSetup
                preferences[Settings.THEME_PREFERENCE_KEY] = newSettings.themePreference.key
                preferences[Settings.LANGUAGE_KEY] = newSettings.language.code
                preferences[Settings.SEED_COLOR_KEY] = newSettings.seedColor
                preferences[Settings.UI_SCALE_KEY] = newSettings.uiScale
                preferences[Settings.SOUND_VOLUME_KEY] = newSettings.soundVolume
                preferences[Settings.MUSIC_VOLUME_KEY] = newSettings.musicVolume
                preferences[Settings.WINDOW_KEY] = newSettings.windowSetting.key
                preferences[Settings.THEME_STYLE_KEY] = newSettings.themeStyle.id
                preferences[Settings.ACCEPTED_PRIVACY_POLICY_KEY] = newSettings.acceptedPrivacyPolicy
            }
        }
    }

    val settings: StateFlow<Settings> =
        dataStore.data
            .map { toSettings(it) }
            .stateIn(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = Settings()
            )
}
