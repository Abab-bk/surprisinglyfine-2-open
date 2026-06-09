package com.rorokaiiworks.goodidlegame.ui.talents

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.composables.icons.feather.Feather
import com.composables.icons.feather.X
import com.moly3.dataviz.core.graph.model.GraphNode
import com.moly3.dataviz.core.graph.model.GraphViewSettings
import com.moly3.dataviz.graph.ui.Graph
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.talents.Talent
import com.rorokaiiworks.goodidlegame.core.talents.TalentTree
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.inventory.ItemModifiersPanel
import kotlinx.coroutines.Dispatchers
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class TalentTreeScreenViewModel : ViewModel(), KoinComponent {
    val talentTree: TalentTree by inject()
    val playerInventory: PlayerInventory by inject()

    var selectedTalent by mutableStateOf<Talent?>(null)
        private set

    fun onTalentClick(talent: Talent) {
        selectedTalent = talent
    }

    fun dismissTalentDetail() {
        selectedTalent = null
    }
}


@Composable
fun TalentTreeScreen(
    viewModel: TalentTreeScreenViewModel = koinViewModel(),
) {
    val talentTree = viewModel.talentTree

    val lockedColor = MaterialTheme.colorScheme.outlineVariant.value
    val unlockedColor = MaterialTheme.colorScheme.secondaryContainer.value
    val canLevelUpColor = MaterialTheme.colorScheme.primary.value
    val maxLevelColor = MaterialTheme.colorScheme.tertiary.value

    var graphState by remember(
        talentTree,
        lockedColor,
        unlockedColor,
        canLevelUpColor,
        maxLevelColor,
    ) {
        mutableStateOf(
            createInitialGraphState(
                talentTree = talentTree,
                lockedColor = lockedColor,
                unlockedColor = unlockedColor,
                canLevelUpColor = canLevelUpColor,
                maxLevelColor = maxLevelColor,
            )
        )
    }
    val selectedTalent = viewModel.selectedTalent

    Box(modifier = Modifier.fillMaxSize()) {
        Graph(
            connections = graphState.connections,
            stateNodes = graphState.graphNodes,
            viewSettings = graphState.graphViewSettings,
            coordinates = graphState.coordinates,
            velocities = graphState.velocities,
            zoom = graphState.zoom,
            onZoomChange = { newZoom ->
                graphState = graphState.copy(zoom = newZoom)
            },
            userPosition = graphState.graphUserPosition,
            onCentralGlobalPosition = { offset ->
                graphState = graphState.copy(
                    graphUserPosition = graphState.graphUserPosition + offset
                )
            },
            onNodeClick = { node -> viewModel.onTalentClick(node.data) },
            onCoordinatesUpdate = { coordinates ->
                graphState = graphState.copy(
                    coordinates = coordinates
                )
            },
            onVelocitiesUpdate = { velocities ->
                graphState = graphState.copy(
                    velocities = velocities
                )
            },
            primaryColor = MaterialTheme.colorScheme.primary,
            fontColor = MaterialTheme.colorScheme.onSurface,
            io = Dispatchers.Default,
            circleLineColor = MaterialTheme.colorScheme.outline,
            circleColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        FilledTonalButton(
            onClick = { graphState = graphState.copy(isShowSettings = !graphState.isShowSettings) },
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
        ) {
            Text("调试参数")
        }

        // 3. 调试面板显示逻辑
        AnimatedVisibility(
            visible = graphState.isShowSettings,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp)
        ) {
            GraphDebugPanel(
                settings = graphState.graphViewSettings,
                onSettingsChange = { newSettings ->
                    graphState = graphState.copy(graphViewSettings = newSettings)
                },
                onClose = { graphState = graphState.copy(isShowSettings = false) }
            )
        }

        AnimatedVisibility(
            visible = selectedTalent != null,
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            selectedTalent?.let { talent ->
                TalentDetailPanel(
                    talent = talent,
                    canLevelUp = viewModel.talentTree.canLevelUp(talent),
                    onLevelUp = {
                        viewModel.talentTree.levelUp(talent.template.id)
                        graphState = graphState.copy(
                            graphNodes = calculateGraphNodes(
                                talentTree = talentTree,
                                lockedColor = lockedColor,
                                unlockedColor = unlockedColor,
                                canLevelUpColor = canLevelUpColor,
                                maxLevelColor = maxLevelColor,
                            )
                        ) },
                    onDismiss = { viewModel.dismissTalentDetail() }
                )
            }
        }
    }
}

