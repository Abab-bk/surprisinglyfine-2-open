package com.rorokaiiworks.goodidlegame.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.prettyPrint
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType
import com.rorokaiiworks.goodidlegame.ui.commons.HighlightTextLabel
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun ItemModifiersPanel(
    modifier: Modifier = Modifier,
    defaultColor: Color = MaterialTheme.colorScheme.background,
    additionalColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    i18n: I18n = koinInject(),
    modifiers: List<StatModifier>,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (modifier in modifiers) {
            val modifierValue = when (modifier.type) {
                StatModifierType.Flat -> modifier.value.prettyPrint()
                StatModifierType.Percent -> "${(modifier.value * 100).prettyPrint()}%"
            }

            HighlightTextLabel(
                text = "+${modifierValue} ${i18n.trc("stat_id", modifier.statId)}",
                color = if (modifier.isAdditional) additionalColor else defaultColor
            )
        }
    }
}
