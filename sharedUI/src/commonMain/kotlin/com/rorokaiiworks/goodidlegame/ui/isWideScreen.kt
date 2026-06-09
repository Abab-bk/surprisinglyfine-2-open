package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass

@Composable
fun isWideScreen(windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass) =
    windowSizeClass
        .isWidthAtLeastBreakpoint(
            WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
        )
