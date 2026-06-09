package com.rorokaiiworks.goodidlegame.ui.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.requirements.Requirement
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.commons.RequirementEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun RequirementsPanel(
    requirements: List<Requirement>,
    i18n: I18n = koinInject(),
) {
    if (requirements.isEmpty()) {
        return
    }

    BaseCard(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CardTitle(title = i18n.tr("Requirements"))

        requirements.forEach {
            RequirementEntry(
                modifier = Modifier.fillMaxWidth(),
                requirement = it
            )
        }
    }
}
