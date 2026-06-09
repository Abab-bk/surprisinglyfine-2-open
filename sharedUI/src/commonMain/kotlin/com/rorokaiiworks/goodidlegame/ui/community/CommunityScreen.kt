package com.rorokaiiworks.goodidlegame.ui.community

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.rorokaiiworks.goodidlegame.core.community.Community
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.ui.commons.ConsumeItemsContent
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateEntry
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class CommunityTab(val label: String) {
    Enchanting(i18nWrapper("Enchanting")),
    Square(i18nWrapper("Square")),
    Altar(i18nWrapper("Altar"))
}

class CommunityViewModel : ViewModel(), KoinComponent {
    val community: Community by inject()

    var selectedTab by mutableStateOf(CommunityTab.Enchanting)
        private set

    fun onTabSelected(tab: CommunityTab) {
        selectedTab = tab
    }
}

@Composable
fun CommunityScreen(viewModel: CommunityViewModel = koinViewModel()) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.9f)) {
            when (viewModel.selectedTab) {
                CommunityTab.Enchanting -> EnchantingTableScreen(
                    modifier = Modifier.fillMaxSize(),
                    enchantingTable = viewModel.community.enchantingTable
                )

                CommunityTab.Square -> SquareScreen(
                    modifier = Modifier.fillMaxSize(),
                    square = viewModel.community.square
                )

                CommunityTab.Altar -> AltarScreen(
                    modifier = Modifier.fillMaxSize(),
                    altar = viewModel.community.altar
                )
            }
        }

        CommunityBottomBar(
            modifier = Modifier.weight(0.1f),
            selectedTab = viewModel.selectedTab,
            onTabSelected = { viewModel.onTabSelected(it) }
        )
    }
}

@Composable
private fun CommunityBottomBar(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    selectedTab: CommunityTab,
    onTabSelected: (CommunityTab) -> Unit
) {
    SecondaryTabRow(
        modifier = modifier,
        selectedTabIndex = CommunityTab.entries.indexOf(selectedTab),
    ) {
        CommunityTab.entries.forEach {
            Tab(
                modifier = Modifier.height(48.dp),
                selected = selectedTab == it,
                onClick = { onTabSelected(it) },
                content = {
                    Text(
                        text = i18n.tr(it.label),
                        textAlign = TextAlign.Center,
                    )
                }
            )
        }
    }
}


@Composable
fun CommunityCostEntry(
    i18n: I18n = koinInject(),
    costs: List<ItemEntry>,
    itemTemplates: DataTable<ItemTemplate>,
    playerInventory: PlayerInventory,
    text: String = "Purchase Slot",
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HorizontalDivider()

        ConsumeItemsContent(
            consumes = costs,
            itemTemplates = itemTemplates,
            inventory = playerInventory.inventory,
        )

        Button(
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick,
        ) {
            Text(text = i18n.tr(text))
        }
    }
}
