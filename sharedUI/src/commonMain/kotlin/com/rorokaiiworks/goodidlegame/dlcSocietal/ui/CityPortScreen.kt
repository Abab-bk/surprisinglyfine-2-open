package com.rorokaiiworks.goodidlegame.dlcSocietal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.alorma.compose.settings.ui.SettingsSegmented
import com.alorma.compose.settings.ui.SettingsSlider
import com.rorokaiiworks.goodidlegame.core.Result
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.FormulaTag
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.prettyPrint
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.CityFormulas
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.CityPort
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.TradeMode
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.TradeRule
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.commons.*
import com.rorokaiiworks.goodidlegame.ui.grayScale
import com.rorokaiiworks.goodidlegame.ui.inventory.InventorySort
import com.rorokaiiworks.goodidlegame.ui.inventory.InventorySortType
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemRow
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemTemplateEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class CityPortScreenUiState(
    val selectedItem: ItemTemplate? = null,
)

class CityPortScreenViewModel : ViewModel(), KoinComponent {
    val cityPort: CityPort by inject()
    val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    val cityInventory: CityInventory by inject()
    var sort: InventorySort by mutableStateOf(
        InventorySort(
            type = InventorySortType.Name,
            direction = SortDirection.Ascending
        )
    )

    val allCityItemTemplates: Map<String, ItemTemplate> by lazy {
        itemTemplates.data.filter { it.value.tags.contains(FormulaTag.CityItem) }
    }
    val allCityItemTemplateList: List<ItemTemplate> by lazy {
        allCityItemTemplates.entries.map { it.value }.toList()
    }

    private val _uiState = MutableStateFlow(CityPortScreenUiState())
    val uiState: StateFlow<CityPortScreenUiState> = _uiState.asStateFlow()

    private val _sortedItems = MutableStateFlow<List<ItemTemplate>>(emptyList())
    val sortedItems: StateFlow<List<ItemTemplate>> = _sortedItems.asStateFlow()

    init {
        updateSortedItems()
    }

    fun onCloseTradeRulePanel() {
        _uiState.update { it.copy(selectedItem = null) }
    }

    fun selectItem(itemTemplate: ItemTemplate) {
        _uiState.update { it.copy(selectedItem = itemTemplate) }
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
        updateSortedItems()
    }

    fun updateSortedItems() {
        val counts = cityInventory.inventory.items.associate { it.template.id to it.count }
        var result = allCityItemTemplateList

        result = when (sort.type) {
            InventorySortType.Name ->
                if (sort.direction == SortDirection.Ascending) result.sortedBy { it.name }
                else result.sortedByDescending { it.name }

            InventorySortType.Price ->
                if (sort.direction == SortDirection.Ascending) result.sortedBy { it.price }
                else result.sortedByDescending { it.price }

            InventorySortType.Count ->
                if (sort.direction == SortDirection.Ascending) result.sortedBy { counts[it.id] ?: 0 }
                else result.sortedByDescending { counts[it.id] ?: 0 }
        }

        _sortedItems.value = result
    }

    fun upgradeCapacity() {
        cityPort.upgradeCapacity(
            canConsume = cityInventory.inventory::canConsume,
            consume = cityInventory.inventory::removeItems,
        )
        updateSortedItems()
    }

    fun upgradeTradeInterval() {
        cityPort.upgradeTradeInterval(
            canConsume = cityInventory.inventory::canConsume,
            consume = cityInventory.inventory::removeItems,
        )
        updateSortedItems()
    }

    fun upgradeSaturationSpeed() {
        cityPort.upgradeSaturationSpeed(
            canConsume = cityInventory.inventory::canConsume,
            consume = cityInventory.inventory::removeItems,
        )
        updateSortedItems()
    }
}

