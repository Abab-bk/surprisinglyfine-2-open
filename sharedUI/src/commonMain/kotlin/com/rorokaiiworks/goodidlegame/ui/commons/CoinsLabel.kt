package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rorokaiiworks.goodidlegame.IdleGameTheme
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer

@Composable
fun TopBarLabel(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(
                start = 8.dp,
                end = 8.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
fun CoinsLabel(
    modifier: Modifier = Modifier,
    value: Long,
    iconName: String = "coins"
) {
    TopBarLabel(modifier = modifier) {
        Text(
            text = Humanizer.abbreviation(value),
            fontSize = 20.sp,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        GameImage(
            modifier = Modifier.size(26.dp),
            iconName = iconName
        )
    }
}



@Composable
@Preview
private fun CoinsLabelPreview() {
    IdleGameTheme {
        CoinsLabel(value = 1000)
    }
}