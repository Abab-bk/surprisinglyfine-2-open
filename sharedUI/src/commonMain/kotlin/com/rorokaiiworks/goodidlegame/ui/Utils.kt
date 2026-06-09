@file:OptIn(ExperimentalFoundationApi::class)

package com.rorokaiiworks.goodidlegame.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

fun i18nWrapper(string: String): String = string
fun i18nWrapperContext(context: String, string: String): String = string


fun Modifier.grayScale(): Modifier {
    val saturationMatrix = ColorMatrix().apply { setToSaturation(0f) }
    val saturationFilter = ColorFilter.colorMatrix(saturationMatrix)
    val paint = Paint().apply { colorFilter = saturationFilter }

    return drawWithCache {
        val canvasBounds = Rect(Offset.Zero, size)
        onDrawWithContent {
            drawIntoCanvas {
                it.saveLayer(canvasBounds, paint)
                drawContent()
                it.restore()
            }
        }
    }
}

object OnlyNumbersInputTransformation : InputTransformation {
    override fun TextFieldBuffer.transformInput() {
        for (char in asCharSequence()) {
            if (!char.isDigit()) {
                revertAllChanges()
                return
            }
        }
    }
}

fun TextFieldState.parseLong(): Long {
    if (text.isEmpty()) return 0L
    return text.toString().toLong()
}