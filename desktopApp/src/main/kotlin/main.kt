import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import co.touchlab.kermit.NoTagFormatter
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import com.codedisaster.steamworks.*
import com.rorokaiiworks.goodidlegame.*
import com.rorokaiiworks.goodidlegame.core.I18nLoader
import com.rorokaiiworks.goodidlegame.core.achievements.AchievementSystem
import com.rorokaiiworks.goodidlegame.core.achievements.IAchievementAdapter
import com.rorokaiiworks.goodidlegame.core.di.preStartModule
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.persistent.SaveSystem
import com.rorokaiiworks.goodidlegame.core.settings.Language.Companion.readBytes
import com.rorokaiiworks.goodidlegame.core.settings.Settings
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.settings.WindowSetting
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import name.kropp.kotlinx.gettext.Gettext
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicBoolean
import javax.sound.sampled.AudioSystem
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

private data class CliOptions(
    val findLibRoot: String?,
    val isDebug: Boolean,
    val skipBootStage: Boolean,
    val steamDisabledByArg: Boolean,
    val disabledSaving: Boolean,
    val debugMenu: Boolean,
    val mockNoDLCUnlocked: Boolean,
)

private data class StartupContext(
    val logger: Logger,
    val isDebug: Boolean,
    val launchSettings: LaunchSettings,
    val initialSettings: Settings,
    val i18n: I18n,
    val steamAvailable: Boolean,
)

private val isShuttingDown = AtomicBoolean(false)

@Suppress("UnsafeDynamicallyLoadedCode")
fun main(args: Array<String>) {
    val logger = Logger(
        config = loggerConfigInit(platformLogWriter(NoTagFormatter)),
        tag = "GoodIdleGame"
    )
    logger.i { "args: ${args.joinToString()}" }

    val options = parseCliOptions(args)
    val startup = bootstrapApplication(logger, options)
    val koin = ensureKoinStarted(startup)

    CrashReporter.setup()

    application {
        DesktopAppWindow(
            startup = startup,
            koin = koin,
            onExitRequest = { exit(
                0,
                info = { logger.i { it } },
                error = { err, info -> logger.e(err) { info } }
            ) }
        )
    }
}

private fun parseCliOptions(args: Array<String>): CliOptions {
    return CliOptions(
        findLibRoot = args.firstOrNull { it.startsWith("--findLib=") }?.removePrefix("--findLib="),
        isDebug = args.any { it == "--debug" },
        skipBootStage = args.any { it == "--skipBootStage" },
        steamDisabledByArg = args.any { it == "--disabledSteam" },
        disabledSaving = args.any { it == "--disabledSaving" },
        debugMenu = args.any { it == "--cheatMenu" },
        mockNoDLCUnlocked = args.any { it == "--mockNoDLCUnlocked" },
    )
}

