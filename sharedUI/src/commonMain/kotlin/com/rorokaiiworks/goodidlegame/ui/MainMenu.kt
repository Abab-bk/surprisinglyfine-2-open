package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.window.core.layout.WindowSizeClass
import co.touchlab.kermit.Logger
import com.mikepenz.hypnoticcanvas.shaderBackground
import com.mikepenz.hypnoticcanvas.shaders.MesmerizingLens
import com.rorokaiiworks.goodidlegame.ComplianceService
import com.rorokaiiworks.goodidlegame.DLCService
import com.rorokaiiworks.goodidlegame.LaunchSettings
import com.rorokaiiworks.goodidlegame.core.ILogin
import com.rorokaiiworks.goodidlegame.core.settings.Settings
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.exit
import com.rorokaiiworks.goodidlegame.ui.onBoarding.OnboardingScreen
import com.rorokaiiworks.goodidlegame.ui.persistent.SaveSystemPanel
import goodidlegame.sharedui.generated.resources.Res
import goodidlegame.sharedui.generated.resources.company_logo
import goodidlegame.sharedui.generated.resources.logo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

sealed class StartupState {
    data object CompanyLogo : StartupState()
    data object GameLogo : StartupState()
    data object Login : StartupState()
    data class Ready(
        val needsOnboarding: Boolean,
        val isLoading: Boolean = false
    ) : StartupState()
}

class StartupViewModel(
    private val loginProvider: ILogin
) : ViewModel(), KoinComponent {
    private val settingsSaver: SettingsSaver by inject()
    private val complianceService: ComplianceService by inject()
    private val logger: Logger by inject { parametersOf("StartupViewModel") }

    var uiState: StartupState by mutableStateOf(StartupState.CompanyLogo)
        private set

    var loginError by mutableStateOf<String?>(null)
        private set

    var isLoggingIn by mutableStateOf(false)
        private set

    private var initialLoginStateResolved by mutableStateOf(false)
    private var isInitiallyLoggedIn by mutableStateOf(false)
    private var isComplianceRunning by mutableStateOf(false)

    init {
        resolveInitialLoginState()
    }

    fun skipBootSequence() {
        viewModelScope.launch {
            waitForInitialLoginState()
            routeAfterBoot()
        }
    }

    fun completeOnboarding(settings: Settings) {
        viewModelScope.launch(Dispatchers.IO) {
            uiState = StartupState.Ready(needsOnboarding = true, isLoading = true)
            settingsSaver.updateData(settings.copy(isFinishedInitialSetup = true))
            uiState = StartupState.Ready(needsOnboarding = false)
        }
    }

    fun proceedToGameLogo() {
        if (uiState == StartupState.CompanyLogo) {
            uiState = StartupState.GameLogo
        }
    }

    fun proceedAfterGameLogo() {
        if (uiState != StartupState.GameLogo) return

        viewModelScope.launch {
            waitForInitialLoginState()
            routeAfterBoot()
        }
    }

    fun performLogin() {
        viewModelScope.launch {
            if (isLoggingIn) {
                logger.i { "Already logging in, ignoring" }
                return@launch
            }
            isLoggingIn = true
            loginError = null

            when (loginProvider.login()) {
                ILogin.LoginResult.Success -> {
                    delay(300)
                    runComplianceAndRoute()
                }

                ILogin.LoginResult.NotLoggedIn -> {
                    isLoggingIn = false
                    loginError = "Login failed"
                }
            }
        }
    }

    private suspend fun routeAfterBoot() {
        if (isInitiallyLoggedIn) {
            runComplianceAndRoute()
        } else {
            uiState = StartupState.Login
        }
    }

    private suspend fun runComplianceAndRoute() {
        if (isComplianceRunning) {
            logger.i { "Compliance already running, ignoring" }
            return
        }

        isComplianceRunning = true
        isLoggingIn = true
        loginError = null
        try {
            logger.i { "Checking for compliance" }
            val passed = complianceService.startCompliance()
            logger.i { "Compliance check passed: $passed" }
            if (passed) {
                uiState = StartupState.Ready(needsOnboarding = needsOnboarding())
            } else {
                uiState = StartupState.Login
                loginError = "Compliance required"
            }
        } catch (t: Throwable) {
            logger.e { "Compliance check failed: ${t.message}" }
            uiState = StartupState.Login
            loginError = t.message ?: "Compliance failed"
        } finally {
            logger.i { "Compliance check finally" }
            isLoggingIn = false
            isComplianceRunning = false
        }
    }

    private fun resolveInitialLoginState() {
        viewModelScope.launch {
            isInitiallyLoggedIn = loginProvider.isLoggedIn() is ILogin.LoginResult.Success
            initialLoginStateResolved = true
        }
    }

    private suspend fun waitForInitialLoginState() {
        while (!initialLoginStateResolved) {
            delay(30)
        }
    }

    private fun needsOnboarding(): Boolean {
        return !settingsSaver.settings.value.isFinishedInitialSetup
    }

    fun exitApp() {
        exit(
            0,
            info = { println(it) },
            error = { err, info ->
                println(err.toString())
                println(info) },
        )
    }
}

