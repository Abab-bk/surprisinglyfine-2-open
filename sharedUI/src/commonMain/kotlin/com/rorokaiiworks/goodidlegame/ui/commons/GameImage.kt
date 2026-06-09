package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.graphicsLayer
import org.jetbrains.compose.resources.imageResource
import goodidlegame.sharedui.generated.resources.Res
import goodidlegame.sharedui.generated.resources.allDrawableResources
import goodidlegame.sharedui.generated.resources.default

@Composable
fun GameImage(
    modifier: Modifier = Modifier,
    iconName: String,
    colorFilter: ColorFilter? = null
) {
    val image = imageResource(Res.allDrawableResources[iconName] ?: Res.drawable.default)
    val painter = BitmapPainter(
        image = image,
        filterQuality = FilterQuality.None,
    )

    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier,
        colorFilter = colorFilter
    )
}

@Composable
fun ProgressGameImage(
    modifier: Modifier = Modifier,
    iconName: String,
    progress: Float,
    grayscaleAlpha: Float = 0.85f,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    val image = imageResource(Res.allDrawableResources[iconName] ?: Res.drawable.default)
    val painter = BitmapPainter(
        image = image,
        filterQuality = FilterQuality.None,
    )

    val grayFilter = ColorFilter.colorMatrix(
        ColorMatrix().apply { setToSaturation(0f) }
    )

    Box(modifier = modifier.clipToBounds()) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(alpha = grayscaleAlpha),
            colorFilter = grayFilter,
        )

        if (clampedProgress > 0f) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        clipRect(right = size.width * clampedProgress) {
                            this@drawWithContent.drawContent()
                        }
                    },
            )
        }
    }
}
