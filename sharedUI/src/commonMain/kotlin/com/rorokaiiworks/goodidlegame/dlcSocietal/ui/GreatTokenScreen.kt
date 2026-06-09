package com.rorokaiiworks.goodidlegame.dlcSocietal.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.alorma.compose.settings.ui.SettingsSlider
import com.composables.icons.feather.Feather
import com.composables.icons.feather.X
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.humanizer.Humanizer
import com.rorokaiiworks.goodidlegame.core.items.FormulaTag
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.GreatToken
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.ItemProgress
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.commons.*
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemRowSurface
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

class GreatTokenScreenViewModel : ViewModel(), KoinComponent {
    val greatToken: GreatToken by inject()
    val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    val cityInventory: CityInventory by inject()
    private val itemService: ItemService by inject()

    private val _uiState = MutableStateFlow(GreatTokenScreenUiState())
    val uiState: StateFlow<GreatTokenScreenUiState> = _uiState.asStateFlow()

    val allCityItemTemplates: Map<String, ItemTemplate> by lazy {
        itemTemplates.data.filter { it.value.tags.contains(FormulaTag.CityItem) }
    }
    val allCityItemTemplateList: List<ItemTemplate> by lazy {
        allCityItemTemplates.entries.map { it.value }.toList()
    }
    val queryState = TextFieldState()

    fun addItemWithConsume(itemTemplate: ItemTemplate, count: Long): Long {
        val item = cityInventory.inventory.tryGetItem(itemTemplate.id) ?: return 0
        val currentProgress = greatToken.itemProgresses[itemTemplate.id]
        val remainingNeeded = if (currentProgress != null) {
            (currentProgress.target - currentProgress.current).coerceAtLeast(0L)
        } else {
            Long.MAX_VALUE
        }
        val actualConsume = minOf(count, item.count, remainingNeeded)
        if (actualConsume <= 0) return 0
        val itemToAdd = itemService.createItem(itemTemplate.id, actualConsume)
        val added = greatToken.addItem(itemToAdd)
        if (added) {
            cityInventory.inventory.removeItems(listOf(ItemEntry(itemTemplate.id, actualConsume)))
        }
        return if (added) actualConsume else 0
    }

    fun getAvailableCount(itemTemplate: ItemTemplate): Long {
        return cityInventory.inventory.tryGetItem(itemTemplate.id)?.count ?: 0
    }

    fun getRemainingNeeded(itemTemplate: ItemTemplate): Long {
        val progress = greatToken.itemProgresses[itemTemplate.id] ?: return Long.MAX_VALUE
        return (progress.target - progress.current).coerceAtLeast(0)
    }

    fun getMaxAddAmount(itemTemplate: ItemTemplate): Long {
        val availableCount = getAvailableCount(itemTemplate)
        val remainingNeeded = getRemainingNeeded(itemTemplate)
        return minOf(availableCount, remainingNeeded)
    }

    fun selectItem(itemTemplate: ItemTemplate?) {
        _uiState.update {
            it.copy(
                selectedItemId = itemTemplate?.id,
                addAmount = 1,
            )
        }
    }

    fun setAddAmount(amount: Long) {
        _uiState.update { it.copy(addAmount = amount.coerceAtLeast(1)) }
    }

    fun setFilter(filter: GreatTokenListFilter) {
        _uiState.update { it.copy(filter = filter) }
    }
}

enum class GreatTokenListFilter {
    All,
    Needed,
    Completed,
}

data class GreatTokenScreenUiState(
    val selectedItemId: String? = null,
    val addAmount: Long = 1,
    val searchQuery: String = "",
    val filter: GreatTokenListFilter = GreatTokenListFilter.All,
)

