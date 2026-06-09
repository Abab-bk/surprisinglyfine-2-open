package com.rorokaiiworks.goodidlegame

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.I18nLoader
import com.rorokaiiworks.goodidlegame.core.ISoundPlayer
import com.rorokaiiworks.goodidlegame.core.achievements.AchievementSystem
import com.rorokaiiworks.goodidlegame.core.codex.Codex
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.offline.OfflineReward
import com.rorokaiiworks.goodidlegame.core.persistent.SaveSlot
import com.rorokaiiworks.goodidlegame.core.persistent.SaveSystem
import com.rorokaiiworks.goodidlegame.core.settings.Language.Companion.layoutDirection
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.tutorial.TutorialSystem
import com.rorokaiiworks.goodidlegame.ui.LoadingScreen
import com.rorokaiiworks.goodidlegame.ui.MainMenuWithStartup
import com.rorokaiiworks.goodidlegame.ui.MainScreen
import com.rorokaiiworks.goodidlegame.ui.codex.ItemMasteredPopup
import com.rorokaiiworks.goodidlegame.ui.codex.MasteryLevelUpPopup
import com.rorokaiiworks.goodidlegame.ui.commons.GameDialog
import com.rorokaiiworks.goodidlegame.ui.commons.RequirementEntry
import com.rorokaiiworks.goodidlegame.ui.mainScreen.GameLoop
import com.svenjacobs.reveal.RevealCanvas
import com.svenjacobs.reveal.rememberRevealCanvasState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.parameter.parametersOf

sealed class AppState {
    data object LoadingSettings : AppState()
    data object StartMenu : AppState()
    data object Loading : AppState()
    data class RunningGame(val offlineReward: OfflineReward?) : AppState()
}


class AppViewModel(
    private val launchSettings: LaunchSettings
) : ViewModel(), KoinComponent {
    private val saveSystem: SaveSystem by inject()
    private val eventBus: EventBus by inject()
    val i18nLoader: I18n by inject()
    val tutorialSystem: TutorialSystem by inject()

    val logger: Logger by inject { parametersOf("AppViewModel") }

    val settingsSaver: SettingsSaver by inject()

    val masteredQueue: SnapshotStateList<ItemTemplate> = mutableStateListOf()
    val masteryLevelQueue: SnapshotStateList<Int> = mutableStateListOf()
    val soundPlayer: ISoundPlayer by inject()

    private val _state = MutableStateFlow<AppState>(AppState.LoadingSettings)
    val state: StateFlow<AppState> = _state

    val layoutDirection: StateFlow<LayoutDirection> = settingsSaver.settings
        .map { it.language.layoutDirection }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LayoutDirection.Ltr
        )

    private var loadedModules = false

    init {
        logger.i { "Init" }

        viewModelScope.launch {
            settingsSaver.settings.collect {
                if (i18nLoader is I18nLoader) {
                    val changed = (i18nLoader as I18nLoader).changeLanguage(it.language)
                    if (changed && state.value is AppState.RunningGame) {
                        _state.value = AppState.Loading
                        delay(500)
                        _state.value = AppState.RunningGame(null)
                    }
                }
            }
        }

        loadingSettings()

        observeEvents()
    }

    private fun observeEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is IEvent.StartGame -> startGame(event.slot)
                    is IEvent.ItemMastered -> {
                        masteredQueue.add(event.itemTemplate)
                    }
                    is IEvent.MasteryLevelUp -> {
                        masteryLevelQueue.add(event.level)
                    }
                    else -> {}
                }
            }
        }
    }

    fun startGame(saveSlot: SaveSlot) {
        viewModelScope.launch {
            _state.value = AppState.Loading
            if (!loadedModules) {
                loadOtherModules()
                loadedModules = true
            }
            val offlineReward = saveSystem.loadGame(saveSlot.slotId)
            saveSystem.save()
            _state.value = AppState.RunningGame(offlineReward)
        }
    }

    private fun loadingSettings() {
        logger.i { "Loading settings.." }
        viewModelScope.launch {
            if (launchSettings.skipStartMenu) {
                if (!loadedModules) {
                    loadOtherModules()
                    loadedModules = true
                }

                if (launchSettings.disabledSaving) {
                    _state.value = AppState.RunningGame(null)
                } else {
                    val offlineReward = saveSystem.loadGame("test_slot")
                    saveSystem.save()
                    _state.value = AppState.RunningGame(offlineReward)
                }

                return@launch
            }

            _state.value = AppState.StartMenu
        }
    }

    private suspend fun loadOtherModules() {
        loadKoinModules(getOtherModules())

        // create something cuz them should be createAtStart
        val codex = get<Codex>()
        val achievementSystem = get<AchievementSystem>()
    }
}

