package com.rorokaiiworks.goodidlegame.ui.journey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.journey.JourneySystem
import com.rorokaiiworks.goodidlegame.core.quests.Quest
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.isWideScreen
import com.rorokaiiworks.goodidlegame.ui.quests.QuestPanel
import com.rorokaiiworks.goodidlegame.ui.quests.QuestRewardsPanel
import com.rorokaiiworks.goodidlegame.ui.quests.RequirementsPanel

@Composable
fun JourneyScreen(journeySystem: JourneySystem) {
    if (journeySystem.currentQuest == null) {
        NoQuest()
    } else {
        CurrentQuest(
            quest = journeySystem.currentQuest!!,
            onClaim = journeySystem::claimQuest
        )
    }
}

@Composable
private fun NoQuest() {
    BaseCard {
        Text(
            text = "No Quest",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}


@Composable
private fun CurrentQuest(
    quest: Quest,
    onClaim: () -> Unit,
) {
    if (isWideScreen()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuestPanel(
                    quest = quest,
                    showTip = false
                )

                RequirementsPanel(
                    requirements = quest.requirements
                )
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                QuestRewardsPanel(
                    quest = quest,
                    onClaim = onClaim
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QuestPanel(
            quest = quest,
            showTip = false
        )
        
        RequirementsPanel(
            requirements = quest.requirements
        )

        QuestRewardsPanel(
            quest = quest,
            onClaim = onClaim
        )
    }
}