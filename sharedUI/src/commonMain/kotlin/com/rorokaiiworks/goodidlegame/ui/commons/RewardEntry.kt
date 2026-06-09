package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.rewards.Reward

@Composable
fun RewardEntry(
    modifier: Modifier = Modifier,
    reward: Reward,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GameImage(
                modifier = Modifier.size(32.dp),
                iconName = reward.getIconName(),
            )

            Text(
                text = reward.getDescription(),
            )
        }
    }
}

