package com.rorokaiiworks.goodidlegame.ui.recipes

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rorokaiiworks.goodidlegame.IdleGameTheme
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.recipes.CraftResult
import com.rorokaiiworks.goodidlegame.core.recipes.Recipe
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.settings.ThemePreference.Companion.isDark
import com.rorokaiiworks.goodidlegame.ui.PreviewConstants
import com.rorokaiiworks.goodidlegame.ui.commons.QuantitySelector
import com.rorokaiiworks.goodidlegame.ui.commons.QuantitySelectorConfig
import com.rorokaiiworks.goodidlegame.ui.inventory.rarityBackground
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun RecipeItemSelectPanel(
    ownedItem: Item,
    i18n: I18n = koinInject(),
    settingsSaver: SettingsSaver = koinInject(),
    recipe: Recipe,
    onCraft: (CraftResult) -> Unit,
    onClose: () -> Unit
) {
    val config = QuantitySelectorConfig.OwnedItems(
        title = ownedItem.displayName,
        iconId = ownedItem.template.id,
        actionButtonText = i18n.tr("Create"),
        modifiers = null,
        ownedCount = ownedItem.count,
        pricePerUnit = recipe.required.count,
        background = Modifier.rarityBackground(
            ownedItem.template.rarity,
            isDark = settingsSaver.settings.value.themePreference.isDark()
        ),
        desc = null
    )

    QuantitySelector(
        config = config,
        onAction = { itemEntry ->
            onCraft(
                CraftResult(
                    consume = itemEntry,
                    product = ItemEntry(
                        recipe.product.itemId,
                        recipe.required.count * itemEntry.count
                    )
                )
            )
        },
        onClose = onClose,
        inputState = rememberTextFieldState()
    )
}


@Composable
@Preview
private fun RecipeItemSelectPanelPreview() {
    IdleGameTheme { 
        RecipeItemSelectPanel(
            ownedItem = PreviewConstants.testItem,
            recipe = PreviewConstants.testRecipe,
            onCraft = { },
            onClose = { }
        )
    }
}