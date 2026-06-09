package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun LootPanel(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    titleContent: @Composable () -> Unit,
    items: List<Item>
) {
    BaseCard(modifier = modifier) {
        CardTitle(
            title = i18n.tr("Loot"),
            content = titleContent
        )

        for (item in items) {
            ItemTemplateEntry(
                itemTemplate = item.template,
                titleLabel = {
                    Text(text = Humanizer.abbreviation(item.count))
                }
            ) {
                Text(
                    text = Humanizer.abbreviation(item.template.price * item.count)
                )

                GameImage(
                    modifier = Modifier.size(30.dp),
                    iconName = "coins"
                )
            }
        }
    }
}