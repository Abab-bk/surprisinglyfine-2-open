@file:OptIn(ExperimentalComposeUiApi::class)

package com.rorokaiiworks.goodidlegame.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSegmented
import com.alorma.compose.settings.ui.SettingsSlider
import com.rorokaiiworks.goodidlegame.core.Constants
import com.rorokaiiworks.goodidlegame.core.settings.Language
import com.rorokaiiworks.goodidlegame.core.settings.Settings
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.settings.ThemePreference
import com.rorokaiiworks.goodidlegame.core.settings.ThemeStyle
import com.rorokaiiworks.goodidlegame.core.settings.WindowSetting
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.commons.GameDialog
import com.rorokaiiworks.goodidlegame.ui.commons.TextPair
import io.github.mohammedalaamorsi.colorpicker.ColorPickerDialog
import io.github.mohammedalaamorsi.colorpicker.ColorPickerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.DecimalFormat


sealed class SettingsScreenUiState {
    data object Loading : SettingsScreenUiState()
    data object Idle : SettingsScreenUiState()
}

class SettingsScreenViewModel : ViewModel(), KoinComponent {
    val i18n: I18n by inject()
    val settingsSaver: SettingsSaver by inject()

    private var _uiState = MutableStateFlow<SettingsScreenUiState>(SettingsScreenUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val saveRequests = MutableSharedFlow<Settings>(extraBufferCapacity = 64)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            saveRequests
                .conflate()
                .collect(settingsSaver::updateData)
        }
    }

    fun save(settings: Settings) {
        viewModelScope.launch {
            _uiState.update { SettingsScreenUiState.Loading }

            saveRequests.tryEmit(settings)

            _uiState.update { SettingsScreenUiState.Idle }
        }
    }
}


@Composable
fun SettingsScreen(
    viewModel: SettingsScreenViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentSettings by viewModel.settingsSaver.settings.collectAsState()
    val i18n = viewModel.i18n

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsEditor(
            settings = currentSettings,
            onSettingsChange = viewModel::save,
            showConfirmButton = false,
        )

        BaseCard(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardTitle(title = i18n.tr("About"))

            TextPair(
                title = { Text(text = i18n.tr("Game Version")) },
                value = { Text(Constants.GameVersion.toString()) }
            )

            Text(
                text = """
                    RoRoKaii Works.
                    
                    Special Thanks:
                    Glionox, thanks for your awesome icons.
                    Kenny Assets, thanks for your awesome icons.
                    SimonBay, thanks for your awesome sounds.
                    Bastianhallo, thanks for your awesome sounds.
                    Pizza Doggy, thanks for your awesome music.
                """.trimIndent(),
            )
        }
    }

    if (uiState is SettingsScreenUiState.Loading) {
        GameDialog(
            title = i18n.tr("Loading"),
            onDismissRequest = {},
            content = { Text(i18n.tr("Loading")) }
        )
    }
}



@Composable
fun SettingsEditor(
    modifier: Modifier = Modifier,
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    showConfirmButton: Boolean,
    i18n: I18n = koinInject(),
    onConfirm: (() -> Unit)? = null,
) {
    val themeTitleMap = ThemePreference.entries.associateWith {
        i18n.trc(ThemePreference.I18N_CONTEXT, it.text)
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember(settings.language) { mutableStateOf(settings.language.code) }

    BaseCard(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingsSegmented(
                title = { Text(i18n.tr("Theme Preference")) },
                items = ThemePreference.entries,
                selectedItem = settings.themePreference,
                onItemSelected = { onSettingsChange(settings.copy(themePreference = it)) },
                itemTitleMap = { themeTitleMap[it].orEmpty() }
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(i18n.tr("Theme Style"))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 3
                    ) {
                        ThemeStyle.entries.forEach {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSettingsChange(settings.copy(themeStyle = it)) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = it == settings.themeStyle,
                                    onClick = { onSettingsChange(settings.copy(themeStyle = it)) }
                                )
                                Text(
                                    text = i18n.trc(ThemeStyle.I18N_CONTEXT, it.text),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            SettingsMenuLink(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(i18n.tr("Seed Color"))
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(settings.seedColor), CircleShape)
                        )
                    }
                },
                onClick = { showColorPicker = true }
            )

            SettingsMenuLink(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = i18n.tr("Language"),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = settings.language.displayName,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                        )
                    }
                },
                onClick = { showLanguageDialog = true }
            )


            val formater = DecimalFormat("###.#")
            SettingsSlider(
                title = { Text(i18n.tr("UI Scale")) },
                subtitle = { Text(i18n.tr("Scale: {0}", formater.format(settings.uiScale))) },
                value = settings.uiScale,
                onValueChange = { onSettingsChange(settings.copy(uiScale = it)) },
                valueRange = 0.5f..2f,
                steps = 10,
            )

            SettingsSlider(
                title = { Text(i18n.tr("Sound Volume")) },
                subtitle = { Text(i18n.tr("Volume: {0}", formater.format(settings.soundVolume))) },
                value = settings.soundVolume,
                onValueChange = { onSettingsChange(settings.copy(soundVolume = it)) },
                valueRange = 0f..1f,
                steps = 10,
            )

            SettingsSlider(
                title = { Text(i18n.tr("Music Volume")) },
                subtitle = { Text(i18n.tr("Volume: {0}", formater.format(settings.musicVolume))) },
                value = settings.musicVolume,
                onValueChange = { onSettingsChange(settings.copy(musicVolume = it)) },
                valueRange = 0f..1f,
                steps = 10,
            )
        }

        if (showConfirmButton && onConfirm != null) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onConfirm
            ) {
                Text(i18n.tr("Confirm"))
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            show = true,
            type = ColorPickerType.Circle(
                showBrightnessBar = true,
                showAlphaBar = true,
                lightCenter = true
            ),
            properties = DialogProperties(),
            onDismissRequest = { showColorPicker = false },
            onPickedColor = {
                onSettingsChange(settings.copy(seedColor = it.toArgb()))
                showColorPicker = false
            },
        )
    }

    if (showLanguageDialog) {
        GameDialog(
            title = i18n.tr("Language"),
            onDismissRequest = {
                onSettingsChange(settings.copy(language = Language.findByCode(selectedLanguage)))
                showLanguageDialog = false
            },
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                maxItemsInEachRow = 3
            ) {
                Language.entries.forEach { lang ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedLanguage = lang.code },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = lang.code == selectedLanguage,
                            onClick = { selectedLanguage = lang.code }
                        )
                        Text(
                            text = lang.displayName,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}