@Composable
fun App(
    viewModel: AppViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val revealCanvasState = rememberRevealCanvasState()

    val settings by viewModel.settingsSaver.settings.collectAsState()
    val layoutDirection by viewModel.layoutDirection.collectAsState()

    LaunchedEffect(settings.soundVolume) {
        viewModel.soundPlayer.setSoundsVolume(settings.soundVolume)
    }

    LaunchedEffect(settings.musicVolume) {
        viewModel.soundPlayer.setMusicVolume(settings.musicVolume)
    }

    LaunchedEffect(Unit) {
        viewModel.soundPlayer.playMusic("goldenGleam")
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        val systemDensity = LocalDensity.current
        val customDensity = remember(systemDensity, settings.uiScale) {
            Density(
                density = systemDensity.density * settings.uiScale,
                fontScale = systemDensity.fontScale
            )
        }

        CompositionLocalProvider(LocalDensity provides customDensity) {
            IdleGameTheme(
                themePreference = settings.themePreference,
                themeStyle = settings.themeStyle,
                seedColor = settings.seedColor,
            ) {
                when (val s = state) {
                    is AppState.LoadingSettings -> {
                        LoadingScreen("Loading settings..")
                    }

                    is AppState.StartMenu -> {
                        MainMenuWithStartup()
                    }

                    is AppState.Loading -> {
                        LoadingScreen("Loading..")
                    }

                    is AppState.RunningGame -> {
                        GameLoop()

                        RevealCanvas(
                            revealCanvasState = revealCanvasState
                        ) {
                            MainScreen(
                                offlineReward = s.offlineReward,
                                revealCanvasState = revealCanvasState
                            )
                        }
                    }
                }

                if (viewModel.masteredQueue.count() > 0) {
                    ItemMasteredPopup(
                        itemTemplate = viewModel.masteredQueue.first(),
                        onClick = { viewModel.masteredQueue.remove(viewModel.masteredQueue.first()) },
                        onReady = { viewModel.soundPlayer.playSound("itemMastered") },
                    )
                    return@IdleGameTheme
                }

                if (viewModel.masteryLevelQueue.count() > 0) {
                    MasteryLevelUpPopup(
                        level = viewModel.masteryLevelQueue.first(),
                        onReady = { viewModel.soundPlayer.playSound("masteryLevelUp") },
                        onClick = { viewModel.masteryLevelQueue.remove(viewModel.masteryLevelQueue.first()) },
                    )
                    return@IdleGameTheme
                }

                viewModel.tutorialSystem.currentStep?.let {
                    if (it.dialog) {
                        GameDialog(
                            title = viewModel.i18nLoader.tr("Tutorial"),
                            onDismissRequest = { viewModel.tutorialSystem.nextStep() },
                            content = {
                                Text(text = viewModel.i18nLoader.tr(it.text))
                            }
                        )
                    } else {
                        Box(modifier = Modifier
                            .padding(40.dp)
                            .fillMaxSize()
                        ) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomEnd).widthIn(max = 300.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(text = viewModel.i18nLoader.tr(it.text))

                                    it.conditions.forEach { requirement ->
                                        if (requirement.progressText != null && requirement.progressText != "") {
                                            RequirementEntry(
                                                modifier = Modifier.fillMaxWidth(),
                                                requirement = requirement
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
