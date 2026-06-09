@file:OptIn(ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.window.core.layout.WindowSizeClass
import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.DLCService
import com.rorokaiiworks.goodidlegame.LaunchSettings
import com.rorokaiiworks.goodidlegame.core.GameState
import com.rorokaiiworks.goodidlegame.core.IAdPlayer
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.codex.Codex
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.journey.JourneySystem
import com.rorokaiiworks.goodidlegame.core.loadouts.PlayerLoadouts
import com.rorokaiiworks.goodidlegame.core.mastery.MasteryLevel
import com.rorokaiiworks.goodidlegame.core.offline.OfflineReward
import com.rorokaiiworks.goodidlegame.core.persistent.SaveSystem
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.reveal.RevealEvent
import com.rorokaiiworks.goodidlegame.core.reveal.Revealer
import com.rorokaiiworks.goodidlegame.core.rewards.Reward
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import com.rorokaiiworks.goodidlegame.core.skills.SkillType
import com.rorokaiiworks.goodidlegame.core.starStore.StarStore
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSystem
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.commons.GameDialog
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.commons.OfflineRewardDialogue
import com.rorokaiiworks.goodidlegame.ui.commons.RewardEntry
import com.rorokaiiworks.goodidlegame.ui.loadout.formatModifier
import com.rorokaiiworks.goodidlegame.ui.mainScreen.GameNavHost
import com.rorokaiiworks.goodidlegame.ui.mainScreen.GameTopBar
import com.rorokaiiworks.goodidlegame.ui.mainScreen.MainDrawer
import com.rorokaiiworks.goodidlegame.ui.mainScreen.buildScreenList
import com.rorokaiiworks.goodidlegame.ui.mastery.CodexProgressPanel
import com.rorokaiiworks.goodidlegame.ui.skills.CachedSkillData
import com.rorokaiiworks.goodidlegame.ui.toasts.ToastHost
import com.svenjacobs.reveal.Reveal
import com.svenjacobs.reveal.RevealCanvasState
import com.svenjacobs.reveal.RevealState
import com.svenjacobs.reveal.rememberRevealState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

sealed interface DialogState {
    data object None : DialogState
    data class AdOpportunity(val rewards: List<Reward>) : DialogState
    data object AdPlaying : DialogState
    data object AdFinished : DialogState
    data object AdFailed : DialogState
    data object Effects : DialogState
    data class OfflineReward(val reward: com.rorokaiiworks.goodidlegame.core.offline.OfflineReward) : DialogState
}

data class MainUiState(
    val dialogState: DialogState = DialogState.None,
    val showAdOpportunityIcon: Boolean = false,
    val showStarDropAnimation: Boolean = false,
)

@OptIn(ExperimentalUuidApi::class)
class MainViewModel(
    private val launchSettings: LaunchSettings,
    initialOfflineReward: OfflineReward? = null
) : ViewModel(), KoinComponent {
    val eventBus: EventBus by inject()
    val saveSystem: SaveSystem by inject()
    val journeySystem: JourneySystem by inject()
    val masteryLevel: MasteryLevel by inject()
    val loadouts: PlayerLoadouts by inject()
    val inventory: PlayerInventory by inject()
    val player: Player by inject()
    val playerSkills: PlayerSkills by inject()
    val taskSystem: TaskSystem by inject()
    val starStore: StarStore by inject()
    val gameState: GameState by inject()
    val revealer: Revealer by inject()
    val dlcService: DLCService by inject()
    val codex: Codex by inject()
    val navigator: Navigator by inject()

    private val timeProvider: ITimeProvider by inject()
    private val adPlayer: IAdPlayer by inject()
    val skillsTemplateTable: DataTable<SkillTemplate> by inject(named<SkillTemplate>())
    private val skillActionsTable: DataTable<SkillAction> by inject(named<SkillAction>())

    private val i18n: I18n by inject()
    private val logger: Logger by inject { parametersOf("MainViewModel") }

    val skillDataCache = mutableMapOf<String, CachedSkillData>()

    private var nextAdOpportunityTime: Instant = Instant.DISTANT_PAST

    private val _uiState = MutableStateFlow(
        MainUiState(
            dialogState = initialOfflineReward?.let { DialogState.OfflineReward(it) }
                ?: DialogState.None
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        observeSkillUnlocks()
        scheduleNextAdOpportunity()
        startAdOpportunityChecker()

        if (!launchSettings.disabledSaving) {
            startAutoSave()
        }

        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    IEvent.StarDropped -> {
                        if (!_uiState.value.showStarDropAnimation) {
                            _uiState.update { it.copy(showStarDropAnimation = true) }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun dismissDialog() {
        val currentDialog = _uiState.value.dialogState

        when (currentDialog) {
            is DialogState.AdOpportunity -> {
                scheduleNextAdOpportunity()
                _uiState.update { it.copy(dialogState = DialogState.None, showAdOpportunityIcon = false) }
            }

            is DialogState.AdFinished, is DialogState.AdFailed -> {
                _uiState.update { it.copy(dialogState = DialogState.None, showAdOpportunityIcon = false) }
            }

            else -> {
                _uiState.update { it.copy(dialogState = DialogState.None) }
            }
        }
    }

    fun showAdOpportunityDialog() {
        _uiState.update {
            it.copy(
                dialogState = DialogState.AdOpportunity(
                    rewards = adPlayer.getRandomAdOpportunityRewards()
                )
            )
        }
    }

    fun showEffectsPanel() {
        _uiState.update { it.copy(dialogState = DialogState.Effects) }
    }


    fun confirmAdOpportunity() {
        viewModelScope.launch {
            val rewards = if (_uiState.value.dialogState is DialogState.AdOpportunity) {
                (_uiState.value.dialogState as DialogState.AdOpportunity).rewards
            } else adPlayer.getRandomAdOpportunityRewards()

            _uiState.update { it.copy(dialogState = DialogState.AdPlaying) }

            when (adPlayer.playAd()) {
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            dialogState = DialogState.AdFailed,
                            showAdOpportunityIcon = false
                        )
                    }
                }

                is Resource.Success<*> -> {
                    rewards.forEach { it.grant() }
                    _uiState.update {
                        it.copy(
                            dialogState = DialogState.AdFinished,
                            showAdOpportunityIcon = false
                        )
                    }
                }
            }

            scheduleNextAdOpportunity()
        }
    }

    private fun scheduleNextAdOpportunity() {
        val minDelay = 3.minutes
        val maxDelay = 6.minutes
        val randomDelay = (minDelay.inWholeMilliseconds..maxDelay.inWholeMilliseconds).random().milliseconds

        nextAdOpportunityTime = timeProvider.now() + randomDelay
    }

    private fun startAdOpportunityChecker() {
        viewModelScope.launch {
            while (true) {
                delay(1.seconds)

                if (!_uiState.value.showAdOpportunityIcon &&
                    timeProvider.now() >= nextAdOpportunityTime &&
                    !adPlayer.reachMaxAdCount()
                ) {
                    _uiState.update { it.copy(showAdOpportunityIcon = true) }
                }
            }
        }
    }

    fun claimOfflineReward(offlineReward: OfflineReward) {
        offlineReward.entries.forEach { entry ->
            entry.items.forEach { item ->
                inventory.inventory.addItem(item, emitEvent = false)
            }
            if (entry.skillXp > 0) {
                playerSkills.skills[entry.skillId]?.addXp(entry.skillXp, player.stats)
            }
        }
        dismissDialog()
    }

    fun hideRevealable(key: Any) {
        viewModelScope.launch {
            revealer.hide()
        }
    }

    fun getSkillData(skillId: String): CachedSkillData {
        return skillDataCache.getOrPut(skillId) {
            val skill = requireNotNull(playerSkills.skills[skillId]) {
                "Skill not found: $skillId"
            }
            val template = skillsTemplateTable.find(skillId)

            val actions = if (template.skillType == SkillType.Combat) {
                skillActionsTable.all().filterIsInstance<SkillAction.CombatSkillAction>()
            } else {
                skillActionsTable.all().filter { it.skillId == skillId }
            }

            CachedSkillData(skill, actions)
        }
    }

    private fun observeSkillUnlocks() {
        viewModelScope.launch {
            gameState.unlockEvents.collect { skillId ->
                val skillName = i18n.tr(skillsTemplateTable.find(skillId).name)
                eventBus.emit(
                    IEvent.ToastMessage(
                        msg = i18n.tr("Skill unlocked: {0}", skillName),
                        iconId = skillId
                    )
                )
            }
        }
    }

    private fun startAutoSave() {
        viewModelScope.launch {
            while (true) {
                delay(10.seconds)
                runCatching {
                    saveSystem.save()
                }.onFailure { e ->
                    logger.e(e) { "Auto-save failed" }
                }
            }
        }
    }

    fun onStarDropAnimationEnd() {
        _uiState.update { it.copy(showStarDropAnimation = false) }
    }
}

@Composable
fun MainScreen(
    offlineReward: OfflineReward?,
    viewModel: MainViewModel = koinViewModel(parameters = { parametersOf(offlineReward) }),
    navigator: Navigator = koinInject(),
    launchSettings: LaunchSettings = koinInject(),
    revealCanvasState: RevealCanvasState,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val revealState = rememberRevealState()

    val screens = remember {
        buildScreenList(
            skillsTemplates = viewModel.skillsTemplateTable,
            launchSettings = launchSettings,
            dlcService = viewModel.dlcService
        )
    }
    val isExpandedScreen = windowSizeClass.isWidthAtLeastBreakpoint(
        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
    )

    LaunchedEffect(Unit) {
        viewModel.revealer.events.collect { event ->
            when (event) {
                is RevealEvent.Reveal -> revealState.reveal(event.key)
                RevealEvent.Hide -> revealState.hide()
            }
        }
    }

    if (uiState.dialogState is DialogState.OfflineReward) {
        val reward = (uiState.dialogState as DialogState.OfflineReward).reward
        OfflineRewardScreen(
            reward = reward,
            onClaim = viewModel::claimOfflineReward
        )
        return
    }

    Reveal(
        revealCanvasState = revealCanvasState,
        revealState = revealState,
        onRevealableClick = viewModel::hideRevealable
    ) {
        MainScreenContent(
            isExpandedScreen = isExpandedScreen,
            uiState = uiState,
            viewModel = viewModel,
            navigator = navigator,
            launchSettings = launchSettings,
            screens = screens,
            scope = scope,
            drawerState = drawerState,
            revealState = revealState
        )
    }

    if (uiState.showStarDropAnimation) {
        StarDropAnimation(
            onAnimationEnd = { viewModel.onStarDropAnimationEnd() }
        )
    }
}

@Composable
private fun StarDropAnimation(
    onAnimationEnd: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")

    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowIntensity"
    )

    val animY = remember { Animatable(1200f) }
    val animAlpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { animAlpha.animateTo(1f, tween(300)) }
        launch {
            rotation.animateTo(720f, tween(1500, easing = LinearOutSlowInEasing))
        }

        animY.animateTo(-100f, tween(500, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
        delay(100)
        animY.animateTo(2000f, tween(600, easing = ExpoEaseIn))

        onAnimationEnd()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Yellow.copy(alpha = 0.3f * glowIntensity), Color.Transparent),
                    center = center,
                    radius = size.maxDimension / 2f
                ),
                radius = size.maxDimension / 2f,
                blendMode = BlendMode.Screen
            )
        }

        GameImage(
            iconName = "star",
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.Center)
                .graphicsLayer {
                    translationY = animY.value
                    alpha = animAlpha.value
                    rotationZ = rotation.value + (animY.value * 0.1f)
                    scaleX = 1.6f
                    scaleY = 1.6f
                },
        )
    }
}

