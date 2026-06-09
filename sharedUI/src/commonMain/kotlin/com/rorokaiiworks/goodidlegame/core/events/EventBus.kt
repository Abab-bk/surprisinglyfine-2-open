package com.rorokaiiworks.goodidlegame.core.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class EventBus {
    private val _events = MutableSharedFlow<IEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val events = _events.asSharedFlow()

    suspend fun emit(event: IEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: IEvent) {
        _events.tryEmit(event)
    }
}