package com.rorokaiiworks.goodidlegame.ui.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.quests.Quest
import com.rorokaiiworks.goodidlegame.core.quests.QuestStatus.*
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.commons.RewardEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun QuestRewardsPanel(
    quest: Quest,
    i18n: I18n = koinInject(),
    onClaim: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BaseCard(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardTitle(title = i18n.tr("Rewards"))

            for (reward in quest.rewards) {
                RewardEntry(
                    modifier = Modifier.fillMaxWidth(),
                    reward = reward
                )
            }
        }

        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    if (quest.status == Completed) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                contentColor =
                    if (quest.status == Completed) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth(),
            onClick = onClaim,
        ) {
            Text(
                text = when (quest.status) {
                    Completed -> i18n.tr("Claim Reward")
                    RewardClaimed -> i18n.tr("Reward Claimed")
                    InProgress -> i18n.tr("In Progress")
                }
            )
        }
    }
}