private val ExpoEaseIn = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)


@Composable
private fun OfflineRewardScreen(
    reward: OfflineReward,
    onClaim: (OfflineReward) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OfflineRewardDialogue(
                offlineReward = reward,
                onClick = onClaim
            )
        }
    }
}

@Composable
private fun MainScreenContent(
    isExpandedScreen: Boolean,
    uiState: MainUiState,
    viewModel: MainViewModel,
    navigator: Navigator,
    launchSettings: LaunchSettings,
    screens: List<AppDestination>,
    scope: CoroutineScope,
    drawerState: DrawerState,
    revealState: RevealState
) {
    val topBar: @Composable () -> Unit = {
        GameTopBar(
            scope = scope,
            drawerState = drawerState,
            destination = navigator.currentDestination,
            showDrawerButton = !isExpandedScreen,
            effectsClick = viewModel::showEffectsPanel,
            showAdOpportunityButton = uiState.showAdOpportunityIcon,
            onAdOpportunityClick = viewModel::showAdOpportunityDialog,
            title = {
                ToastHost()
            }
        )
    }

    val drawerContent: @Composable () -> Unit = {
        MainDrawer(
            screens = screens,
            destination = navigator.currentDestination,
            viewModel = viewModel,
            scope = scope,
            drawerState = drawerState,
            revealState = revealState,
            lockSkills = launchSettings.lockSkills,
            onNavigate = { navigator.navigateTo(it) }
        )
    }

    val isWideScreen = isWideScreen()

    val content: @Composable (PaddingValues) -> Unit = { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            DialogHost(
                dialogState = uiState.dialogState,
                viewModel = viewModel
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GameNavHost(
                    modifier = Modifier.weight(0.7f),
                    viewModel = viewModel,
                    backStack = navigator.backStack
                )

                if (viewModel.navigator.currentDestination is AppDestination.SkillDestination && isWideScreen) {
                    CodexProgressPanel(
                        modifier = Modifier.weight(0.3f).fillMaxHeight(),
                        codex = viewModel.codex,
                    )
                }
            }
        }
    }

    if (isExpandedScreen) {
        PermanentNavigationDrawer(drawerContent = drawerContent) {
            Scaffold(topBar = topBar, content = content)
        }
    } else {
        ModalNavigationDrawer(
            drawerContent = drawerContent,
            drawerState = drawerState
        ) {
            Scaffold(topBar = topBar, content = content)
        }
    }
}

