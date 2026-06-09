package com.rorokaiiworks.goodidlegame.ui.combat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.enemies.EnemyTemplate
import com.rorokaiiworks.goodidlegame.ui.commons.HighlightTextLabel
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import java.text.DecimalFormat

@Composable
fun EnemyStatsPanel(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    enemyTemplate: EnemyTemplate) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val decimalFormat = DecimalFormat("###.#")

        HighlightTextLabel("${enemyTemplate.maxHealth} HP")
        HighlightTextLabel("${decimalFormat.format(enemyTemplate.attackSpeed)} ${i18n.tr("Attack Speed")}")

        if (enemyTemplate.slashDamage != 0) {
            HighlightTextLabel("${enemyTemplate.slashDamage} ${i18n.tr("Slash Damage")}")
        }

        if (enemyTemplate.punctureDamage != 0) {
            HighlightTextLabel("${enemyTemplate.punctureDamage} ${i18n.tr("Puncture Damage")}")
        }

        if (enemyTemplate.impactDamage != 0) {
            HighlightTextLabel("${enemyTemplate.impactDamage} ${i18n.tr("Impact Damage")}")
        }

        if (enemyTemplate.slashResistance != 0f) {
            HighlightTextLabel("${decimalFormat.format((enemyTemplate.slashResistance * 100f))}% ${i18n.tr("Slash Resistance")}")
        }

        if (enemyTemplate.punctureResistance != 0f) {
            HighlightTextLabel("${decimalFormat.format((enemyTemplate.punctureResistance * 100f))}% ${i18n.tr("Puncture Resistance")}")
        }

        if (enemyTemplate.impactResistance != 0f) {
            HighlightTextLabel("${decimalFormat.format((enemyTemplate.impactResistance * 100f))}% ${i18n.tr("Impact Resistance")}")
        }
    }
}
