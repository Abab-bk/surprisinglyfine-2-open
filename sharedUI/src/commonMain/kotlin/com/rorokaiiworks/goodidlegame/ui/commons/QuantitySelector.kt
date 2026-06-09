package com.rorokaiiworks.goodidlegame.ui.commons

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.composables.icons.feather.Feather
import com.composables.icons.feather.X
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.shop.ShopItem
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.ui.OnlyNumbersInputTransformation
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemModifiersPanel
import com.rorokaiiworks.goodidlegame.ui.parseLong
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

sealed class QuantitySelectorConfig {
    abstract val title: String
    abstract val iconId: String
    abstract val actionButtonText: String
    abstract val modifiers: List<StatModifier>?
    abstract val background: Modifier?
    abstract val desc: String?

    data class OwnedItems(
        override val title: String,
        override val iconId: String,
        override val actionButtonText: String,
        override val modifiers: List<StatModifier>? = null,
        val ownedCount: Long,
        val pricePerUnit: Long,
        override val background: Modifier?,
        override val desc: String?,
    ) : QuantitySelectorConfig()

    data class Purchasable(
        override val title: String,
        override val iconId: String,
        override val actionButtonText: String,
        override val modifiers: List<StatModifier>? = null,
        val ownedCount: Long,
        val playerCoins: Long,
        val calculatePrice: (quantity: Long) -> Long,
        val calculateMaxAffordable: (playerCoins: Long) -> Long,
        override val background: Modifier? = Modifier,
        override val desc: String? = null,
    ) : QuantitySelectorConfig()
}

data class InfoRow(
    val label: String,
    val value: String
)


@Composable
fun QuantitySelector(
    config: QuantitySelectorConfig,
    i18n: I18n = koinInject(),
    inputState: TextFieldState,
    onAction: (ItemEntry) -> Unit,
    onClose: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit = {}
) {
    val maxQuantity = when (config) {
        is QuantitySelectorConfig.OwnedItems -> config.ownedCount
        is QuantitySelectorConfig.Purchasable -> {
            config.calculateMaxAffordable(config.playerCoins)
        }
    }

    val infoRows = remember(inputState.text, config) {
        buildInfoRows(config, inputState.parseLong(), i18n)
    }

    BaseCard(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HeaderRow(
            modifier = config.background ?: Modifier,
            title = config.title,
            iconId = config.iconId,
            onClose = onClose
        )

        config.desc?.let {
            Text(
                text = i18n.tr(it),
                modifier = Modifier.fillMaxWidth()
            )
        }

        config.modifiers?.let {
            ItemModifiersPanel(modifiers = it)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            infoRows.forEach { row ->
                DefaultHorizontalDivider()
                DisplayTextPair(row.label, row.value)
            }
            DefaultHorizontalDivider()
        }

        TextField(
            modifier = Modifier.fillMaxWidth(),
            state = inputState,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            inputTransformation = InputTransformation
                .maxLength(8)
                .then(OnlyNumbersInputTransformation),
            placeholder = { Text(i18n.tr("Quantity")) }
        )

        if (enabled) {
            ActionButtons(
                actionButtonText = config.actionButtonText,
                onSelectAll = { inputState.setTextAndPlaceCursorAtEnd(maxQuantity.toString()) },
                onSelectAllButOne = {
                    val selectedQuantity = (maxQuantity - 1).coerceAtLeast(0)
                    inputState.setTextAndPlaceCursorAtEnd(selectedQuantity.toString())
                },
                onAction = {
                    onAction(ItemEntry(config.iconId, inputState.parseLong()))
                },
                i18n = i18n
            )
        }

        content()
    }
}

@Composable
private fun HeaderRow(
    modifier: Modifier = Modifier,
    title: String,
    iconId: String,
    onClose: () -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.width(4.dp))
            GameImage(
                iconName = iconId,
                modifier = Modifier.size(30.dp)
            )
            Text(title)
        }

        IconButton(onClick = onClose) {
            Icon(imageVector = Feather.X, contentDescription = null)
        }
    }
}

