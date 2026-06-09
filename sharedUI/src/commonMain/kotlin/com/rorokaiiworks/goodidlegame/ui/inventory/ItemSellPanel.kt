@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.ui.inventory

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.settings.ThemePreference.Companion.isDark
import com.rorokaiiworks.goodidlegame.ui.commons.QuantitySelector
import com.rorokaiiworks.goodidlegame.ui.commons.QuantitySelectorConfig
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import kotlin.uuid.ExperimentalUuidApi

@Composable
fun ItemSellPanel(
    i18n: I18n = koinInject(),
    playerInventory: PlayerInventory = koinInject(),
    settingsSaver: SettingsSaver = koinInject(),
    inputState: TextFieldState,
    ownedItem: Item,
    onSell: (Item) -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    val config = QuantitySelectorConfig.OwnedItems(
        title = ownedItem.displayName,
        iconId = ownedItem.template.id,
        actionButtonText = if (ownedItem.isLocked) {
            i18n.tr("Item is locked")
        } else {
            i18n.tr("Sell")
        },
        modifiers = ownedItem.allModifiers,
        ownedCount = ownedItem.count,
        pricePerUnit = ownedItem.template.price,
        background = Modifier.rarityBackground(
            ownedItem.template.rarity,
            isDark = settingsSaver.settings.value.themePreference.isDark()
        ),
        desc = ownedItem.template.perk?.desc
    )

    QuantitySelector(
        config = config,
        enabled = ownedItem.template.canSell &&
                ownedItem.template.price > 0 &&
                !ownedItem.isLocked,
        onAction = { itemEntry ->
            if (itemEntry.count <= 0) return@QuantitySelector

            val sellItem = Item(
                template = ownedItem.template,
                count = itemEntry.count
            )

            playerInventory.inventory.removeItem(sellItem)
            playerInventory.addCoins(ownedItem.template.price * itemEntry.count)

            onSell(sellItem)
        },
        onClose = onClose,
        inputState = inputState,
        content = content
    )
}