@Composable
fun CityPortScreen(viewModel: CityPortScreenViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val sortedItems by viewModel.sortedItems.collectAsState()

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TradeItemsPanel(
            modifier = Modifier.weight(0.6f),
            sort = viewModel.sort,
            sortedItems = sortedItems,
            uiState = uiState,
            cityInventory = viewModel.cityInventory,
            getRule = viewModel.cityPort::findRule,
            getSaturation = viewModel.cityPort::getSaturation,
            getPriceMultiplier = viewModel.cityPort::getPriceMultiplier,
            onChangeSort = viewModel::onChangeSort,
            onClickItem = viewModel::selectItem,
        )

        Column(
            modifier = Modifier.weight(0.4f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.selectedItem?.let {
                TradeRulePanel(
                    itemTemplate = it,
                    rule = viewModel.cityPort.findRule(it.id),
                    onRuleChanged = { rule -> viewModel.cityPort.setRule(it.id, rule) },
                    onClose = viewModel::onCloseTradeRulePanel,
                    cityInventory = viewModel.cityInventory,
                    cityPort = viewModel.cityPort,
                )
            }

            CityPortDetailsPanel(
                cityPort = viewModel.cityPort,
                cityInventory = viewModel.cityInventory,
                onClickUpgradeCapacity = viewModel::upgradeCapacity,
                onClickUpgradeTradeInterval = viewModel::upgradeTradeInterval,
                onClickUpgradeSaturationSpeed = viewModel::upgradeSaturationSpeed,
            )

            LastTradeResultPanel(
                modifier = Modifier.fillMaxWidth(),
                tradeResult = viewModel.cityPort.lastTradeResult,
                itemTemplates = viewModel.itemTemplates,
            )
        }
    }
}

