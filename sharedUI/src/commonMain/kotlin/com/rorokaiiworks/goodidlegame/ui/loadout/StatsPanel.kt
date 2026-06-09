package com.rorokaiiworks.goodidlegame.ui.loadout

import androidx.compose.runtime.Composable
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import com.rorokaiiworks.goodidlegame.core.stats.StatSet
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun StatSetPanel(
    statSet: StatSet,
    effects: List<Effect>,
) {
    BaseCard {
        for (stat in statSet.stats.values) {
            StatEntryPanel(stat, effects)
        }
    }
}
