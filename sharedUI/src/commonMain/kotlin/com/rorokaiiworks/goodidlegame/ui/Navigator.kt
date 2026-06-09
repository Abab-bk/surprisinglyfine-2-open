package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.runtime.mutableStateListOf
import com.rorokaiiworks.goodidlegame.AppDestination

class Navigator {
    val backStack = mutableStateListOf<AppDestination>(AppDestination.JourneyDestination)

    val currentDestination: AppDestination get() = backStack.last()

    fun navigateTo(destination: AppDestination) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = destination
        } else {
            backStack.add(destination)
        }
    }
}