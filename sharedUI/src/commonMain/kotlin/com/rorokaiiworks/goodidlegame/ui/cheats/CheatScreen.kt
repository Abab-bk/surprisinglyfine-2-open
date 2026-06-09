@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.ui.cheats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.core.*
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.journey.JourneySystem
import com.rorokaiiworks.goodidlegame.core.quests.QuestStatus
import com.rorokaiiworks.goodidlegame.core.reveal.Revealer
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Search
import com.rorokaiiworks.goodidlegame.core.achievements.AchievementSystem
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSystem
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

class CheatScreenViewModel : ViewModel(), KoinComponent {
    private val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    private val journeySystem: JourneySystem by inject()
    private val inventory: PlayerInventory by inject()
    private val itemService: ItemService by inject()
    private val gameState: GameState by inject()
    private val revealer: Revealer by inject()
    private val eventBus: EventBus by inject()
    private val timeProvider: ITimeProvider by inject()
    private val adPlayer: IAdPlayer by inject()
    private val playerSkills: PlayerSkills by inject()
    private val achievementSystem: AchievementSystem by inject()
    private val taskSystem: TaskSystem by inject()
    private val cityInventory: CityInventory by inject()
    private val player: Player by inject()

    private val starDropRandom: RandomSource by inject { parametersOf(RandomSource.TAG_STAR_DROP) }

    val gameEngine: GameEngine by inject()

    sealed class CheatAction(
        open val label: String,
        open val description: String = ""
    ) {
        data class AddCurrency(
            val itemId: String,
            val amount: Long,
            override val label: String = "Add $amount $itemId"
        ) : CheatAction(label)

        data class CustomSuspendAction(
            override val label: String,
            override val description: String = "",
            val action: suspend () -> Unit
        ) : CheatAction(label, description)

        data class CustomAction(
            override val label: String,
            override val description: String = "",
            val action: () -> Unit
        ) : CheatAction(label, description)
    }

    data class CheatCategory(
        val title: String,
        val actions: List<CheatAction>
    )

