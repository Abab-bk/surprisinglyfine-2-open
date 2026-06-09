package com.rorokaiiworks.goodidlegame

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.FormulaTag
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.CityPort
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.TradeMode
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.TradeRule
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTier
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTier.*
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.City
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

data class Snapshot(
    val time: Instant,
    val isleBucks: Long,
    val balance: Long,
    val farmerBuildings: Int,
    val workerBuildings: Int,
    val astrologerBuildings: Int,
    val alchemistBuildings: Int
)

class CityEmulator : KoinComponent {
    val city: City by inject()
    val buildingTemplates: DataTable<BuildingTemplate> by inject(named<BuildingTemplate>())
    private val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    private val cityPort: CityPort by inject()
    val cityInventory: CityInventory by inject()

    private val _data = MutableStateFlow<List<Snapshot>>(emptyList())
    val data: StateFlow<List<Snapshot>> = _data.asStateFlow()

    // 追踪购买状态
    private var lastPurchaseTick: Int = 0
    private var currentTick: Int = 0
    private val PURCHASE_TIMEOUT_TICKS = 10 // 10个tick未购买就尝试其他建筑

    private val farmerBuildings by lazy {
        buildingTemplates.data.filter { it.value.buildingTier == BuildingTier.Farmer }
    }

    private val workerBuildings by lazy {
        buildingTemplates.data.filter { it.value.buildingTier == BuildingTier.Worker }
    }

    private val astrologerBuildings by lazy {
        buildingTemplates.data.filter { it.value.buildingTier == BuildingTier.Astrologer }
    }

    private val alchemistBuildings by lazy {
        buildingTemplates.data.filter { it.value.buildingTier == BuildingTier.Alchemist }
    }

    init {
        cityPort.tradeRules.clear()
        itemTemplates.data.forEach {
            if (it.value.tags.contains(FormulaTag.CityItem)) {
                cityPort.tradeRules[it.key] = TradeRule(
                    mode = TradeMode.Sell,
                    minStock = 10
                )
            }
        }
        city.forceAddBuilding(buildingTemplates.find("farmer_residences"), 3)
    }

    fun tick(delta: Float, currentMills: Long): Boolean {
        city.tick(delta, currentMills)
        currentTick++
        
        val purchased = buyBuilding()
        if (purchased) {
            lastPurchaseTick = currentTick
        }
        
        upgradeCityPort()

        val stats = city.stats
        val snapshot = Snapshot(
            time = Instant.fromEpochMilliseconds(currentMills),
            isleBucks = stats.isleBucks,
            balance = stats.balance,
            farmerBuildings = city.buildings.values.count { it.template.buildingTier == Farmer },
            workerBuildings = city.buildings.values.count { it.template.buildingTier == Worker },
            astrologerBuildings = city.buildings.values.count { it.template.buildingTier == Astrologer },
            alchemistBuildings = city.buildings.values.count { it.template.buildingTier == Alchemist }
        )

        _data.value += snapshot
        return buildingTemplates.data.all { it.key in city.buildings.keys }
    }

    fun upgradeCityPort() {
        val canConsume = cityInventory.inventory::canConsume
        val consume = cityInventory.inventory::removeItems
        cityPort.upgradeCapacity(canConsume, consume)
        cityPort.upgradeTradeInterval(canConsume, consume)
        cityPort.upgradeSaturationSpeed(canConsume, consume)
    }

    fun buyResistance(tier: BuildingTier) {
        when (tier) {
            Farmer -> city.addBuilding(buildingTemplates.find("farmer_residences"), 1)
            Worker -> city.addBuilding(buildingTemplates.find("worker_residences"), 1)
            Astrologer -> city.addBuilding(buildingTemplates.find("astrologer_residences"), 1)
            Alchemist -> city.addBuilding(buildingTemplates.find("alchemist_residences"), 1)
        }
    }

    fun buyBuilding(): Boolean {
        if (cityInventory.isleBucks <= 500) return false

        // 优先处理生产力低的建筑
        if (city.buildings.any { it.value.productivity <= 0.9f }) {
            val building = city.buildings.entries.first { it.value.productivity <= 0.9f }
            buyResistance(building.value.template.buildingTier)
            return true
        }

        val tierGroups = listOf(farmerBuildings, workerBuildings, astrologerBuildings, alchemistBuildings)
        
        // 检查是否长时间未购买（可能是材料不足）
        val shouldTryAlternative = (currentTick - lastPurchaseTick) >= PURCHASE_TIMEOUT_TICKS

        for (group in tierGroups) {
            val remaining = group.filter { it.key !in city.buildings.keys }.toList()
            if (remaining.isEmpty()) continue

            // 正常策略：按顺序尝试购买
            for ((_, template) in remaining) {
                if (city.canAddBuilding(template, 1)) {
                    city.addBuilding(template, 1)
                    return true
                }
            }
            
            // 如果长时间未购买，尝试在当前tier内购买其他可购买的建筑
            if (shouldTryAlternative) {
                // 尝试购买当前tier内已存在的建筑（增加生产）
                val existingInTier = city.buildings.values
                    .filter { it.template.buildingTier == getTierFromGroup(group) }
                    .sortedBy { it.count } // 优先购买数量少的
                
                for (building in existingInTier) {
                    if (city.canAddBuilding(building.template, 1)) {
                        city.addBuilding(building.template, 1)
                        return true
                    }
                }
            }
            
            return false // 当前tier无法购买，等待下一个tick
        }
        return false
    }
    
