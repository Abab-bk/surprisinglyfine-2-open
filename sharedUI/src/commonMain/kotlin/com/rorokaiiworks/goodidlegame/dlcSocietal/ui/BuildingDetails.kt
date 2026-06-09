package com.rorokaiiworks.goodidlegame.dlcSocietal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.Building
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitleWithCloseBtn
import kotlin.math.roundToInt

@Composable
fun BuildingDetails(building: Building, onClose: () -> Unit) {
    BaseCard(modifier = Modifier.verticalScroll(rememberScrollState())) {
        CardTitleWithCloseBtn(
            title = building.template.name,
            onClose = onClose
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Building Status",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = "Total: ${building.count}")
                    Text(text = "Active: ${building.activeCount}")
                    Text(text = "Stopped: ${building.inactiveCount}")
                }
            }

            Surface {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Stopped Buildings",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = "${building.inactiveCount} / ${building.count}")

                    Slider(
                        value = building.inactiveCount.toFloat(),
                        onValueChange = { building.updateInactiveCount(it.roundToInt()) },
                        valueRange = 0f..building.count.toFloat(),
                        steps = (building.count - 1).coerceAtLeast(0),
                    )
                }
            }
        }
    }
}