    val cheatCategories = listOf(
        CheatCategory(
            title = "Currency",
            actions = listOf(
                CheatAction.AddCurrency("star", 1000),
                CheatAction.AddCurrency("star", 10000),
            )
        ),
        CheatCategory(
            title = "Equipment",
            actions = listOf(
                CheatAction.CustomAction(
                    label = "Rainbow Gear Set",
                    description = "Add full rainbow equipment"
                ) {
                    inventory.inventory.addItem(itemService.createItem("rainbow_sword"))
                    inventory.inventory.addItem(itemService.createItem("rainbow_helmet"))
                    inventory.inventory.addItem(itemService.createItem("rainbow_armor"))
                    inventory.inventory.addItem(itemService.createItem("rainbow_leg_armor"))
                    inventory.inventory.addItem(itemService.createItem("rainbow_boots"))
                    inventory.inventory.addItem(itemService.createItem("rainbow_shield"))
                    inventory.inventory.addItem(itemService.createItem("rainbow_cape"))
                },


                CheatAction.CustomAction(
                    label = "Copper Gear Set",
                    description = "Add full copper equipment"
                ) {
                    itemService.equipItem(itemService.createItem("copper_sword"), player)
                    itemService.equipItem(itemService.createItem("copper_armor"), player)
                    itemService.equipItem(itemService.createItem("copper_leg_armor"), player)
                    itemService.equipItem(itemService.createItem("copper_boots"), player)
                    itemService.equipItem(itemService.createItem("copper_shield"), player)
                    itemService.equipItem(itemService.createItem("copper_cape"), player)
                },

                CheatAction.CustomAction(
                    label = "Altar test"
                ) {
                    inventory.inventory.addItem(itemService.createItem("gem_ruby", 10000))
                    inventory.inventory.addItem(itemService.createItem("pine_wood", 10000))
                    inventory.inventory.addItem(itemService.createItem("coins", 10000))
                    inventory.inventory.addItem(itemService.createItem("map_cave", 10000))
                    itemTemplates.all().filter { it.type == ItemType.Relic }.forEach {
                        inventory.inventory.addItem(itemService.createItem(it.id, 1000))
                    }
                }
            )
        ),
        CheatCategory(
            title = "Skills & Progress",
            actions = listOf(
                CheatAction.CustomAction(
                    label = "Unlock Mining",
                    action = { gameState.unlockSkill("skill_mining") }
                ),
                CheatAction.CustomAction(
                    label = "Woodcutting +10",
                    description = "Add 10 levels to woodcutting"
                ) {
                    playerSkills.skills["skill_woodcutting"]?.let {
                        repeat(10) {
                            playerSkills.skills["skill_woodcutting"]?.addOneLevel()
                        }
                    }
                },

                CheatAction.CustomAction(
                    label = "OneHanded +10",
                    description = "Add 10 levels to oneHanded"
                ) {
                    playerSkills.skills["skill_one_handed"]?.let {
                        repeat(10) {
                            playerSkills.skills["skill_one_handed"]?.addOneLevel()
                        }
                    }
                },
                CheatAction.CustomAction(
                    label = "Finish Quest",
                    description = "Complete current quest"
                ) {
                    journeySystem.currentQuest?.changeStatus(QuestStatus.Completed)
                },
                CheatAction.CustomAction(
                    label = "Complete & Claim Journey Quest",
                    description = "Fully complete current journey quest, claim rewards, and advance to next"
                ) {
                    journeySystem.currentQuest?.let { quest ->
                        if (quest.status != QuestStatus.Completed) {
                            quest.changeStatus(QuestStatus.Completed)
                        }
                        journeySystem.claimQuest()
                    }
                },
                CheatAction.CustomSuspendAction(
                    label = "Reveal Loadout",
                    action = {
                        revealer.reveal(AppDestination.LoadoutDestination.route)
                    }
                ),
            )
        ),
        CheatCategory(
            title = "Time Control",
            actions = listOf(
                CheatAction.CustomAction(
                    label = "Set 2025-01-01",
                    action = {
                        if (timeProvider !is FakeTimeProvider) return@CustomAction
                        (timeProvider as FakeTimeProvider).setTime(Instant.parse("2025-01-01T00:00:00Z"))
                        showToast("Time set to 2025-01-01")
                    }
                ),
                CheatAction.CustomAction(
                    label = "+12 Hours",
                    action = {
                        if (timeProvider !is FakeTimeProvider) return@CustomAction
                        (timeProvider as FakeTimeProvider).advance(12.hours)
                        showToast("Time advanced by 12 hours")
                    }
                ),
                CheatAction.CustomAction(
                    label = "+1 minutes",
                    action = {
                        if (timeProvider !is FakeTimeProvider) return@CustomAction
                        (timeProvider as FakeTimeProvider).advance(1.minutes)
                        showToast("Time advanced by 1 minute")
                    }
                ),
                CheatAction.CustomAction(
                    label = "+1 Day",
                    action = {
                        if (timeProvider !is FakeTimeProvider) return@CustomAction
                        (timeProvider as FakeTimeProvider).advanceDays(1)
                        showToast("Time advanced by 1 day")
                    }
                ),
                CheatAction.CustomAction(
                    label = "+7 Days",
                    action = {
                        if (timeProvider !is FakeTimeProvider) return@CustomAction
                        (timeProvider as FakeTimeProvider).advanceDays(7)
                        showToast("Time advanced by 7 days")
                    }
                ),

                CheatAction.CustomAction(
                    label = "Toggle Model",
                    action = {
                        if (timeProvider !is FakeTimeProvider) return@CustomAction
                        (timeProvider as FakeTimeProvider).realModel = !(timeProvider as FakeTimeProvider).realModel
                        showToast("Model toggled: real mode: ${(timeProvider as FakeTimeProvider).realModel}")
                    }
                ),
            )
        ),
        CheatCategory(
            title = "Game Systems",
            actions = listOf(
                CheatAction.CustomAction(
                    label = "Tick Passive 1h",
                    description = "Simulate 1 hour of passive progress"
                ) {
                    taskSystem.passiveSessions.forEach { it.tick(timeProvider.nowMillis()) }
                },
                CheatAction.CustomAction(
                    label = "Star Drop 100%",
                    description = "Guarantee next star drop"
                ) {
                    starDropRandom.nextFloat = 1f
                },
                CheatAction.CustomAction(
                    label = "Clear Achievements",
                    action = {
                        achievementSystem.clearAllAchievements()
                    }
                ),
            )
        ),
        CheatCategory(
            title = "Testing",
            actions = listOf(
                CheatAction.CustomAction(
                    label = "Test Toast",
                    description = "Show notification"
                ) { showToast("Toast test") },
                CheatAction.CustomAction(
                    label = "Top Toast",
                    description = "Show top notification"
                ) { showToast("Top Toast test", isTop = true) },
                CheatAction.CustomAction(
                    label = "Play Ad",
                    action = {
                        viewModelScope.launch {
                            adPlayer.playAd()
                        }
                    }
                ),
                CheatAction.CustomAction(
                    label = "Crash",
                    action = {
                        throw RuntimeException("Test Crash")
                    }
                ),
                CheatAction.CustomSuspendAction(
                    label = "Review Dialog",
                    description = "Show game review prompt"
                ) {
                    eventBus.emit(IEvent.RequestReviewDialog)
                },
            )
        ),
    )

