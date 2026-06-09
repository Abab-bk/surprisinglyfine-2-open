package com.rorokaiiworks.goodidlegame.core.reveal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class Revealer {
    private val _events = MutableSharedFlow<RevealEvent>()
    val events = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Default)

    fun tryReveal(key: Any) {
        scope.launch {
            reveal(key)
        }
    }

    suspend fun reveal(key: Any) {
        _events.emit(RevealEvent.Reveal(key))
    }

    suspend fun hide() {
        _events.emit(RevealEvent.Hide)
    }
}

sealed interface RevealEvent {
    data class Reveal(val key: Any) : RevealEvent
    object Hide : RevealEvent
}