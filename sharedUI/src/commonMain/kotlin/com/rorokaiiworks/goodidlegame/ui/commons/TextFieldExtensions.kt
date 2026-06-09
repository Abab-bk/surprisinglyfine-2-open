package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable

@Composable
fun TextFieldDefaults.defaultColors(): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,

        focusedTextColor = MaterialTheme.colorScheme.onTertiary,
        unfocusedTextColor = MaterialTheme.colorScheme.onTertiary,
        disabledTextColor = MaterialTheme.colorScheme.onTertiary,

        cursorColor = MaterialTheme.colorScheme.onTertiary,
    )
}