    enum class InventoryTarget {
        PLAYER,
        CITY
    }

    fun changeGameSpeed(speed: Float) {
        gameEngine.gameSpeed = speed
    }

    fun getAllItems(): List<ItemTemplate> = itemTemplates.all()

    fun executeAction(action: CheatAction) {
        when (action) {
            is CheatAction.AddCurrency -> {
                addItemToPlayer(itemTemplates.find(action.itemId), action.amount)
                showToast("Added ${action.amount} ${action.itemId}")
            }
            is CheatAction.CustomAction -> {
                action.action()
            }
            is CheatAction.CustomSuspendAction -> {
                viewModelScope.launch {
                    action.action()
                }
            }
        }
    }

    fun addItemToPlayer(itemTemplate: ItemTemplate, count: Long = 1) {
        inventory.inventory.addItem(itemService.createItem(itemTemplate.id, count))
        showToast("Added $count ${itemTemplate.name} to Player")
    }

    fun addItemToCity(itemTemplate: ItemTemplate, count: Long = 1) {
        cityInventory.inventory.addItem(itemService.createItem(itemTemplate.id, count))
        showToast("Added $count ${itemTemplate.name} to City")
    }

    private fun showToast(message: String, isTop: Boolean = false) {
        viewModelScope.launch {
            eventBus.emit(IEvent.ToastMessage(
                msg = message,
                isTop = isTop
            ))
        }
    }
}