//@Composable
//private fun QuantityTextField(
//    quantity: Long,
//    onQuantityChange: (Long) -> Unit,
//    i18n: I18n
//) {
//    TextField(
//        modifier = Modifier.fillMaxWidth(),
//        placeholder = {
//            Text(
//                text = i18n.tr("Quantity")
////                color = MaterialTheme.colorScheme.onTertiary
//            )
//        },
//        value = quantity,
//        onValueChange = { newValue ->
//            if (newValue.isEmpty()) {
//                textValue = "" // TODO: FIX IT
//                onQuantityChange(0)
//                return@TextField
//            }
//            val count = newValue.toLongOrNull()
//            if (count != null) {
//                textValue = newValue
//                onQuantityChange(count)
//            }
//        },
//        keyboardOptions = KeyboardOptions.Default.copy(
//            keyboardType = KeyboardType.Number
//        ),
//    )
//}

@Composable
private fun ActionButtons(
    actionButtonText: String,
    onSelectAll: () -> Unit,
    onSelectAllButOne: () -> Unit,
    onAction: () -> Unit,
    i18n: I18n
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(4.dp),
            onClick = onSelectAll
        ) {
            Text(i18n.tr("Max Count"))
        }

        Button(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(4.dp),
            onClick = onSelectAllButOne
        ) {
            Text(i18n.tr("1 Left"))
        }

        Button(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(4.dp),
            onClick = onAction
        ) {
            Text(actionButtonText)
        }
    }
}

@Composable
private fun DisplayTextPair(title: String, value: String) {
    TextPair(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp),
        title = { Text(text = title) }
    ) {
        Text(value)
    }
}


private fun buildInfoRows(
    config: QuantitySelectorConfig,
    selectedQuantity: Long,
    i18n: I18n
): List<InfoRow> {
    return when (config) {
        is QuantitySelectorConfig.OwnedItems -> listOf(
            InfoRow(
                i18n.tr("Owned"),
                Humanizer.abbreviation(config.ownedCount)
            ),
            InfoRow(
                i18n.tr("Unit Price"),
                Humanizer.abbreviation(config.pricePerUnit)
            ),
            InfoRow(
                i18n.tr("Total Price"),
                Humanizer.abbreviation(config.pricePerUnit * selectedQuantity)
            )
        )

        is QuantitySelectorConfig.Purchasable -> listOf(
            InfoRow(
                i18n.tr("Owned"),
                Humanizer.abbreviation(config.ownedCount)
            ),
            InfoRow(
                i18n.tr("Unit Price"),
                Humanizer.abbreviation(config.calculatePrice(1))
            ),
            InfoRow(
                i18n.tr("Total Price"),
                Humanizer.abbreviation(config.calculatePrice(selectedQuantity))
            )
        )
    }
}

@Composable
fun ShopItemPurchasePanel(
    selectedItem: ShopItem,
    playerCoins: Long,
    inputState: TextFieldState,
    i18n: I18n = koinInject(),
    onPerformPurchase: (ShopItem, Long) -> Unit,
    onClose: () -> Unit
) {
    val config = QuantitySelectorConfig.Purchasable(
        title = selectedItem.getName(),
        iconId = selectedItem.iconId,
        actionButtonText = i18n.tr("Buy"),
        modifiers = null,
        ownedCount = selectedItem.getOwnedCount(),
        playerCoins = playerCoins,
        calculatePrice = { quantity -> selectedItem.getPrice(count = quantity) },
        calculateMaxAffordable = { coins ->
            // 二分
            var left = 0L
            var right = 1000L
            var result = 0L

            while (left <= right) {
                val mid = (left + right) / 2
                val price = selectedItem.getPrice(count = mid)

                if (price <= coins) {
                    result = mid
                    left = mid + 1
                } else {
                    right = mid - 1
                }
            }
            result
        }
    )

    QuantitySelector(
        config = config,
        onAction = { itemEntry ->
            onPerformPurchase(selectedItem, itemEntry.count)
        },
        onClose = onClose,
        inputState = inputState
    )
}