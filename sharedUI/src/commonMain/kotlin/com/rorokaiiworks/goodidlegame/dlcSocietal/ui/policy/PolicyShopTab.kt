package com.rorokaiiworks.goodidlegame.dlcSocietal.ui.policy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicyCard
import com.rorokaiiworks.goodidlegame.ui.inventory.EntrySurface
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun PolicyShopTab(
    viewModel: PolicyScreenViewModel,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyVerticalStaggeredGrid(
        modifier = modifier.fillMaxSize(),
        columns = StaggeredGridCells.Adaptive(
            minSize = 300.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        contentPadding = PaddingValues(4.dp),
    ) {
        items(
            viewModel.policySystem.cards,
            key = { it.id }
        ) {
            ShopItemCard(
                unlocked = viewModel.policySystem.isUnlockedPolicy(it.id),
                purchasable = viewModel.policySystem.isPurchasable(it),
                card = it,
                viewModel = viewModel,
                isDark = isDark,
            )
        }
    }
}

@Composable
private fun ShopItemCard(
    unlocked: Boolean,
    purchasable: Boolean,
    card: PolicyCard,
    viewModel: PolicyScreenViewModel,
    isDark: Boolean,
    i18n: I18n = koinInject()
) {
    PolicyCardItem(
        policyCard = card,
        isDark = isDark,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (card.unlockConditions.isNotEmpty()) {
                Text(
                    text = i18n.tr("Requirements"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                card.unlockConditions.forEach { condition ->
                    EntrySurface(
                        iconName = condition.iconName(),
                        title = condition.title(i18n)
                    ) {
                        Text(
                            text = condition.progressText(i18n),
                            color =
                                if (viewModel.policySystem.isConditionMet(condition)) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (card.unlockCosts.isNotEmpty()) {
                Text(
                    text = i18n.tr("Costs"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                card.unlockCosts.forEach { cost ->
                    val current = viewModel.playerInventory.inventory.findItem(cost.itemId)?.count ?: 0

                    ItemTemplateEntry(
                        itemTemplate = viewModel.itemTemplates.find(cost.itemId)
                    ) {
                        Text(
                            text = "${Humanizer.abbreviation(cost.count)} / ${Humanizer.abbreviation(current)}",
                            color = if (current >= cost.count) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (unlocked) {
                OutlinedButton(onClick = {}, enabled = false) {
                    Text(i18n.tr("Unlocked"))
                }
            } else {
                Button(
                    onClick = { viewModel.policySystem.tryPurchase(card.id) },
                    enabled = purchasable,
                ) {
                    Text(i18n.tr("Buy"))
                }
            }
        }
    }
}
