package com.rorokaiiworks.goodidlegame.ui.mainScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import com.rorokaiiworks.goodidlegame.core.GameEngine
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class GameLoopViewModel : ViewModel(), KoinComponent {
    val gameEngine: GameEngine by inject()
    val timeProvider: ITimeProvider by inject()
}

@Composable
fun GameLoop(viewModel: GameLoopViewModel = koinViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.gameEngine.start()

        withContext(Dispatchers.Default) {
            while (true) {
                viewModel.gameEngine.tick1(0.2f, viewModel.timeProvider)
                viewModel.gameEngine.tick2(0.2f, viewModel.timeProvider)

                delay(500)
            }
        }
    }
}