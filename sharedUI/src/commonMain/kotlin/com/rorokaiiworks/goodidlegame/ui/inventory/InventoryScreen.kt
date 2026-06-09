package com.rorokaiiworks.goodidlegame.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.window.core.layout.WindowSizeClass
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.Inventory
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.ui.commons.GameDialog
import com.rorokaiiworks.goodidlegame.ui.commons.SortDirection
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import com.rorokaiiworks.goodidlegame.ui.isWideScreen
import com.rorokaiiworks.goodidlegame.ui.skills.DropTablePanel
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named


class InventoryScreenViewModel : ViewModel(), KoinComponent {
    private val player: Player by inject()
    private val itemService: ItemService by inject()
    private val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())

    var selectedItem: Item? by mutableStateOf(null)
    var sort: InventorySort by mutableStateOf(
        InventorySort(InventorySortType.Name, SortDirection.Ascending)
    )
    val itemsListState = LazyListState()

    val queryState = TextFieldState()
    var openResults: List<Item> by mutableStateOf(emptyList())
        private set
    var openResultsKey: Int by mutableIntStateOf(0)
        private set
    var isOpenResultsDialogVisible: Boolean by mutableStateOf(false)
        private set
    var openErrorMessage: String? by mutableStateOf(null)
        private set

    fun openItemFromDropTable(item: Item, inventory: Inventory, requestedCount: Long) {
        val dropTable = item.template.dropTable ?: return
        openErrorMessage = null
        if (inventory.isFull()) {
            openErrorMessage = i18nWrapper("Inventory full")
            openResults = emptyList()
            isOpenResultsDialogVisible = false
            return
        }

        val openCount = requestedCount.coerceAtMost(item.count)
        if (openCount <= 0) return

        val newItems = mutableListOf<Item>()
        var opened = 0L

        repeat(openCount.toInt()) {
            if (inventory.isFull()) return@repeat
            newItems += dropTable.pick(itemTemplates)
            opened += 1
        }

        if (opened == 0L) {
            openErrorMessage = i18nWrapper("Inventory full")
            openResults = emptyList()
            isOpenResultsDialogVisible = false
            return
        }

        inventory.removeItem(item.copy(count = opened))
        inventory.addItems(newItems)

        openErrorMessage = if (opened < openCount) i18nWrapper("Inventory full") else null
        openResults = aggregateOpenResults(newItems)
        openResultsKey += 1
        isOpenResultsDialogVisible = true
        refreshSelectedItem(inventory)
    }

    fun onChangeSort(sortType: InventorySortType) {
        sort = if (sort.type == sortType) {
            sort.copy(direction =
                if (sort.direction == SortDirection.Ascending) SortDirection.Descending
                else SortDirection.Ascending
            )
        } else {
            sort.copy(type = sortType)
        }
    }

    fun onSell(item: Item, inventory: Inventory) {
        itemService.unequipItem(item = item, actor = player)
        refreshSelectedItem(inventory)
    }

    fun onClose() {
        selectedItem = null
        clearOpenResults()
    }

    fun onSelectItem(item: Item) {
        selectedItem = item
        clearOpenResults()
    }

    fun dismissOpenResultsDialog() {
        isOpenResultsDialogVisible = false
    }

    private fun clearOpenResults() {
        openResults = emptyList()
        isOpenResultsDialogVisible = false
        openErrorMessage = null
    }

    private fun aggregateOpenResults(items: List<Item>): List<Item> {
        val merged = mutableListOf<Item>()
        items.forEach { item ->
            val existing = merged.firstOrNull { it.isSameItem(item) }
            if (existing != null) {
                existing.count += item.count
            } else {
                merged += item.copy()
            }
        }
        return merged
    }

    private fun refreshSelectedItem(inventory: Inventory) {
        val current = selectedItem ?: return
        val updated = inventory.items.firstOrNull { it.isSameItem(current) }
        selectedItem = updated
    }
}

@Composable
fun InventoryScreen(
    inventory: Inventory,
    viewModel: InventoryScreenViewModel = koinViewModel(),
    windowSizeClass: WindowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
) {
    if (isWideScreen(windowSizeClass)) {
        InventoryScreenWide(
            inventory = inventory,
            selectedItem = viewModel.selectedItem,
            onClick = { viewModel.onSelectItem(it) },
            onSell = { viewModel.onSell(it, inventory) },
            sort = viewModel.sort,
            onChangeSort = viewModel::onChangeSort,
            onClose = { viewModel.onClose() },
            onOpenItemDropTable = viewModel::openItemFromDropTable,
            itemsListState = viewModel.itemsListState,
            openErrorMessage = viewModel.openErrorMessage,
            queryState = viewModel.queryState
        )
    } else {
        InventoryScreenMobile(
            inventory = inventory,
            selectedItem = viewModel.selectedItem,
            onClick = { viewModel.onSelectItem(it) },
            onSell = { viewModel.onSell(it, inventory) },
            sort = viewModel.sort,
            onChangeSort = viewModel::onChangeSort,
            onClose = { viewModel.onClose() },
            onOpenItemDropTable = viewModel::openItemFromDropTable,
            itemsListState = viewModel.itemsListState,
            openErrorMessage = viewModel.openErrorMessage,
            queryState = viewModel.queryState
        )
    }

    if (viewModel.isOpenResultsDialogVisible) {
        OpenResultsDialog(
            results = viewModel.openResults,
            resultsKey = viewModel.openResultsKey,
            onDismiss = viewModel::dismissOpenResultsDialog
        )
    }
}

