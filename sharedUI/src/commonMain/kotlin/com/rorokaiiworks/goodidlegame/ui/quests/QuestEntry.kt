package com.rorokaiiworks.goodidlegame.ui.quests

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.Constants
import com.rorokaiiworks.goodidlegame.core.quests.Quest
import com.rorokaiiworks.goodidlegame.core.quests.QuestStatus
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage

@Composable
fun QuestEntry(
    quest: Quest,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        selected = isSelected,
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(
                min = 48.dp,
                max = 48.dp
            ),
        shape = RectangleShape,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (quest.status == QuestStatus.Completed) {
                            Badge(
                                modifier = Modifier.size(12.dp),
                                containerColor = Constants.NoticeColor
                            )
                        }
                    }
                ) {
                    GameImage(
                        Modifier.size(30.dp),
                        quest.getIconName()
                    )
                }

                Text(
                    text = quest.tryGetName(),
                    color = when (quest.status) {
                        QuestStatus.InProgress -> MaterialTheme.colorScheme.onSurface
                        QuestStatus.Completed -> MaterialTheme.colorScheme.onPrimary
                        QuestStatus.RewardClaimed -> MaterialTheme.colorScheme.onTertiary
                    },
                    style = TextStyle(
                        textDecoration = if (quest.status == QuestStatus.RewardClaimed) TextDecoration.LineThrough else null
                    )
                )
            }
        }
    }
}