@Composable
fun GreatTokenScreen(viewModel: GreatTokenScreenViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val allItems = remember { viewModel.allCityItemTemplateList }
    val selectedItem = remember(uiState.selectedItemId, allItems) {
        uiState.selectedItemId?.let { selectedId -> allItems.firstOrNull { it.id == selectedId } }
    }

    // TODO: BUG FIX: 名称应该考虑本地化
    val filteredItems by remember(viewModel.queryState.text, uiState.filter, allItems) {
        derivedStateOf {
            allItems.asSequence()
                .filter { itemTemplate ->
                    if (viewModel.queryState.text.isEmpty()) return@filter true
                    itemTemplate.id.lowercase().contains(viewModel.queryState.text) ||
                            itemTemplate.name.lowercase().contains(viewModel.queryState.text)
                }
                .filter { itemTemplate ->
                    val progress = viewModel.greatToken.itemProgresses[itemTemplate.id]
                    val isComplete = progress != null && progress.current >= progress.target
                    when (uiState.filter) {
                        GreatTokenListFilter.All -> true
                        GreatTokenListFilter.Needed -> !isComplete
                        GreatTokenListFilter.Completed -> isComplete
                    }
                }
                .toList()
        }
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GreatTokenItemsPanel(
            modifier = Modifier.weight(0.6f),
            items = filteredItems,
            greatToken = viewModel.greatToken,
            selectedItemId = uiState.selectedItemId,
            queryState = viewModel.queryState,
            filter = uiState.filter,
            getAvailableCount = viewModel::getAvailableCount,
            onFilterChange = viewModel::setFilter,
            onSelectItem = viewModel::selectItem,
            onQuickAddOne = { itemTemplate -> viewModel.addItemWithConsume(itemTemplate, 1) },
        )

        Column(
            modifier = Modifier.weight(0.4f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            selectedItem?.let {
                GreatTokenSelectedItemPanel(
                    itemTemplate = it,
                    viewModel = viewModel,
                    addAmount = uiState.addAmount,
                    onAddAmountChange = viewModel::setAddAmount,
                    onClose = { viewModel.selectItem(null) },
                )
            }

            GreatTokenDetailsPanel(
                greatToken = viewModel.greatToken,
            )
        }
    }
}

@Composable
private fun GreatTokenSelectedItemPanel(
    itemTemplate: ItemTemplate,
    i18n: I18n = koinInject(),
    viewModel: GreatTokenScreenViewModel,
    addAmount: Long,
    onAddAmountChange: (Long) -> Unit,
    onClose: () -> Unit,
) {
    val availableCount = viewModel.getAvailableCount(itemTemplate)
    val remainingNeeded = viewModel.getRemainingNeeded(itemTemplate)
    val maxAmount = viewModel.getMaxAddAmount(itemTemplate)
    val amount = addAmount.coerceAtMost(maxAmount.coerceAtLeast(1L))

    val progress = viewModel.greatToken.itemProgresses[itemTemplate.id]
    val currentProgress = progress?.current ?: 0
    val targetProgress = progress?.target ?: 0
    val progressPercent = if (targetProgress > 0) currentProgress.toFloat() / targetProgress else 0f
    val isComplete = progress != null && currentProgress >= targetProgress

    BaseCard(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GameImage(
                modifier = Modifier.size(48.dp),
                iconName = itemTemplate.id
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = i18n.tr(itemTemplate.name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = i18n.tr("Available: {0}", availableCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = i18n.tr("Still needed: {0}", remainingNeeded),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            IconButton(
                onClick = onClose,
            ) {
                Icon(
                    Feather.X,
                    contentDescription = null
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isComplete) i18n.tr("Done") else "${currentProgress}/${targetProgress}",
                color = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isComplete) FontWeight.Bold else null
            )
            LinearProgressIndicator(
                progress = { progressPercent.coerceIn(0f, 1f) },
                modifier = Modifier.height(30.dp).fillMaxWidth(),
            )
        }

        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (maxAmount <= 0 || isComplete) {
                Text(
                    text = if (isComplete) i18n.tr("Already completed.") else i18n.tr("No available items."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SettingsSlider(
                    title = {
                        Column {
                            Text(i18n.tr("Add Items."))
                            Text(i18n.tr("Amount: {0}", Humanizer.abbreviation(amount)))
                        } },
                    value = amount.toFloat(),
                    valueRange = 1f..maxAmount.toFloat(),
                    onValueChange = { onAddAmountChange(it.toLong().coerceIn(1, maxAmount)) },
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.addItemWithConsume(itemTemplate, amount)
                            onAddAmountChange(1)
                        },
                        enabled = amount in 1..maxAmount
                    ) {
                        Text(i18n.tr("Add"))
                    }
                }
            }
        }
    }
}

@Composable
private fun GreatTokenItemsPanel(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    items: List<ItemTemplate>,
    greatToken: GreatToken,
    selectedItemId: String?,
    queryState: TextFieldState,
    filter: GreatTokenListFilter,
    getAvailableCount: (ItemTemplate) -> Long,
    onFilterChange: (GreatTokenListFilter) -> Unit,
    onSelectItem: (ItemTemplate?) -> Unit,
    onQuickAddOne: (ItemTemplate) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameTextFieldThin(
                modifier = Modifier.weight(0.6f),
                state = queryState,
                placeholder = { Text(i18n.tr("Search")) }
            )

            Row(
                modifier = Modifier.weight(0.4f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = filter == GreatTokenListFilter.All,
                    onClick = { onFilterChange(GreatTokenListFilter.All) },
                    label = { Text(i18n.tr("All")) },
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = filter == GreatTokenListFilter.Needed,
                    onClick = { onFilterChange(GreatTokenListFilter.Needed) },
                    label = { Text(i18n.tr("Needed")) },
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = filter == GreatTokenListFilter.Completed,
                    onClick = { onFilterChange(GreatTokenListFilter.Completed) },
                    label = { Text(i18n.tr("Done")) },
                )
            }
        }

        ListHeader(
            modifier = Modifier.weight(1f),
        )

        LazyColumn(
            modifier = Modifier.weight(19f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items, key = { it.id }) {
                GreatTokenItemEntry(
                    itemTemplate = it,
                    progress = greatToken.itemProgresses[it.id],
                    availableCount = getAvailableCount(it),
                    onSelectItem = onSelectItem,
                    onQuickAddOne = onQuickAddOne,
                    isSelected = it.id == selectedItemId
                )
            }
        }
    }
}

