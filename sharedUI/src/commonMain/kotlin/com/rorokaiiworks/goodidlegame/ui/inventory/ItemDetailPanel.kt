package com.rorokaiiworks.goodidlegame.ui.inventory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemRarity
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.settings.ThemePreference.Companion.isDark
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun ItemDetailPanel(
    modifier: Modifier = Modifier,
    additionalModifiersColor: Color = MaterialTheme.colorScheme.primaryContainer,
    i18n: I18n = koinInject(),
    title: @Composable () -> Unit = { },
    item: Item?,
    content: @Composable () -> Unit = { },
) {
    GenericItemDetailPanel(
        modifier = modifier,
        i18n = i18n,
        title = title,
        name = item?.displayName ?: "",
        rarity = item?.template?.rarity ?: ItemRarity.Common,
        iconId = item?.template?.id,
        modifiers = item?.allModifiers,
        desc = item?.template?.perk?.desc,
        additionalModifiersColor = additionalModifiersColor,
        content = content
    )
}

@Composable
fun ItemTemplateDetailPanel(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    title: @Composable () -> Unit = { },
    item: ItemTemplate?,
    content: @Composable () -> Unit = { },
) {
    GenericItemDetailPanel(
        modifier = modifier,
        i18n = i18n,
        title = title,
        rarity = item?.rarity ?: ItemRarity.Common,
        name = item?.name,
        iconId = item?.id,
        modifiers = item?.modifiers,
        content = content
    )
}


@Composable
private fun GenericItemDetailPanel(
    modifier: Modifier = Modifier,
    settingsSaver: SettingsSaver = koinInject(),
    i18n: I18n,
    title: @Composable () -> Unit,
    name: String?,
    iconId: String?,
    rarity: ItemRarity,
    modifiers: List<StatModifier>? = null,
    desc: String? = null,
    additionalModifiersColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    val isDark = settingsSaver.settings.value.themePreference.isDark()

    BaseCard(modifier = modifier) {
        title()

        if (name == null || iconId == null) {
            Text(
                text = i18n.tr("Empty"),
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = name,
                modifier = Modifier.fillMaxWidth()
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .rarityBackground(rarity = rarity, isDark = isDark)
            ) {
                GameImage(
                    modifier = Modifier.fillMaxSize(),
                    iconName = iconId
                )
            }

            desc?.let {
                Text(
                    text = i18n.tr(it),
                )
            }

            if (!modifiers.isNullOrEmpty()) {
                ItemModifiersPanel(
                    additionalColor = additionalModifiersColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    modifiers = modifiers
                )
            }
        }

        content()
    }
}