@Composable
private fun TalentDetailPanel(
    talent: Talent,
    canLevelUp: Boolean,
    onLevelUp: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasEffects = talent.template.effects.isNotEmpty()
    val maxLevel = talent.template.effects.size
    val isMaxLevel = talent.level >= maxLevel
    val canShowCurrentEffects = hasEffects && talent.level > 0
    val canShowNextLevel = hasEffects && !isMaxLevel

    BaseCard(
        modifier = Modifier
            .fillMaxHeight()
            .width(360.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = talent.template.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Feather.X,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LevelProgressBar(
                currentLevel = talent.level,
                maxLevel = maxLevel
            )

            if (canShowCurrentEffects) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "当前效果",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    EffectsList(
                        effects = talent.template.effects[talent.level - 1].effects
                    )
                }
            }

            if (canShowNextLevel) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "下一级",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    EffectsList(
                        effects = talent.template.effects[talent.level].effects,
                        isNextLevel = true
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (hasEffects) {
                Button(
                    onClick = onLevelUp,
                    enabled = canLevelUp && !isMaxLevel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isMaxLevel) {
                        Text(text = "已满级")
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "升级: ${talent.template.effects[talent.level].cost}")

                            GameImage(
                                iconName = "star"
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun LevelProgressBar(
    currentLevel: Int,
    maxLevel: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Lv. $currentLevel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Max. $maxLevel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { if (maxLevel > 0) currentLevel.toFloat() / maxLevel else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}


@Composable
private fun EffectsList(
    effects: List<StatModifier>,
    isNextLevel: Boolean = false
) {
    val alpha by animateFloatAsState(
        targetValue = if (isNextLevel) 0.7f else 1f,
        label = "effectsAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (effects.isEmpty()) {
            Text(
                text = "无额外效果",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            ItemModifiersPanel(modifiers = effects)
        }
    }
}


private fun calculateGraphNodes(
    talentTree: TalentTree,
    lockedColor: ULong,
    unlockedColor: ULong,
    canLevelUpColor: ULong,
    maxLevelColor: ULong,
): List<TalentGraphNode> {
    return talentTree.allTalents.map { talent ->
        TalentGraphNode(
            id = talent.template.id,
            name = talent.template.name,
            data = talent,
            colorValue = when {
                talent.locked -> lockedColor
                talent.isMaxLevel -> maxLevelColor
                talentTree.canLevelUp(talent) -> canLevelUpColor
                else -> unlockedColor
            }
        )
    }
}

private fun calculateTalentConnectionMap(
    talentTree: TalentTree,
): Map<String, List<String>> {
    val connectionsMap = mutableMapOf<String, List<String>>()
    talentTree.allTalents.forEach { talent ->
        if (talent.template.connections.isNotEmpty()) {
            connectionsMap[talent.template.id] = talent.template.connections
        }
    }
    return connectionsMap
}

private fun createInitialGraphState(
    talentTree: TalentTree,
    lockedColor: ULong,
    unlockedColor: ULong,
    canLevelUpColor: ULong,
    maxLevelColor: ULong,
): GraphState {
    val talentTreeSettings = GraphViewSettings.Default.copy(
        centerForce = 0.0088f,
        linkForce = 10f,
        linkDistance = 100f,
        repelForce = 20000f,
        circleSize = 20f,
        connectedRepulsionMultiplier = 0.3f,
        mutualConnectionRepulsionMultiplier = 0.05f,
        unconnectedRepulsionMultiplier = 1.0f,
        longDistanceLinkMultiplier = 1f,
        clusteringForce = 1f,
        minMutualConnectionsForClustering = 10,
        maxForce = 15f,
        maxConnectionsForFullProcessing = 100,
        spatialOptimizationThreshold = 50
    )

    return GraphState(
        graphNodes = calculateGraphNodes(
            talentTree = talentTree,
            lockedColor = lockedColor,
            unlockedColor = unlockedColor,
            canLevelUpColor = canLevelUpColor,
            maxLevelColor = maxLevelColor,
        ),
        connections = calculateTalentConnectionMap(talentTree),
        zoom = 1f,
        graphUserPosition = Offset.Zero,
        graphViewSettings = talentTreeSettings,
        isShowSettings = false
    )
}


@Composable
fun GraphDebugPanel(
    settings: GraphViewSettings,
    onSettingsChange: (GraphViewSettings) -> Unit,
    onClose: () -> Unit
) {
    BaseCard(
        modifier = Modifier
            .width(320.dp) // 稍微加宽一点点
            .fillMaxHeight(0.8f)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("物理引擎参数", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onClose) {
                    Icon(Feather.X, contentDescription = "Close")
                }
            }

            @Composable
            fun <T : Number> SettingSlider(
                label: String,
                value: T,
                range: ClosedFloatingPointRange<Float>,
                onValueChange: (Float) -> Unit
            ) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = if (value is Int) value.toString() else value.toString().take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = value.toFloat(),
                        onValueChange = onValueChange,
                        valueRange = range,
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            // --- 核心力学参数 ---
            SettingSlider("中心引力 (centerForce)", settings.centerForce, 0f..0.05f) {
                onSettingsChange(settings.copy(centerForce = it))
            }
            SettingSlider("连线拉力 (linkForce)", settings.linkForce, 0f..50f) {
                onSettingsChange(settings.copy(linkForce = it))
            }
            SettingSlider("排斥力 (repelForce)", settings.repelForce, 0f..50000f) {
                onSettingsChange(settings.copy(repelForce = it))
            }
            SettingSlider("聚集力 (clusteringForce)", settings.clusteringForce, 0f..5f) {
                onSettingsChange(settings.copy(clusteringForce = it))
            }

            // --- 距离与大小 ---
            SettingSlider("连线理想距离 (linkDistance)", settings.linkDistance, 10f..500f) {
                onSettingsChange(settings.copy(linkDistance = it))
            }
            SettingSlider("节点大小 (circleSize)", settings.circleSize, 5f..50f) {
                onSettingsChange(settings.copy(circleSize = it))
            }

            // --- 惩罚因子 (Multipliers) ---
            SettingSlider("已连接排斥倍率", settings.connectedRepulsionMultiplier, 0f..1f) {
                onSettingsChange(settings.copy(connectedRepulsionMultiplier = it))
            }
            SettingSlider("双向连接排斥倍率", settings.mutualConnectionRepulsionMultiplier, 0f..1f) {
                onSettingsChange(settings.copy(mutualConnectionRepulsionMultiplier = it))
            }
            SettingSlider("未连接排斥倍率", settings.unconnectedRepulsionMultiplier, 0f..2f) {
                onSettingsChange(settings.copy(unconnectedRepulsionMultiplier = it))
            }
            SettingSlider("长距离连接倍率", settings.longDistanceLinkMultiplier, 0f..2f) {
                onSettingsChange(settings.copy(longDistanceLinkMultiplier = it))
            }

            // --- 性能与阈值 (Int 类型处理) ---
            SettingSlider("最大力限制 (maxForce)", settings.maxForce, 1f..100f) {
                onSettingsChange(settings.copy(maxForce = it))
            }
            SettingSlider("聚集触发最小连接数", settings.minMutualConnectionsForClustering, 0f..20f) {
                onSettingsChange(settings.copy(minMutualConnectionsForClustering = it.toInt()))
            }
            SettingSlider("全量处理连接数上限", settings.maxConnectionsForFullProcessing, 0f..500f) {
                onSettingsChange(settings.copy(maxConnectionsForFullProcessing = it.toInt()))
            }
            SettingSlider("空间优化阈值", settings.spatialOptimizationThreshold, 0f..200f) {
                onSettingsChange(settings.copy(spatialOptimizationThreshold = it.toInt()))
            }
        }
    }
}


data class GraphState(
    val isShowSettings: Boolean = false,
    val graphNodes: List<TalentGraphNode> = emptyList(),
    val connections: Map<String, List<String>> = emptyMap(),
    val zoom: Float = 1f,
    val graphUserPosition: Offset = Offset.Zero,
    val graphViewSettings: GraphViewSettings = GraphViewSettings.Default,
    val coordinates: Map<String, Offset> = emptyMap(),
    val velocities: Map<String, Offset> = emptyMap(),
)

typealias TalentGraphNode = GraphNode<String, Talent>