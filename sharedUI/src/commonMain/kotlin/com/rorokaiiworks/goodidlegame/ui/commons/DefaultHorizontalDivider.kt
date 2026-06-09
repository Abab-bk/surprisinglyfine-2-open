package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DefaultHorizontalDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier,
        DividerDefaults.Thickness,
    )
}
