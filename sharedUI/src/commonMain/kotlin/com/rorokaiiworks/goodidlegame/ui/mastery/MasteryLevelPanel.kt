package com.rorokaiiworks.goodidlegame.ui.mastery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rorokaiiworks.goodidlegame.IdleGameTheme
import com.rorokaiiworks.goodidlegame.core.mastery.MasteryLevel
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun MasteryLevelPanel(
    modifier: Modifier = Modifier,
    masteryLevel: MasteryLevel,
    i18n: I18n = koinInject(),
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Lv. ${masteryLevel.level}",
                fontWeight = FontWeight.Bold
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = i18n.tr("Mastery Level"),
                fontSize = 12.sp
            )
        }

        LinearProgressIndicator(
            modifier = Modifier.height(16.dp),
            progress =  { masteryLevel.currentXp.toFloat() / masteryLevel.maxXp.toFloat() }
        )
    }
}


@Composable
@Preview
private fun MasteryLevelPanelPreview() {
    IdleGameTheme {
        BaseCard {
            MasteryLevelPanel(
                masteryLevel = MasteryLevel()
            )
        }
    }
}