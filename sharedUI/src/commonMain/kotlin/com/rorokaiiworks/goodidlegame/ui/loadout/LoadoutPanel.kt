package com.rorokaiiworks.goodidlegame.ui.loadout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.items.ItemSlot
import com.rorokaiiworks.goodidlegame.core.loadouts.Loadout
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.CardTitle
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun LoadoutPanel(
    loadout: Loadout,
    i18n: I18n = koinInject(),
    onClick: (ItemSlot) -> Unit,
    isWideScreen: Boolean
) {
    BaseCard(
        modifier = Modifier.shadow(
            elevation = 2.dp
        ),
    ) {
        CardTitle(title = i18n.tr(loadout.name))

        if (isWideScreen) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = {
                    items(loadout.slots) { itemSlot ->
                        ItemSlotPanel(itemSlot, onClick)
                    }
                }
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (itemSlot in loadout.slots) {
                    ItemSlotPanel(itemSlot, onClick)
                }
            }
        }
    }
}