@Composable
private fun DialogHost(
    i18n: I18n = koinInject(),
    dialogState: DialogState,
    viewModel: MainViewModel
) {
    when (dialogState) {
        is DialogState.AdOpportunity -> {
            GameDialog(
                title = i18n.tr("Ad Opportunity"),
                onDismissRequest = viewModel::dismissDialog,
                onConfirmation = viewModel::confirmAdOpportunity,
                content = {
                    Text(text = i18n.tr("Ad Opportunity Dialog Desc"))
                    dialogState.rewards.forEach {
                        RewardEntry(
                            modifier = Modifier.fillMaxWidth(),
                            reward = it
                        )
                    }
                }
            )
        }

        is DialogState.AdPlaying -> {
            GameDialog(
                title = "Ad Playing",
                onDismissRequest = {},
                content = {}
            )
        }

        is DialogState.AdFinished -> {
            GameDialog(
                title = i18n.tr("Ad Opportunity Finished"),
                onDismissRequest = viewModel::dismissDialog,
                content = {}
            )
        }

        is DialogState.AdFailed -> {
            GameDialog(
                title = i18n.tr("Ad Failed"),
                onDismissRequest = viewModel::dismissDialog,
                content = {}
            )
        }

        is DialogState.Effects -> {
            GameDialog(
                title = i18n.tr("Activated Modifiers"),
                onDismissRequest = viewModel::dismissDialog,
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.player.effectManager.effects.forEach { effect ->
                            EffectLabel(effect = effect)
                        }
                    }
                }
            )
        }

        is DialogState.None -> {}
        is DialogState.OfflineReward -> { /* 在外部处理 */
        }
    }
}


@Composable
private fun EffectLabel(i18n: I18n = koinInject(), effect: Effect) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = effect.sourceName.sourceName)

        Column(
            modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth()
        ) {
            effect.modifiers.forEach {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = i18n.trc("stat_id", it.statId))
                    Text(text = formatModifier(it), color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        HorizontalDivider()
    }
}
