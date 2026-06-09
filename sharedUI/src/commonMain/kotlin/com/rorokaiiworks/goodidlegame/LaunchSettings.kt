package com.rorokaiiworks.goodidlegame

data class LaunchSettings(
    val skipStartMenu: Boolean = false,
    val disabledSaving: Boolean = false,
    val lockSkills: Boolean = true,
    val skipBootAnimation: Boolean = false,
    val debugMenu: Boolean = false,
    val mockNoDLCUnlocked: Boolean = false,
)