    private fun getTierFromGroup(group: Map<String, BuildingTemplate>): BuildingTier? {
        return group.values.firstOrNull()?.buildingTier
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityEmulatorUi(emulator: CityEmulator) {
    val snapshots by emulator.data.collectAsState()
    var running by remember { mutableStateOf(false) }
    var currentMills by remember { mutableStateOf(1000L) }
    var ticks by remember { mutableStateOf(0) }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("City Emulator") },
                    actions = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Button(
                                onClick = { running = !running },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (running) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(if (running) "Stop" else "Start")
                            }

                            OutlinedButton(
                                onClick = {
                                    running = false
                                    currentMills = 1000L
                                    ticks = 0
                                    emulator.city.buildings.clear()
                                    emulator.city.forceAddBuilding(
                                        emulator.buildingTemplates.find("farmer_residences"),
                                        3
                                    )
                                }
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                StatsPanel(emulator, snapshots, currentMills, ticks)

                if (snapshots.isNotEmpty()) {
                    ChartsPanel(snapshots)
                }
            }
        }
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect

        withContext(Dispatchers.Default) {
            while (running) {
                if (currentMills >= 86400000L * 100) { // 100 days
                    running = false
                    break
                }

                val done = emulator.tick(30f, currentMills)
                ticks++

                if (done) {
                    running = false
                    break
                }

                currentMills += 30000

                if (ticks % 10 == 0) {
                    kotlinx.coroutines.delay(1)
                }
            }
        }
    }
}

@Composable
private fun StatsPanel(
    emulator: CityEmulator,
    snapshots: List<Snapshot>,
    currentMills: Long,
    ticks: Int
) {
    val latest = snapshots.lastOrNull()
    val stats = emulator.city.stats

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Statistics", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBox("Time", currentMills.toDuration(DurationUnit.MILLISECONDS).toString())
                StatBox("Ticks", ticks.toString())
                StatBox("Data Points", snapshots.size.toString())
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBox("Isle Bucks", "${stats.isleBucks}")
                StatBox(
                    "Balance",
                    "${latest?.balance ?: 0}",
                    if ((latest?.balance ?: 0) < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text("Buildings: ${latest?.let { it.farmerBuildings + it.workerBuildings + it.astrologerBuildings + it.alchemistBuildings } ?: 0}")

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BuildingChip("Farmer Buildings", latest?.farmerBuildings ?: 0, 0xFF8BC34A)
                BuildingChip("Worker Buildings", latest?.workerBuildings ?: 0, 0xFFFF9800)
                BuildingChip("Astrologer Buildings", latest?.astrologerBuildings ?: 0, 0xFF9C27B0)
                BuildingChip("Alchemist Buildings", latest?.alchemistBuildings ?: 0, 0xFF2196F3)
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = color ?: LocalContentColor.current
        )
    }
}

@Composable
private fun BuildingChip(label: String, count: Int, colorValue: Long) {
    val color = Color(colorValue)
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}

private enum class ChartPage {
    Assets,
    Balance,
    Buildings
}

@Composable
private fun ChartsPanel(snapshots: List<Snapshot>) {
    var selectedPage by remember { mutableStateOf(ChartPage.Assets) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            TabRow(
                selectedTabIndex = selectedPage.ordinal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedPage == ChartPage.Assets,
                    onClick = { selectedPage = ChartPage.Assets },
                    text = { Text("Assets") },
                )
                Tab(
                    selected = selectedPage == ChartPage.Balance,
                    onClick = { selectedPage = ChartPage.Balance },
                    text = { Text("Balance") },
                )
                Tab(
                    selected = selectedPage == ChartPage.Buildings,
                    onClick = { selectedPage = ChartPage.Buildings },
                    text = { Text("Buildings") },
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(16.dp)
            ) {
                when (selectedPage) {
                    ChartPage.Assets -> ChartCard("Assets", snapshots.map { it.isleBucks.toDouble() })
                    ChartPage.Balance -> ChartCard("Balance (Maintenance)", snapshots.map { it.balance.toDouble() })
                    ChartPage.Buildings -> ChartCardMultiLine("Buildings", snapshots)
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, values: List<Double>) {
    LineChart(
        modifier = Modifier.fillMaxSize(),
        data = remember {
            listOf(
                Line(
                    label = title,
                    values = values,
                    color = SolidColor(Color(0xFF23af92)),
                    firstGradientFillColor = Color(0xFF2BC0A1).copy(alpha = .5f),
                    secondGradientFillColor = Color.Transparent,
                    strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                    gradientAnimationDelay = 1000,
                    drawStyle = DrawStyle.Stroke(width = 2.dp),
                )
            )
        },
    )
}

@Composable
private fun ChartCardMultiLine(title: String, values: List<Snapshot>) {
    val linesData = listOf(
        Pair("Farmer", values.map { it.farmerBuildings.toDouble() }),
        Pair("Worker", values.map { it.workerBuildings.toDouble() }),
        Pair("Astrologer", values.map { it.astrologerBuildings.toDouble() }),
        Pair("Alchemist", values.map { it.alchemistBuildings.toDouble() }),
    )

    val colors = listOf(
        Color(0xFF8BC34A),
        Color(0xFFFF9800),
        Color(0xFF9C27B0),
        Color(0xFF2196F3)
    )

    LineChart(
        modifier = Modifier.fillMaxSize(),
        data = remember {
            linesData.mapIndexed { index, pair ->
                Line(
                    label = pair.first,
                    values = pair.second,
                    color = SolidColor(colors[index]),
                    firstGradientFillColor = colors[index].copy(alpha = .3f),
                    secondGradientFillColor = Color.Transparent,
                    strokeAnimationSpec = tween(2000, easing = EaseInOutCubic),
                    gradientAnimationDelay = 1000,
                    drawStyle = DrawStyle.Stroke(width = 2.dp),
                )
            }
        },
    )
}
