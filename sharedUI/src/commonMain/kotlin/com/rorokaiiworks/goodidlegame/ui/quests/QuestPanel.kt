package com.rorokaiiworks.goodidlegame.ui.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.quests.Quest
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.commons.RequirementEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun QuestPanel(
    i18n: I18n = koinInject(),
    quest: Quest,
    showTip: Boolean
) {
    BaseCard(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CardTitle(title = i18n.tr("Quest"))

        GameImage(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            iconName = quest.getIconName()
        )

        for (condition in quest.conditions) {
             RequirementEntry(
                modifier = Modifier.fillMaxWidth(),
                requirement = condition
            )
        }

//        if (showTip) {
//            Text(
//                modifier = Modifier.padding(top = 16.dp),
//                text = stringResource(Res.string.max_finished_count_tip),
//                color = MaterialTheme.colorScheme.onSurface
//            )
//        }
    }
}
