package com.rorokaiiworks.goodidlegame.dlcSocietal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.City
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations.PopulationTier
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun CityTopBar(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    city: City = koinInject(),
) {
    val cityStats = city.stats

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Entry(
            title = "Isle Bucks",
            iconId = "isle_bucks",
            text = Humanizer.abbreviation(cityStats.isleBucks),
        )

        PopulationTier.entries.forEach { tier ->
            Entry(
                title = tier.title,
                iconId = tier.id,
                text = Humanizer.abbreviation(cityStats.populationByTier.getValue(tier).current),
            )
        }

        Entry(
            title = i18n.tr("Balance"),
            iconId = "balance",
            text = Humanizer.abbreviation(cityStats.balance),
        )

        EntryWithBackground(
            title = "",
            text = i18n.tr("Maintenance: {0}s", cityStats.secondsUntilSettlement),
            iconId = null,
        )

        EntryWithBackground(
            title = "",
            text = i18n.tr("Trade: {0}", Humanizer.duration(city.cityPort.nextTradeTimeDistance)),
            iconId = null,
        )
    }
}


@Composable
private fun EntryWithBackground(
    title: String,
    iconId: String?,
    text: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(4.dp),
    ) {
        Entry(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            title = title,
            iconId = iconId,
            text = text,
        )
    }
}


@Composable
private fun Entry(
    modifier: Modifier = Modifier,
    title: String,
    iconId: String?,
    text: String,
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        iconId?.let {
            GameImage(
                modifier = Modifier.size(24.dp),
                iconName = it
            )
        }

        if (windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND)) {
            Text(
                text = "$title: $text",
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