@Composable
private fun CityPortDetailsPanel(
    i18n: I18n = koinInject(),
    cityPort: CityPort,
    cityInventory: CityInventory,
    onClickUpgradeCapacity: () -> Unit,
    onClickUpgradeTradeInterval: () -> Unit,
    onClickUpgradeSaturationSpeed: () -> Unit,
) {
    val nextCapacity = CityFormulas.calculatePortCapacityForLevel(cityPort.capacityLevel + 1)
    val nextInterval = CityFormulas.calculatePortTradeIntervalForLevel(cityPort.tradeIntervalLevel + 1)
    val currentSatIncrease = CityFormulas.calculatePortSaturationIncreasePerItemForLevel(cityPort.saturationSpeedLevel)
    val nextSatIncrease = CityFormulas.calculatePortSaturationIncreasePerItemForLevel(cityPort.saturationSpeedLevel + 1)
    val currentSatDecay = CityFormulas.calculatePortSaturationDecayPerSecondForLevel(cityPort.saturationSpeedLevel)
    val nextSatDecay = CityFormulas.calculatePortSaturationDecayPerSecondForLevel(cityPort.saturationSpeedLevel + 1)

    BaseCard(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = i18n.tr("Port"), style = MaterialTheme.typography.titleMedium)

        Text(
            text = i18n.tr("Next Trade In: {0}", Humanizer.duration(cityPort.nextTradeTimeDistance)),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = i18n.tr("Current Stats"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DetailRow(label = i18n.tr("Capacity (per trade)"), value = cityPort.capacity.toString())
                DetailRow(label = i18n.tr("Trade Interval"), value = Humanizer.duration(cityPort.tradeInterval))
                DetailRow(
                    label = i18n.tr("Saturation Speed"),
                    value = i18n.tr(
                        "+{0}/item, -{1}/sec",
                        currentSatIncrease.prettyPrint(),
                        currentSatDecay.prettyPrint()
                    )
                )
            }
        }

        Text(
            text = i18n.tr("Upgrades"),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UpgradeRow(
                    title = i18n.tr("Capacity"),
                    level = cityPort.capacityLevel,
                    description = i18n.tr("How many items can be sold per trade."),
                    current = cityPort.capacity.toString(),
                    next = nextCapacity.toString(),
                    costs = CityFormulas.calculatePortCapacityLevelUpCosts(cityPort.capacityLevel),
                    canConsumeResult = cityInventory.inventory::canConsumeResult,
                    onClickUpgrade = onClickUpgradeCapacity,
                )

                UpgradeRow(
                    title = i18n.tr("Trade Interval"),
                    level = cityPort.tradeIntervalLevel,
                    description = i18n.tr("How often trades happen. Lower is better."),
                    current = Humanizer.duration(cityPort.tradeInterval),
                    next = Humanizer.duration(nextInterval),
                    costs = CityFormulas.calculatePortTradeIntervalLevelUpCosts(cityPort.tradeIntervalLevel),
                    canConsumeResult = cityInventory.inventory::canConsumeResult,
                    onClickUpgrade = onClickUpgradeTradeInterval,
                )

                UpgradeRow(
                    title = i18n.tr("Saturation Speed"),
                    level = cityPort.saturationSpeedLevel,
                    description = i18n.tr("Saturation rises slower and decays faster."),
                    current = i18n.tr(
                        "+{0}/item, -{1}/sec",
                        currentSatIncrease.prettyPrint(),
                        currentSatDecay.prettyPrint()
                    ),
                    next = i18n.tr(
                        "+{0}/item, -{1}/sec",
                        nextSatIncrease.prettyPrint(),
                        nextSatDecay.prettyPrint()
                    ),
                    costs = CityFormulas.calculatePortSaturationSpeedLevelUpCosts(cityPort.saturationSpeedLevel),
                    canConsumeResult = cityInventory.inventory::canConsumeResult,
                    onClickUpgrade = onClickUpgradeSaturationSpeed,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun UpgradeRow(
    i18n: I18n = koinInject(),
    cityInventory: CityInventory = koinInject(),
    title: String,
    level: Int,
    description: String,
    current: String,
    next: String,
    costs: List<ItemEntry>,
    canConsumeResult: (List<ItemEntry>) -> Result<*>,
    onClickUpgrade: () -> Unit,
) {
    val consumeResult = canConsumeResult(costs)

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "Lv. $level",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCell(
                title = i18n.tr("Now"),
                value = current,
                modifier = Modifier.weight(1f),
            )
            StatCell(
                title = i18n.tr("Next"),
                value = next,
                modifier = Modifier.weight(1f),
            )
        }

        UpgradeCosts(
            costs = costs,
            cityInventory = cityInventory,
        )

        if (consumeResult is Result.Error) {
            Text(
                text = consumeResult.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClickUpgrade,
            enabled = consumeResult is Result.Success,
        ) {
            Text(text = i18n.tr("Upgrade"))
        }
    }
}

@Composable
private fun StatCell(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun UpgradeCosts(
    i18n: I18n = koinInject(),
    cityInventory: CityInventory,
    costs: List<ItemEntry>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = i18n.tr("Cost"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column {
            costs.forEach {
                ConsumeItemEntry(
                    consume = it,
                    inventory = cityInventory.inventory,
                )
            }
        }
    }
}

@Composable
private fun LastTradeResultPanel(
    i18n: I18n = koinInject(),
    modifier: Modifier = Modifier,
    tradeResult: List<ItemEntry>,
    itemTemplates: DataTable<ItemTemplate>
) {
    BaseCard(modifier = modifier) {
        Text(text = i18n.tr("Last Trade"))

        Column {
            tradeResult.forEach {
                val template = itemTemplates.find(it.itemId)
                ItemTemplateEntry(
                    itemTemplate = template,
                ) {
                    Text(
                        text = Humanizer.abbreviation(template.price * it.count)
                    )

                    GameImage(
                        modifier = Modifier.size(30.dp),
                        iconName = "coins"
                    )
                }
            }
        }
    }
}

@Composable
private fun TradeRulePanel(
    i18n: I18n = koinInject(),
    itemTemplate: ItemTemplate,
    cityInventory: CityInventory,
    cityPort: CityPort,
    rule: TradeRule,
    onRuleChanged: (TradeRule) -> Unit,
    onClose: () -> Unit,
) {
    BaseCard(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardTitleWithCloseBtn(
            title = i18n.tr("Trade Rule"),
            onClose = onClose
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GameImage(
                modifier = Modifier.size(48.dp),
                iconName = itemTemplate.id
            )

            Column {
                Text(text = i18n.tr(itemTemplate.name))
                Text(text = i18n.tr("Stock: {0}", cityInventory.inventory.findItem(itemTemplate.id)?.count ?: 0))
            }
        }

        val saturation = cityPort.getSaturation(itemTemplate.id)
        val multiplier = cityPort.getPriceMultiplier(itemTemplate.id)
        Text(
            text = i18n.tr(
                "Saturation: {0}% (Price x{1})",
                (saturation * 100).roundToInt(),
                multiplier.prettyPrint()
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SettingsSegmented(
            title = { Text(text = i18n.tr("Trade Mode")) },
            items = TradeMode.entries,
            selectedItem = rule.mode,
            onItemSelected = { onRuleChanged(rule.copy(mode = it)) },
            itemTitleMap = { i18n.tr(it.label) }
        )

        SettingsSlider(
            title = { Text(text = i18n.tr("Min Stock")) },
            subtitle = { Text(text = rule.minStock.toString()) },
            value = rule.minStock.toFloat(),
            onValueChange = { onRuleChanged(rule.copy(minStock = it.toInt())) },
            valueRange = 0f..200f,
        )
    }
}


@Composable
private fun TradeItemsPanel(
    modifier: Modifier = Modifier,
    sort: InventorySort,
    sortedItems: List<ItemTemplate>,
    uiState: CityPortScreenUiState,
    cityInventory: CityInventory,
    getRule: (String) -> TradeRule,
    getSaturation: (String) -> Float,
    getPriceMultiplier: (String) -> Float,
    onClickItem: (ItemTemplate) -> Unit,
    onChangeSort: (InventorySortType) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ListHeader(
            modifier = Modifier.weight(1f),
            sort = sort,
            onChangeSort = onChangeSort
        )

        LazyColumn(
            modifier = Modifier.weight(19f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(sortedItems, key = { it.id }) { item ->
                TradeItemEntry(
                    uiState = uiState,
                    itemTemplate = item,
                    cityInventory = cityInventory,
                    getRule = getRule,
                    getSaturation = getSaturation,
                    getPriceMultiplier = getPriceMultiplier,
                    onClick = { onClickItem(item) }
                )
            }
        }
    }
}


@Composable
private fun ListHeader(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    sort: InventorySort,
    onChangeSort: (InventorySortType) -> Unit,
) {
    InventoryItemListHeader(
        modifier = modifier,
        sort = sort,
        onChangeSort = onChangeSort
    ) {
        VerticalDivider()

        HeaderButton(
            modifier = Modifier.weight(1f),
            text = i18n.tr("Saturation"),
            onClick = { },
        ) { }

        VerticalDivider()

        HeaderButton(
            modifier = Modifier.weight(1f),
            text = i18n.tr("Trade Mode"),
            onClick = { },
        ) { }

        VerticalDivider()

        HeaderButton(
            modifier = Modifier.weight(1f),
            text = i18n.tr("Min Stock"),
            onClick = { },
        ) { }
    }
}


@Composable
private fun TradeItemEntry(
    i18n: I18n = koinInject(),
    uiState: CityPortScreenUiState,
    itemTemplate: ItemTemplate,
    cityInventory: CityInventory,
    onClick: () -> Unit,
    getRule: (String) -> TradeRule,
    getSaturation: (String) -> Float,
    getPriceMultiplier: (String) -> Float,
) {
    val item = cityInventory.inventory.tryGetItem(itemTemplate.id)
    val rule = getRule(itemTemplate.id)
    val saturation = getSaturation(itemTemplate.id)
    val priceMultiplier = getPriceMultiplier(itemTemplate.id)
    val effectivePrice = (itemTemplate.price * priceMultiplier).roundToLong()

    ItemRow(
        modifier = if (item == null) Modifier.grayScale() else Modifier,
        priceIcon = "isle_bucks",
        name = i18n.tr(itemTemplate.name),
        count = item?.count ?: 0L,
        rarity = itemTemplate.rarity,
        iconId = itemTemplate.id,
        price = effectivePrice,
        isSelected = uiState.selectedItem?.id == itemTemplate.id,
        onClick = onClick,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "${(saturation * 100).roundToInt()}%",
            textAlign = TextAlign.End,
        )

        Text(
            modifier = Modifier.weight(1f),
            text = i18n.tr(rule.mode.label),
            textAlign = TextAlign.End,
        )

        Text(
            modifier = Modifier.weight(1f),
            text = "${rule.minStock}",
            textAlign = TextAlign.End,
        )
    }
}