@Composable
private fun StartupFlow(
    viewModel: StartupViewModel,
    loginUi: ILoginUi
) {
    Box(
        Modifier
            .fillMaxSize()
    ) {
        when (val state = viewModel.uiState) {
            StartupState.CompanyLogo -> CompanyLogoScreen(viewModel::proceedToGameLogo)

            StartupState.GameLogo,
            StartupState.Login -> GameLogoWithLogin(
                loginUi = loginUi,
                error = viewModel.loginError,
                isLoading = viewModel.isLoggingIn,
                showLogin = state == StartupState.Login,
                onLogoPhaseComplete = viewModel::proceedAfterGameLogo,
                onLoginClick = viewModel::performLogin
            )

            is StartupState.Ready -> ReadyTransition(
                uiState = state,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun CompanyLogoScreen(onComplete: () -> Unit) {
    var isVisible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600)
    )

    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
        delay(1800)
        isVisible = false
        delay(1000)
        onComplete()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onComplete),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.company_logo),
                contentScale = ContentScale.None,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
        }
    }
}

@Composable
private fun GameLogoWithLogin(
    loginUi: ILoginUi,
    error: String?,
    isLoading: Boolean,
    showLogin: Boolean,
    onLoginClick: () -> Unit,
    onLogoPhaseComplete: () -> Unit
) {
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }
    val offset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(500)) }
        launch { offset.animateTo(100f, tween(600, easing = LinearOutSlowInEasing)) }
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        onLogoPhaseComplete()
    }

    Surface(
        Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize()) {
            GameLogoImage(
                modifier = Modifier.offset(y = offset.value.dp).align(Alignment.TopCenter),
                scale = scale.value,
                alpha = alpha.value,
            )

            AnimatedVisibility(
                visible = showLogin,
                enter = fadeIn() + slideInVertically { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
            ) {
                LoginArea(loginUi, isLoading, error, onLoginClick)
            }
        }
    }
}

@Composable
private fun GameLogoImage(
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    alpha: Float = 1f,
) {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
    )
}

@Composable
private fun LoginArea(
    loginUi: ILoginUi,
    isLoading: Boolean,
    error: String?,
    onLoginClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 360.dp)
    ) {
        loginUi.LoginButton(onClick = onLoginClick, isLoading = isLoading)
        Spacer(Modifier.height(16.dp))
        Text(
            text = error ?: "",
            color = Color(0xFFFFCDD2),
            fontSize = 14.sp,
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun ReadyTransition(
    uiState: StartupState.Ready,
    viewModel: StartupViewModel,
) {
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    val isWide = isWideScreen(windowSize)

    var phase by remember { mutableIntStateOf(0) }

    val shaderAlpha by animateFloatAsState(if (phase >= 1) 1f else 0f, tween(600))
    val menuAlpha by animateFloatAsState(if (phase >= 2) 1f else 0f, tween(500))
    val menuOffset by animateFloatAsState(if (phase >= 2) 0f else 24f, tween(500))

    LaunchedEffect(Unit) {
        phase = 1
        delay(200)
        phase = 2
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0xFF002FA7)).graphicsLayer { alpha = 1f - shaderAlpha })
        Box(Modifier.fillMaxSize().shaderBackground(MesmerizingLens).graphicsLayer { alpha = shaderAlpha })

        ResponsiveLayout(
            uiState = uiState,
            viewModel = viewModel,
            isWide = isWide,
            logo = { MenuLogoWithDlcNews() },
            content = {
                MenuContent(
                    modifier = Modifier.graphicsLayer {
                        alpha = menuAlpha
                        translationY = menuOffset.dp.toPx()
                    },
                    onExit = {

                    }
                )
            }
        )
    }
}

