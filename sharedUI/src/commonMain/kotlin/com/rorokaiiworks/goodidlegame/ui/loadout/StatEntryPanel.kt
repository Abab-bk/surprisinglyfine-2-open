package com.rorokaiiworks.goodidlegame.ui.loadout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.prettyPrint
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import com.rorokaiiworks.goodidlegame.core.stats.Stat
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun StatEntryPanel(
    stat: Stat,
    effects: List<Effect>,
    i18n: I18n = koinInject()
) {
    var isExpanded by remember { mutableStateOf(false) }
    val effectEntries = effects.mapNotNull { effect ->
        val modifiers = effect.modifiers.filter { it.statId == stat.id }
        if (modifiers.isEmpty()) null else effect to modifiers
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = 6.dp,
                    bottom = 6.dp,
                )
        ) {
            Text(i18n.trc("stat_id", stat.id))
            Spacer(Modifier.weight(1f))
            Text(
                text = stat.value.prettyPrint(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, bottom = 8.dp),
            ) {
                effectEntries.forEach { (effect, modifiers) ->
                    val modifierText = modifiers.joinToString(" ") { formatModifier(it) }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = i18n.tr(effect.sourceName.sourceName),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = modifierText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun formatModifier(modifier: StatModifier): String {
    val sign = if (modifier.value >= 0f) "+" else ""
    val valueText = when (modifier.type) {
        StatModifierType.Flat -> modifier.value.prettyPrint()
        StatModifierType.Percent -> "${(modifier.value * 100).prettyPrint()}%"
    }
    return "$sign$valueText"
}
