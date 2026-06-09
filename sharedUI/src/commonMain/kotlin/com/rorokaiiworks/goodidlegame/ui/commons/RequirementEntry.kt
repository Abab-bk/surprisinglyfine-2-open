package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.requirements.Requirement
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun RequirementEntry(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    requirement: Requirement
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HighlightNumbers(
                text = requirement.toText(i18n)
            )

            HighlightNumbers(
                text = requirement.progressText ?: "",
            )
        }
    }
}
