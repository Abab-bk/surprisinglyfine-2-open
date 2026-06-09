package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun BaseCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    padding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface (
//        shadowElevation = 3.dp,
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column (
            modifier = Modifier.padding(padding),
            verticalArrangement = verticalArrangement
        ) {
            content()
        }
    }
}