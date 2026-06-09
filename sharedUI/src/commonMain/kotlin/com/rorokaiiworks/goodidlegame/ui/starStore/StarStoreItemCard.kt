package com.rorokaiiworks.goodidlegame.ui.starStore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.starStore.StarStoreItem
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Youtube

@Composable
fun StarStoreItemCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    starStoreItem: StarStoreItem
) {
    BaseCard(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.clickable(onClick = onClick),
        padding = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameImage(
                modifier = Modifier
                    .weight(0.2f)
                    .fillMaxSize(),
                iconName = starStoreItem.id
            )

            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .padding(horizontal = 8.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (starStoreItem.isAdNeeded) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            imageVector = Feather.Youtube,
                            contentDescription = null
                        )
                    } else {
                        GameImage(
                            modifier = Modifier.size(24.dp),
                            iconName = "star"
                        )
                    }

                    Text(
                        text = starStoreItem.price.toString(),
                    )
                }

                Text(
                    text = starStoreItem.name,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
