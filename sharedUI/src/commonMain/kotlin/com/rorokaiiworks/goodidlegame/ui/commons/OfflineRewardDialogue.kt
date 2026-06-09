package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.offline.OfflineReward
import com.rorokaiiworks.goodidlegame.core.offline.OfflineRewardEntry
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun OfflineRewardDialogue(
    offlineReward: OfflineReward,
    i18n: I18n = koinInject(),
    onClick: (OfflineReward) -> Unit
) {
    val offlineDurationString = Humanizer.duration(offlineReward.offlineTime)

    GameDialog(
        title = i18n.tr("Offline Rewards"),
        onDismissRequest = { onClick(offlineReward) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HighlightNumbers(
                text = i18n.tr("You have been offline for {0}.", offlineDurationString),
            )

            offlineReward.entries.forEach { entry ->
                OfflineEntrySection(
                    entry = entry,
                    i18n = i18n
                )
            }
        }
    }
}

@Composable
private fun OfflineEntrySection(
    skillTemplates: DataTable<SkillTemplate> = koinInject(named<SkillTemplate>()),
    entry: OfflineRewardEntry,
    i18n: I18n
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val skillTemplate = skillTemplates.find(entry.skillId)
            Text(
                text = i18n.tr(skillTemplate.name),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            if (entry.skillXp > 0) {
                Text(
                    text = "+${entry.skillXp} XP",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        if (entry.items.isNotEmpty()) {
            val mergedItems = entry.items.groupBy { it.template.id }
                .map { (_, list) -> list.first().copy(count = list.sumOf { it.count }) }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                mergedItems.forEach { item ->
                    ItemTemplateEntry(itemTemplate = item.template) {
                        Text(text = "x${item.count}")
                    }
                }
            }
        }

        DefaultHorizontalDivider()
    }
}