@Composable
fun CheatScreen(
    viewModel: CheatScreenViewModel = koinViewModel()
) {
    var searchText by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableLongStateOf(1L) }
    var gameSpeed by remember { mutableFloatStateOf(viewModel.gameEngine.gameSpeed) }
    var inventoryTarget by remember { mutableStateOf(CheatScreenViewModel.InventoryTarget.PLAYER) }
    var expandedCategories by remember { mutableStateOf(setOf<String>()) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(0.4f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚡ Game Speed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${gameSpeed.prettyPrint()}×",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = gameSpeed,
                        onValueChange = {
                            gameSpeed = it
                            viewModel.changeGameSpeed(gameSpeed)
                        },
                        valueRange = 1f..20.0f,
                        steps = 18
                    )
                }
            }

            QuickActionsCategorized(
                categories = viewModel.cheatCategories,
                expandedCategories = expandedCategories,
                onToggleCategory = { category ->
                    expandedCategories = if (category in expandedCategories) {
                        expandedCategories - category
                    } else {
                        expandedCategories + category
                    }
                },
                onActionClick = { viewModel.executeAction(it) }
            )
        }

        Column(
            modifier = Modifier.weight(0.6f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ItemSearchSection(
                searchText = searchText,
                onSearchChange = { searchText = it },
                selectedQuantity = selectedQuantity,
                onQuantityChange = { selectedQuantity = it },
                inventoryTarget = inventoryTarget,
                onTargetChange = { inventoryTarget = it }
            )

            ItemListSection(
                items = viewModel.getAllItems(),
                searchText = searchText,
                quantity = selectedQuantity,
                inventoryTarget = inventoryTarget,
                onItemClick = { item ->
                    when (inventoryTarget) {
                        CheatScreenViewModel.InventoryTarget.PLAYER ->
                            viewModel.addItemToPlayer(item, selectedQuantity)
                        CheatScreenViewModel.InventoryTarget.CITY ->
                            viewModel.addItemToCity(item, selectedQuantity)
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickActionsCategorized(
    modifier: Modifier = Modifier,
    categories: List<CheatScreenViewModel.CheatCategory>,
    expandedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    onActionClick: (CheatScreenViewModel.CheatAction) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        categories.forEach { category ->
            val isExpanded = category.title in expandedCategories

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Column {
                    Surface(
                        onClick = { onToggleCategory(category.title) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isExpanded) "▼" else "▶",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    if (isExpanded) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            category.actions.forEach { action ->
                                Button(
                                    onClick = { onActionClick(action) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text(
                                        text = action.label,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemSearchSection(
    searchText: String,
    onSearchChange: (String) -> Unit,
    selectedQuantity: Long,
    onQuantityChange: (Long) -> Unit,
    inventoryTarget: CheatScreenViewModel.InventoryTarget,
    onTargetChange: (CheatScreenViewModel.InventoryTarget) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Item Spawner",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = inventoryTarget == CheatScreenViewModel.InventoryTarget.PLAYER,
                onClick = { onTargetChange(CheatScreenViewModel.InventoryTarget.PLAYER) },
                label = {
                    Text(
                        text = "Player Inventory",
                        fontWeight = if (inventoryTarget == CheatScreenViewModel.InventoryTarget.PLAYER)
                            FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = inventoryTarget == CheatScreenViewModel.InventoryTarget.CITY,
                onClick = { onTargetChange(CheatScreenViewModel.InventoryTarget.CITY) },
                label = {
                    Text(
                        text = "City Inventory",
                        fontWeight = if (inventoryTarget == CheatScreenViewModel.InventoryTarget.CITY)
                            FontWeight.Bold else FontWeight.Normal
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search items") },
            placeholder = { Text("Type to search...") },
            leadingIcon = { Icon(Feather.Search, "Search") },
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quantity:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            listOf(1L, 10L, 100L, 1000L, 100000L, 10000000L).forEach { qty ->
                FilterChip(
                    selected = selectedQuantity == qty,
                    onClick = { onQuantityChange(qty) },
                    label = {
                        Text(
                            text = "×$qty",
                            fontWeight = if (selectedQuantity == qty)
                                FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ItemListSection(
    items: List<ItemTemplate>,
    searchText: String,
    quantity: Long,
    inventoryTarget: CheatScreenViewModel.InventoryTarget,
    onItemClick: (ItemTemplate) -> Unit
) {
    val filteredItems = remember(items, searchText) {
        if (searchText.isBlank()) {
            items
        } else {
            items.filter {
                it.name.contains(searchText, ignoreCase = true) ||
                        it.id.contains(searchText, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${filteredItems.size} items",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Target: ${if (inventoryTarget == CheatScreenViewModel.InventoryTarget.PLAYER) "Player" else "City"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        items(filteredItems, key = { it.id }) { item ->
            ItemCard(
                item = item,
                quantity = quantity,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun ItemCard(
    item: ItemTemplate,
    quantity: Long,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    GameImage(
                        iconName = item.id,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "+$quantity",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}