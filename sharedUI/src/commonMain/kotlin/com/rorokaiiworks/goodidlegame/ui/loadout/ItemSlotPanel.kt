package com.rorokaiiworks.goodidlegame.ui.loadout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Lock
import com.rorokaiiworks.goodidlegame.core.items.ItemRarity
import com.rorokaiiworks.goodidlegame.core.items.ItemSlot
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.settings.ThemePreference.Companion.isDark
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.inventory.rarityBackground
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun ItemSlotPanel(
    itemSlot: ItemSlot,
    onClick: (ItemSlot) -> Unit,
    i18n: I18n = koinInject(),
    settingsSaver: SettingsSaver = koinInject(),
    columnContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    Surface (
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).rarityBackground(
            rarity = itemSlot.item?.template?.rarity ?: ItemRarity.Common,
            isDark = settingsSaver.settings.value.themePreference.isDark()
        ),
        onClick = {
            if (itemSlot.isLocked) return@Surface
            onClick(itemSlot) },
        shape = RectangleShape,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GameImage(
                        iconName = itemSlot.item?.template?.id ?: itemSlot.id,
                        modifier = Modifier.size(32.dp)
                    )

                    Text(
                        text = i18n.tr(itemSlot.item?.displayName ?: itemSlot.name),
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                content()

                if (itemSlot.isLocked) {
                    Icon(
                        imageVector = Feather.Lock,
                        contentDescription = "Locked",
                    )
                }
            }

            columnContent()
        }
    }
}


@Composable
fun PropSlotPanel(
    name: String,
    iconName: String,
    onClick: () -> Unit,
    i18n: I18n = koinInject(),
    content: @Composable () -> Unit = {}
) {
    Surface (
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        onClick = { onClick() },
        shape = RectangleShape,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            )
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GameImage(
                    iconName = iconName,
                    modifier = Modifier.size(32.dp)
                )

                Text(
                    text = i18n.tr(name),
                    textAlign = TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            content()
        }
    }
}