@Composable
private fun ListHeader(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject()
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderButton(
            modifier = Modifier.weight(2f),
            text = i18n.tr("Item"),
            onClick = { }
        ) { }

        VerticalDivider()

        HeaderButton(
            modifier = Modifier.weight(1f),
            text = i18n.tr("Stock"),
            onClick = { }
        ) { }

        VerticalDivider()

        HeaderButton(
            modifier = Modifier.weight(3f),
            text = i18n.tr("Progress"),
            onClick = { }
        ) { }

        VerticalDivider()

        HeaderButton(
            modifier = Modifier.weight(1f),
            text = i18n.tr("Action"),
            onClick = { }
        ) { }
    }
}

@Composable
private fun GreatTokenItemEntry(
    i18n: I18n = koinInject(),
    itemTemplate: ItemTemplate,
    isSelected: Boolean,
    progress: ItemProgress?,
    availableCount: Long,
    onSelectItem: (ItemTemplate?) -> Unit,
    onQuickAddOne: (ItemTemplate) -> Unit,
) {
    val currentProgress = progress?.current ?: 0
    val targetProgress = progress?.target ?: 0
    val progressPercent = if (targetProgress > 0) (currentProgress.toFloat() / targetProgress) * 100 else 0f
    val isComplete = progress != null && currentProgress >= targetProgress
    val remainingNeeded = (targetProgress - currentProgress).coerceAtLeast(0)
    val canQuickAdd = !isComplete && availableCount > 0 && remainingNeeded > 0

    ItemRowSurface(
        isSelected = isSelected,
        rarity = itemTemplate.rarity,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectItem(itemTemplate) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .height(46.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.weight(2f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameImage(
                    modifier = Modifier.size(32.dp),
                    iconName = itemTemplate.id
                )
                Text(text = i18n.tr(itemTemplate.name))
            }

            Text(
                modifier = Modifier.weight(1f),
                text = Humanizer.abbreviation(availableCount),
                textAlign = TextAlign.End,
            )

            if (isComplete) {
                Text(
                    modifier = Modifier.weight(3f),
                    text = i18n.tr("Done"),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                )
            } else {
                Column(
                    modifier = Modifier.weight(3f)
                ) {
                    Text(
                        text = "${currentProgress}/${targetProgress}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(
                        progress = { progressPercent / 100 },
                        modifier = Modifier
                            .height(18.dp)
                            .fillMaxWidth(),
                    )
                }
            }

            FilledTonalButton(
                modifier = Modifier.weight(1f),
                onClick = { onQuickAddOne(itemTemplate) },
                enabled = canQuickAdd,
            ) {
                Text("+1")
            }
        }
    }
}

@Composable
private fun GreatTokenDetailsPanel(
    i18n: I18n = koinInject(),
    greatToken: GreatToken,
) {
    val totalProgress by remember {
        derivedStateOf { greatToken.totalProgress() }
    }
    val current = totalProgress.current
    val target = totalProgress.target
    val progressFraction = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    val progressPercent = (progressFraction * 100).toInt()

    val glowColor = MaterialTheme.colorScheme.primary

    BaseCard(
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    )
                ),
                shape = RoundedCornerShape(4.dp)
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(modifier = Modifier.size(8.dp)) {
                rotate(45f) {
                    drawRect(
                        color = glowColor,
                        size = Size(size.width, size.height)
                    )
                }
            }
            Text(
                text = i18n.tr("Great Token"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(140.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    glowColor.copy(alpha = 0.22f),
                                    glowColor.copy(alpha = 0f),
                                )
                            )
                        )
                )
                ProgressGameImage(
                    modifier = Modifier.size(100.dp),
                    iconName = "great_token",
                    progress = progressFraction,
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "$current",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = " / $target",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = i18n.tr("Total Progress"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                    )
                    Text(
                        text = "$progressPercent%",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                AnimatedProgressIndicator(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth(),
                    targetValue = progressFraction
                )
            }
        }
    }
}