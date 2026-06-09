package com.rorokaiiworks.goodidlegame.ui.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.ktx.harmonize
import com.rorokaiiworks.goodidlegame.core.Constants
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.drops.DropTable
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun DropTablePanel(
    modifier: Modifier = Modifier,
    dropTable: DropTable,
    i18n: I18n = koinInject(),
    itemTemplates: DataTable<ItemTemplate> = koinInject(named<ItemTemplate>()),
) {
    BaseCard(
        modifier = modifier
    ) {
        CardTitle(title = i18n.tr("Drops"))

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (entry in dropTable.entries) {
                val item = itemTemplates.find(entry.itemId)
                ItemTemplateEntry(
                    itemTemplate = item,
                    titleLabel = @Composable {
                        val text = if (entry.max == entry.min) {
                            "${entry.min}"
                        } else {
                            "${entry.min} - ${entry.max}"
                        }

                        if (entry.max > 1) {
                            Text(
                                text = text,
                            )
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val isDarkTheme = false

                        val rarityColor = when {
                            entry.isAlways -> if (isDarkTheme) Constants.RarityAlwaysColorDark else Constants.RarityAlwaysColorLight
                            entry.inCommon -> if (isDarkTheme) Constants.RarityCommonColorDark else Constants.RarityCommonColorLight
                            entry.inUncommon -> if (isDarkTheme) Constants.RarityUnCommonColorDark else Constants.RarityUnCommonColorLight
                            entry.inRare -> if (isDarkTheme) Constants.RarityRareColorDark else Constants.RarityRareColorLight
                            entry.inLegendary -> if (isDarkTheme) Constants.RarityLegendaryColorDark else Constants.RarityLegendaryColorLight
                            else -> if (isDarkTheme) Constants.RarityAlwaysColorDark else Constants.RarityAlwaysColorLight
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            GameImage(modifier = Modifier.size(32.dp), iconName = "coins")
                            Text(Humanizer.abbreviation(item.price))
                        }

                        Text(
                            text = entry.label,
                            color = rarityColor.harmonize(
                                matchSaturation = true,
                                other = MaterialTheme.colorScheme.primary
                            ),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