private fun bootstrapApplication(
    logger: Logger,
    options: CliOptions,
): StartupContext {
    if (options.isDebug) {
        logger.i { "debug mode" }
    }

    logger.i { "--- Supported File Types ---" }
    AudioSystem.getAudioFileTypes().forEach { logger.i { "Type: $it" } }

    val launchSettings = LaunchSettings(
        lockSkills = !options.isDebug,
        skipStartMenu = options.skipBootStage,
        disabledSaving = options.disabledSaving,
        skipBootAnimation = options.skipBootStage,
        debugMenu = options.debugMenu,
        mockNoDLCUnlocked = options.mockNoDLCUnlocked,
    )

    val steamAvailable = initSteam(
        logger = logger,
        steamDisabledByArg = options.steamDisabledByArg,
        findLibRoot = options.findLibRoot,
        isDebug = options.isDebug
    )

    val initialSettings = try {
        loadJvmInitialSettings()
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

    return StartupContext(
        logger = logger,
        isDebug = options.isDebug,
        launchSettings = launchSettings,
        initialSettings = initialSettings,
        i18n = i18n,
        steamAvailable = steamAvailable,
    )
}

private fun initSteam(
    logger: Logger,
    steamDisabledByArg: Boolean,
    findLibRoot: String?,
    isDebug: Boolean,
): Boolean {
    if (steamDisabledByArg) {
        logger.i { "steamDisabledByArg: ${true}" }
        return false
    }

    logger.i { "Loading steam..." }

    val osName = System.getProperty("os.name").lowercase()
    val isWindows = osName.contains("windows")
    val isLinux = osName.contains("linux")

    val (steamLibDir, steamApiLib, steamworks4jLib) = when {
        isWindows -> Triple("windows64", "steam_api64.dll", "steamworks4j64.dll")
        isLinux -> Triple("linux64", "libsteam_api.so", "libsteamworks4j.so")
        else -> {
            logger.e { "Unsupported operating system for Steam: $osName" }
            return false
        }
    }

    val steamApiLibPath = resolveSteamLibPath(
        findLibRoot = findLibRoot,
        isDebug = isDebug,
        steamLibDir = steamLibDir,
        fileName = steamApiLib
    )
    val steamworks4jLibPath = resolveSteamLibPath(
        findLibRoot = findLibRoot,
        isDebug = isDebug,
        steamLibDir = steamLibDir,
        fileName = steamworks4jLib
    )

    logger.i { "Operating System: $osName" }
    logger.i { "Steam API Path: $steamApiLibPath" }
    logger.i { "Steamworks4j Path: $steamworks4jLibPath" }

    return try {
        System.load(Path(steamApiLibPath).normalize().toAbsolutePath().toString())
        System.load(Path(steamworks4jLibPath).normalize().toAbsolutePath().toString())

        SteamAPI.loadLibraries(LocalSteamLibraryLoader())
        SteamAPI.restartAppIfNecessary(4313870)

        val status = SteamAPI.init()
        logger.i { "steam init status: $status" }
        status
    } catch (e: SteamException) {
        logger.e(e) { "Steam init failed, continue with non-Steam mode" }
        false
    } catch (e: Throwable) {
        logger.e(e) { "Steam native library loading failed, continue with non-Steam mode" }
        false
    }
}

private fun resolveSteamLibPath(
    findLibRoot: String?,
    isDebug: Boolean,
    steamLibDir: String,
    fileName: String,
): String {
    return when {
        findLibRoot != null -> Path(findLibRoot).resolve("$steamLibDir/$fileName").normalize().absolutePathString()
        isDebug -> Path("$steamLibDir/$fileName").normalize().absolutePathString()
        else -> Path(fileName).normalize().absolutePathString()
    }
}

private fun ensureKoinStarted(startup: StartupContext): Koin {
    GlobalContext.getOrNull()?.let { return it }

    return startKoin {
        modules(
            module {
                single<I18n> { I18nLoader(startup.initialSettings.language.locale, startup.i18n) }
                single {
                    AchievementSystem(
                        runCatching { get<IAchievementAdapter>() }.getOrNull()
                    )
                }

                if (startup.steamAvailable) {
                    single { SteamStatsManager() }
                    single<SteamUserStatsCallback> { get<SteamStatsManager>() }
                    single { SteamUserStats(get()) }
                    single { SteamUser(object : SteamUserCallback {}) }
                    single {
                        val eventBus: EventBus = get()
                        SteamFriends(object : SteamFriendsCallback {
                            override fun onGameOverlayActivated(active: Boolean, userInitiated: Boolean, appID: Int) {
                                if (active) {
                                    eventBus.tryEmit(IEvent.SteamOverlayOpened)
                                }
                            }
                        })
                    }
                    single { SteamApps() }
                }
            } + jvmSettingsModule + jvmModules(startup.steamAvailable) + preStartModule(startup.launchSettings, startup.isDebug)
        )
    }.koin
}

@Composable
private fun DesktopAppWindow(
    startup: StartupContext,
    koin: Koin,
    onExitRequest: () -> Unit,
) {
    val settingsSaver = koin.get<SettingsSaver>()
    val settings by settingsSaver.settings.map { it }.collectAsState(startup.initialSettings)

    val windowState = rememberWindowState(
        width = 1200.dp,
        height = 800.dp,
        placement = settings.windowSetting.toPlacement(),
    )

    LaunchedEffect(settings.windowSetting) {
        windowState.placement = settings.windowSetting.toPlacement()
    }

    Window(
        state = windowState,
        onCloseRequest = {
            exitAppHandle(
                logger = startup.logger,
                koin = koin,
                finished = onExitRequest
            )
        },
        title = "GoodIdleGame",
    ) {
        window.minimumSize = Dimension(350, 600)

        if (startup.steamAvailable) {
            SteamCallbackHandler()
        }

        App()
    }
}

private fun WindowSetting.toPlacement(): WindowPlacement {
    return when (this) {
        WindowSetting.Windowed -> WindowPlacement.Floating
        WindowSetting.Fullscreen -> WindowPlacement.Fullscreen
    }
}

private fun exitAppHandle(
    logger: Logger,
    koin: Koin?,
    finished: () -> Unit = {},
) {
    if (!isShuttingDown.compareAndSet(false, true)) return

    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            koin?.get<SaveSystem>()?.save()
            logger.i { "exit app; saveSystem saved" }
        }.onFailure { logger.e(it) { "save on exit failed" } }

        logger.i { "exit application" }
        finished()
    }
}

@Composable
private fun SteamCallbackHandler() {
    LaunchedEffect(Unit) {
        while (isActive) {
            if (SteamAPI.isSteamRunning()) {
                SteamAPI.runCallbacks()
            }
            delay(33)
        }
    }
}
