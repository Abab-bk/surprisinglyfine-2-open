package com.rorokaiiworks.goodidlegame.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.ItemRarity
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.settings.ThemePreference.Companion.isDark
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject


@Composable
fun ItemRowSurface(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    rarity: ItemRarity,
    settingsSaver: SettingsSaver = koinInject(),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .rarityBackground(
                rarity = rarity,
                isDark = settingsSaver.settings.value.themePreference.isDark()
            ),
        color = Color.Transparent,
        shape = RectangleShape,
        border = if (isSelected) selectedItemEntryBorder() else null,
        content = content
    )
}


@Composable
fun ItemRow(
    modifier: Modifier = Modifier,
    name: String,
    count: Long?,
    rarity: ItemRarity,
    iconId: String,
    price: Long,
    priceIcon: String = "coins",
    i18n: I18n = koinInject(),
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit = { },
) {
    ItemRowSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        isSelected = isSelected,
        rarity = rarity,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameImage(
                    modifier = Modifier.size(32.dp),
                    iconName = iconId
                )

                Text(
                    text = i18n.tr(name),
                )
            }

            count?.let {
                Text(
                    text = "x${Humanizer.abbreviation(it)}",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                if (price > 0) {
                    Text(
                        text = Humanizer.abbreviation(price),
                    )

                    GameImage(
                        modifier = Modifier.size(24.dp),
                        iconName = priceIcon
                    )
                }
            }

            content()
        }
    }
}