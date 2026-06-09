package com.rorokaiiworks.goodidlegame.ui.toasts

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ToastHostViewModel : ViewModel(), KoinComponent {
    private val eventBus: EventBus by inject()

    private val _currentToast = MutableStateFlow<IEvent.ToastMessage?>(null)
    val currentToast = _currentToast.asStateFlow()

    init {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                if (event is IEvent.ToastMessage) {
                    showToast(event)
                }
            }
        }
    }

    private var toastJob: Job? = null

    fun showToast(message: IEvent.ToastMessage) {
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            _currentToast.value = message
            delay(4000)
            _currentToast.value = null
        }
    }
}


@Composable
fun ToastHost(
    modifier: Modifier = Modifier,
    viewModel: ToastHostViewModel = koinInject(),
) {
    val currentToast by viewModel.currentToast.collectAsState()

    AnimatedContent(
        modifier = modifier,
        targetState = currentToast,
        transitionSpec = {
            val springSpec = spring<Float>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )

            if (targetState != null) {
                (slideInVertically { it / 2 } + fadeIn(animationSpec = springSpec) + scaleIn(initialScale = 0.8f))
                    .togetherWith(
                        slideOutVertically { -it / 2 } + fadeOut() + scaleOut(targetScale = 0.9f)
                    )
            } else {
                fadeIn() togetherWith fadeOut(animationSpec = springSpec)
            } using SizeTransform(clip = false)
        },
        label = "ToastAnimation"
    ) { toast ->
        if (toast != null) {
            ToastView(toast)
        }
    }
}

@Composable
fun ToastView(message: IEvent.ToastMessage) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message.msg,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}