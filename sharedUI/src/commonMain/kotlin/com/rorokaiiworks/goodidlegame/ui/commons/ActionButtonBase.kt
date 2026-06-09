package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun ActionButtonBase(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    onClick: () -> Unit,
    isRemarkable: Boolean = false,
    shape: Shape = RectangleShape,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        color =
            if (isRemarkable) MaterialTheme.colorScheme.primaryContainer
            else if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            )
        ) {
            content()
        }
    }
}
