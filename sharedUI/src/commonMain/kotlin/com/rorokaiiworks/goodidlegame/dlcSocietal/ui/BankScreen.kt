package com.rorokaiiworks.goodidlegame.dlcSocietal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Repeat
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.prettyPrint
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.Bank
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.BankMode
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.OnlyNumbersInputTransformation
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BankScreenViewModel : ViewModel(), KoinComponent {
    val bank: Bank by inject()
    val inputTextState = TextFieldState()

    fun getAmount(value: CharSequence): Long {
        if (value.isEmpty()) return 0L
        return value.toString().toLong()
    }

    fun onBankModeClick(bankMode: BankMode) {
        bank.bankMode = bankMode
        inputTextState.clearText()
    }

    fun onCustomAmountChange(value: Long) {
        inputTextState.setTextAndPlaceCursorAtEnd(value.toString())
    }

    fun onBuyBtnClick() {
        val amount = inputTextState.text.toString().toLong()
        bank.trade(amount)
        inputTextState.clearText()
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector? = null,
    label: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            label()
        }
    }
}

@Composable
fun BankScreen(
    viewModel: BankScreenViewModel = koinViewModel(),
    i18n: I18n = koinInject()
) {
    val colorScheme = MaterialTheme.colorScheme
    val receive = viewModel.bank.calculateReceive(viewModel.getAmount(viewModel.inputTextState.text))
    val maxAffordable = viewModel.bank.getMaxAffordable()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InfoChip(
                icon = Feather.Repeat,
                label = {
                    val leftIconName = when (viewModel.bank.bankMode) {
                        BankMode.CoinsToIsleBucks -> "coins"
                        BankMode.IsleBucksToCoins -> "isle_bucks"
                    }

                    val rightIconName = when (viewModel.bank.bankMode) {
                        BankMode.CoinsToIsleBucks -> "isle_bucks"
                        BankMode.IsleBucksToCoins -> "coins"
                    }

                    val rateLabel = when (viewModel.bank.bankMode) {
                        BankMode.CoinsToIsleBucks -> {
                            viewModel.bank.buyRate.prettyPrint()
                        }
                        BankMode.IsleBucksToCoins -> {
                            viewModel.bank.sellRate.prettyPrint()
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("1")
                        GameImage(modifier = Modifier.size(24.dp), iconName = leftIconName)
                        Text("=")
                        Text(rateLabel)
                        GameImage(modifier = Modifier.size(24.dp), iconName = rightIconName)
                    }
                }
            )

            InfoChip(
                label = {
                    Text(i18n.tr("Fee: {0}", "${(viewModel.bank.feePercent * 100f).prettyPrint()}%"))
                }
            )

            InfoChip(
                label = {
                    Text(i18n.tr("Market Pressure: {0}", "${(viewModel.bank.pressure * 100f).prettyPrint()}%"))
                }
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ConversionBox(
                    label = i18n.tr("You Pay"),
                    currency = if (viewModel.bank.bankMode == BankMode.CoinsToIsleBucks) "coins" else "isle_bucks",
                    balance = if (viewModel.bank.bankMode == BankMode.CoinsToIsleBucks) viewModel.bank.playerInventory.coins else viewModel.bank.cityInventory.isleBucks,
                    isInput = true,
                    textFieldState = viewModel.inputTextState,
                    onMaxClick = { viewModel.onCustomAmountChange(maxAffordable.coerceAtMost(Bank.MAX_TRADE_MONEY)) },
                    outputValue = 0
                )

                ConversionBox(
                    label = i18n.tr("You Receive"),
                    outputValue = receive,
                    currency = if (viewModel.bank.bankMode == BankMode.CoinsToIsleBucks) "isle_bucks" else "coins",
                    balance = if (viewModel.bank.bankMode == BankMode.CoinsToIsleBucks) viewModel.bank.cityInventory.isleBucks else viewModel.bank.playerInventory.coins,
                    isInput = false
                )
            }

            IconButton(
                onClick = { viewModel.onBankModeClick(if(viewModel.bank.bankMode == BankMode.CoinsToIsleBucks) BankMode.IsleBucksToCoins else BankMode.CoinsToIsleBucks) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-2).dp)
                    .size(40.dp)
                    .background(colorScheme.background, CircleShape)
                    .border(1.dp, colorScheme.outlineVariant, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Feather.Repeat,
                    contentDescription = "Switch",
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(10L, 100L, 1000L, 10000L).forEach { valItem ->
                val enabled = maxAffordable >= valItem
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (enabled) colorScheme.secondaryContainer else colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable(enabled) { viewModel.onCustomAmountChange(valItem) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Humanizer.abbreviation(valItem),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) colorScheme.onSecondaryContainer else colorScheme.outline
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colorScheme.surfaceVariant,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(i18n.tr("Market pressure represents the level of market saturation. The higher the market pressure, the worse the exchange rate for buying. Market pressure naturally decays over time."))
                Text(i18n.tr("Exchanging Coins for Isle Bucks will increase market pressure."))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val canAfford = viewModel.getAmount(viewModel.inputTextState.text) in 1..maxAffordable
        Button(
            onClick = { viewModel.onBuyBtnClick() },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            enabled = canAfford,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.primary,
                disabledContainerColor = colorScheme.outlineVariant
            )
        ) {
            Text(
                text =
                    if (canAfford) i18n.tr("Confirm Exchange")
                    else if (viewModel.inputTextState.text.isEmpty()) i18n.tr("Enter Amount")
                    else i18n.tr("Insufficient Balance"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ConversionBox(
    label: String,
    currency: String,
    balance: Long,
    outputValue: Long,
    i18n: I18n = koinInject(),
    isInput: Boolean,
    textFieldState: TextFieldState? = null,
    onMaxClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isInput) colorScheme.surfaceVariant.copy(alpha = 0.3f) else colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colorScheme.primary.copy(alpha = 0.8f))
            Text(
                i18n.tr("Balance: {0}", Humanizer.abbreviation(balance)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GameImage(
                modifier = Modifier.size(40.dp),
                iconName = currency
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isInput && textFieldState != null)
            {
                BasicTextField(
                    modifier = Modifier.weight(1f),
                    state = textFieldState,
                    lineLimits = TextFieldLineLimits.SingleLine,
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(colorScheme.primary),
                    decorator = { innerTextField ->
                        if (textFieldState.text.isEmpty())
                            Text(
                                modifier = Modifier.fillMaxSize(),
                                text = i18n.tr("Amount"),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary.copy(alpha = 0.5f)
                            )
                        innerTextField()
                    },
                    inputTransformation = OnlyNumbersInputTransformation.then(InputTransformation.maxLength(Bank.MAX_TRADE_MONEY_LENGTH))
                )

                Text(
                    text = i18n.tr("MAX"),
                    modifier = Modifier
                        .clickable { onMaxClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                Text(
                    text = if(outputValue == 0L) "0" else outputValue.toString(),
                    modifier = Modifier.weight(1f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}
