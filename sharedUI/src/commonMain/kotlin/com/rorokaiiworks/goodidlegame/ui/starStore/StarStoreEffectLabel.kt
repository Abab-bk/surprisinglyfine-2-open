package com.rorokaiiworks.goodidlegame.ui.starStore

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.starStore.StarStoreItem
import com.rorokaiiworks.goodidlegame.ui.commons.TextLabel
import com.rorokaiiworks.goodidlegame.ui.commons.TextPair
import kotlin.time.toDuration

@Composable
fun StarStoreEffectLabel(
    starStoreItem: StarStoreItem
) {
    TextLabel(
        modifier = Modifier.height(48.dp)
    ) {
        TextPair(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),
            title = {
                Text(
                    text = starStoreItem.name,
                    color = MaterialTheme.colorScheme.onSurface
                ) },
            value = { Text(
                text = Humanizer.duration(
                    duration = starStoreItem
                        .remain
                        .toInt()
                        .toDuration(unit = kotlin.time.DurationUnit.SECONDS)
                ),
                color = MaterialTheme.colorScheme.onSurface
            ) }
        )
    }
}