@Composable
private fun ResponsiveLayout(
    uiState: StartupState.Ready,
    viewModel: StartupViewModel,
    isWide: Boolean,
    logo: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val mainContent: @Composable () -> Unit = when {
        uiState.isLoading -> {
            { LoadingIndicator("Ready..") }
        }

        uiState.needsOnboarding -> {
            {
                OnboardingScreen(
                    modifier = Modifier.fillMaxSize(),
                    onConfirm = viewModel::completeOnboarding,
                )
            }
        }

        else -> content
    }

    if (isWide) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(0.4f).fillMaxHeight(), Alignment.Center) { logo() }
            Box(Modifier.weight(0.6f).fillMaxHeight()) { mainContent() }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(0.3f).fillMaxSize(), Alignment.Center) { logo() }
            Box(Modifier.weight(0.7f).fillMaxHeight()) { mainContent() }
        }
    }
}

@Composable
private fun LoadingIndicator(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(message)
        }
    }
}

@Composable
fun MainMenuWithStartup(
    loginUi: ILoginUi = koinInject(),
    launchSettings: LaunchSettings = koinInject(),
    viewModel: StartupViewModel = koinInject()
) {
    LaunchedEffect(launchSettings.skipBootAnimation) {
        if (launchSettings.skipBootAnimation) {
            viewModel.skipBootSequence()
        }
    }

    when (val state = viewModel.uiState) {
        is StartupState.Ready -> MainMenu(startupReadyState = state, viewModel = viewModel)
        else -> StartupFlow(viewModel, loginUi)
    }
}

@Composable
private fun MainMenu(
    startupReadyState: StartupState.Ready,
    viewModel: StartupViewModel,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shaderBackground(MesmerizingLens)
        ) {
        ResponsiveLayout(
            uiState = startupReadyState,
            viewModel = viewModel,
            isWide = isWideScreen(windowSizeClass),
            logo = { MenuLogoWithDlcNews() },
            content = {
                MenuContent(
                    modifier = Modifier.fillMaxSize(),
                    onExit = viewModel::exitApp
                )
            }
        )
        }
    }
}

@Composable
private fun MenuContent(
    modifier: Modifier = Modifier,
    onExit: () -> Unit,
    i18n: I18n = koinInject()
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SaveSystemPanel(Modifier.weight(0.9f))

        Button(
            onClick = onExit,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.weight(0.1f).fillMaxWidth()
        ) {
            Text(i18n.tr("Exit"))
        }
    }
}

@Composable
private fun MenuLogoWithDlcNews(
    modifier: Modifier = Modifier,
    dlcService: DLCService = koinInject()
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GameLogoImage(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
        )

//        if (!dlcService.unlocked(DLC.Societal)) {
//            Spacer(Modifier.height(12.dp))
//            Surface(
//                shape = RoundedCornerShape(10.dp),
//                tonalElevation = 4.dp,
//                shadowElevation = 3.dp,
//                modifier = Modifier
//                    .fillMaxWidth(0.75f)
//                    .clickable { dlcService.goToDlcShop(DLC.Societal) }
//            ) {
//                Image(
//                    painter = painterResource(Res.drawable.dlc_free_news),
//                    contentDescription = "free dlc",
//                    contentScale = ContentScale.FillWidth,
//                    modifier = Modifier.fillMaxWidth()
//                )
//            }
//        }
    }
}
