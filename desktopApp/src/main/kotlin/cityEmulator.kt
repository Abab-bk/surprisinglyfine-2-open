import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rorokaiiworks.goodidlegame.CityEmulator
import com.rorokaiiworks.goodidlegame.CityEmulatorUi
import com.rorokaiiworks.goodidlegame.LaunchSettings
import com.rorokaiiworks.goodidlegame.core.di.coreModule
import com.rorokaiiworks.goodidlegame.core.di.preStartModule
import com.rorokaiiworks.goodidlegame.getOtherModules
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin

fun main(args: Array<String>) {
    runBlocking {
        val otherModules = getOtherModules()

        startKoin {
            modules(
                preStartModule(
                    launchSettings = LaunchSettings(),
                    isDebug = false
                ) + coreModule + otherModules
            )
        }
    }

    val emulator = CityEmulator()

    application {
        Window(
            state = rememberWindowState(
                width = 500.dp,
                height = 500.dp,
            ),
            title = "GoodIdleGame Emulator",
            onCloseRequest = { exitApplication() },
        ) {
            CityEmulatorUi(emulator)
        }
    }
}