package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val scale = remember { Animatable(1f) }

    return this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput

            detectTapGestures(
                onPress = {
                    scale.animateTo(
                        0.92f,
                        animationSpec = tween(120)
                    )

                    val success = tryAwaitRelease()

                    val target = if (success) 1.06f else 1f
                    scale.animateTo(
                        target,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        )
                    )
                    scale.animateTo(1f)
                    if (success) onClick()
                }
            )
        }
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
}