@Composable
private fun InventoryScreenWide(
    inventory: Inventory,
    selectedItem: Item?,
    onClick: (Item) -> Unit,
    onSell: (Item) -> Unit,
    sort: InventorySort,
    onChangeSort: (InventorySortType) -> Unit,
    onOpenItemDropTable: (Item, Inventory, Long) -> Unit,
    queryState: TextFieldState,
    itemsListState: LazyListState,
    openErrorMessage: String?,
    onClose: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InventoryPanel(
            modifier = Modifier.weight(1f),
            inventory = inventory,
            selectedItem = selectedItem,
            onItemClick = onClick,
            sort = sort,
            onChangeSort = onChangeSort,
            itemsListState = itemsListState,
            queryState = queryState
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (selectedItem != null) {
                ItemDetailPanel(
                    inventory = inventory,
                    selectedItem = selectedItem,
                    inputState = rememberTextFieldState(),
                    onSell = onSell,
                    onClose = onClose,
                    onOpenItemDropTable = onOpenItemDropTable,
                    openErrorMessage = openErrorMessage,
                    compactButtons = false
                )
            }
        }
    }
}

@Composable
private fun InventoryScreenMobile(
    inventory: Inventory,
    selectedItem: Item?,
    onClick: (Item) -> Unit,
    onSell: (Item) -> Unit,
    sort: InventorySort,
    onChangeSort: (InventorySortType) -> Unit,
    queryState: TextFieldState,
    onOpenItemDropTable: (Item, Inventory, Long) -> Unit,
    itemsListState: LazyListState,
    openErrorMessage: String?,
    onClose: () -> Unit
) {
    if (selectedItem != null) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            ItemDetailPanel(
                inventory = inventory,
                inputState = rememberTextFieldState(),
                selectedItem = selectedItem,
                onSell = onSell,
                onClose = onClose,
                onOpenItemDropTable = onOpenItemDropTable,
                openErrorMessage = openErrorMessage,
                compactButtons = true
            )
        }

        return
    }

    InventoryPanel(
        modifier = Modifier.fillMaxSize(),
        inventory = inventory,
        onItemClick = onClick,
        sort = sort,
        selectedItem = selectedItem,
        onChangeSort = onChangeSort,
        itemsListState = itemsListState,
        queryState = queryState
    )
}

@Composable
private fun ItemDetailPanel(
    inventory: Inventory,
    selectedItem: Item,
    inputState: TextFieldState,
    onSell: (Item) -> Unit,
    onClose: () -> Unit,
    onOpenItemDropTable: (Item, Inventory, Long) -> Unit,
    openErrorMessage: String?,
    compactButtons: Boolean
) {
    ItemSellPanel(
        ownedItem = selectedItem,
        inputState = inputState,
        onSell = onSell,
        onClose = onClose
    ) {
        selectedItem.template.dropTable?.let {
            DropTablePanel(
                dropTable = it,
            )
        }

        selectedItem.template.dropTable?.let {
            OpenItemButtons(
                inventory = inventory,
                selectedItem = selectedItem,
                onOpenItemDropTable = onOpenItemDropTable,
                openErrorMessage = openErrorMessage,
                compactButtons = compactButtons
            )
        }
    }
}

@Composable
private fun OpenItemButtons(
    inventory: Inventory,
    selectedItem: Item,
    onOpenItemDropTable: (Item, Inventory, Long) -> Unit,
    openErrorMessage: String?,
    compactButtons: Boolean
) {
    val isInventoryFull = inventory.isFull()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                shape = RectangleShape,
                enabled = !isInventoryFull && selectedItem.count >= 1,
                onClick = {
                    onOpenItemDropTable(selectedItem, inventory, 1)
                }
            ) {
                Text("Open 1")
            }

            Button(
                modifier = Modifier.weight(1f),
                shape = RectangleShape,
                enabled = !isInventoryFull && selectedItem.count >= 10,
                onClick = {
                    onOpenItemDropTable(selectedItem, inventory, 10)
                }
            ) {
                Text("Open 10")
            }

            if (!compactButtons) {
                Button(
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    enabled = !isInventoryFull && selectedItem.count > 0,
                    onClick = {
                        onOpenItemDropTable(selectedItem, inventory, selectedItem.count)
                    }
                ) {
                    Text("Open All")
                }
            }
        }

        if (compactButtons) {
            Button(
                modifier = Modifier.weight(1f),
                shape = RectangleShape,
                enabled = !isInventoryFull && selectedItem.count > 0,
                onClick = {
                    onOpenItemDropTable(selectedItem, inventory, selectedItem.count)
                }
            ) {
                Text("Open All")
            }
        }

        openErrorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun OpenResultsDialog(
    results: List<Item>,
    resultsKey: Int,
    onDismiss: () -> Unit,
    i18n: I18n = koinInject()
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(resultsKey) {
        scrollState.scrollTo(0)
    }

    GameDialog(
        title = i18n.tr("Open Results"),
        onDismissRequest = onDismiss,
        confirmEnabled = true,
        onConfirmation = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .verticalScroll(scrollState)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (results.isEmpty()) {
                Text(
                    text = i18n.tr("Nothing obtained."),
                    color = MaterialTheme.colorScheme.secondary
                )
                return@Column
            }

            results.forEach { item ->
                ItemTemplateEntry(itemTemplate = item.template) {
                    if (item.count > 1) {
                        Text(text = "x${item.count}")
                    }
                }
            }
        }
    }
}
