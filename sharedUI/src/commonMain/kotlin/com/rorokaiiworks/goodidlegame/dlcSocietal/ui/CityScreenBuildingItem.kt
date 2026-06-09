package com.rorokaiiworks.goodidlegame.dlcSocietal.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Plus
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.prettyPrint
import com.rorokaiiworks.goodidlegame.core.recipes.Recipe
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingState
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingType
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.BuildingStats
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.commons.AnimatedProgressIndicator
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.ConsumeItemEntry
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import kotlinx.coroutines.delay
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun CityScreenBuildingItem(
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject(),
    itemTemplates: DataTable<ItemTemplate>,
    cityInventory: CityInventory = koinInject(),
    buildingTemplate: BuildingTemplate,
    buildingStats: BuildingStats?,
    canBuild: Boolean,
    onBuild: (BuildingTemplate, Int) -> Unit,
    onDetails: () -> Unit,
    onProductBtnClick: (String) -> Unit,
) {
    val isBuilding = remember { mutableStateOf(false) }
    val showAnimation = remember { mutableStateOf(false) }
    val animatedBorderColor by animateColorAsState(
        targetValue = if (showAnimation.value) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 300)
    )
    
    val borderWidth = if (showAnimation.value) 2.dp else 0.dp

    if (isBuilding.value) {
        LaunchedEffect(Unit) {
            showAnimation.value = true
            delay(500)
            showAnimation.value = false
            isBuilding.value = false
            onBuild(buildingTemplate, 1)
        }
    }

    val handleBuild = {
        if (canBuild && !isBuilding.value) {
            isBuilding.value = true
        }
    }

    BaseCard(
        modifier = modifier
            .border(
                width = borderWidth,
                color = animatedBorderColor,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BuildingHeader(
                buildingTemplate = buildingTemplate,
                count = buildingStats?.count ?: 0,
                i18n = i18n
            )

            if (buildingStats != null) {
                BuildingStatus(
                    buildingTemplate = buildingTemplate,
                    buildingStats = buildingStats,
                    i18n = i18n
                )
            }

            if (buildingTemplate.maintenanceBalance != 0 || buildingTemplate.maintenanceWorkforce.isNotEmpty()) {
                BuildingMaintenance(
                    buildingTemplate = buildingTemplate,
                    i18n = i18n
                )
            }

            BuildingResources(
                buildingTemplate = buildingTemplate,
                itemTemplates = itemTemplates,
                cityInventory = cityInventory,
                onProductBtnClick = onProductBtnClick,
                i18n = i18n
            )

            BuildingActions(
                canBuild = canBuild && !isBuilding.value,
                isBuilding = showAnimation.value,
                onBuild = handleBuild,
                onDetails = onDetails,
                i18n = i18n
            )
        }
    }
}

@Composable
private fun BuildingHeader(
    buildingTemplate: BuildingTemplate,
    count: Int,
    i18n: I18n
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GameImage(
            modifier = Modifier.size(48.dp),
            iconName = buildingTemplate.getIconId(),
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = i18n.tr(buildingTemplate.name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "×$count",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun BuildingStatus(
    buildingTemplate: BuildingTemplate,
    buildingStats: BuildingStats,
    i18n: I18n
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (buildingTemplate.buildingType == BuildingType.Production) {
                Text(
                    text = i18n.tr("Productivity" + " ${(buildingStats.productivity * 100).prettyPrint()}%"),
                )

                AnimatedProgressIndicator(
                    modifier = Modifier.height(14.dp),
                    targetValue = buildingStats.tickProgress
                )
            }

            if (buildingTemplate.buildingType == BuildingType.Residences) {
                Text(i18n.tr("Population") + " ${buildingStats.current}")
            }

            when (buildingStats.currentState) {
                BuildingState.PeriodCostsNotEnough -> {
                    StatusBadge(
                        text = i18n.tr("Insufficient Resources"),
                        isError = true
                    )
                }
                BuildingState.WorkforceNotEnough -> {
                    StatusBadge(
                        text = i18n.tr("Insufficient Workforce"),
                        isError = true
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    isError: Boolean = false
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isError)
            MaterialTheme.colorScheme.errorContainer
        else
            MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isError)
                MaterialTheme.colorScheme.onErrorContainer
            else
                MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun BuildingMaintenance(
    buildingTemplate: BuildingTemplate,
    i18n: I18n
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = i18n.tr("Maintenance"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (buildingTemplate.maintenanceBalance != 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GameImage(
                                iconName = "balance",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "${buildingTemplate.maintenanceBalance}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    buildingTemplate.maintenanceWorkforce.forEach { (tier, workforceCount) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GameImage(
                                iconName = tier.id,
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = "$workforceCount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (buildingTemplate.buildingType == BuildingType.Production) {
                VerticalDivider()

                Column(
                    modifier = Modifier.weight(1f).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = i18n.tr("Production Period"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${buildingTemplate.period}s",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
private fun BuildingResources(
    buildingTemplate: BuildingTemplate,
    itemTemplates: DataTable<ItemTemplate>,
    cityInventory: CityInventory,
    onProductBtnClick: (String) -> Unit,
    i18n: I18n
) {
    val sections = remember(buildingTemplate) {
        mutableListOf<Pair<String, List<ItemEntry>>>().apply {
            add(i18nWrapper("Build Cost") to buildingTemplate.buildCosts)
            if (buildingTemplate.periodCosts.isNotEmpty()) {
                add(i18nWrapper("Period Costs") to buildingTemplate.periodCosts)
            }
            if (buildingTemplate.periodYields.isNotEmpty()) {
                add(i18nWrapper("Period Yields") to buildingTemplate.periodYields)
            }
        }
    }

    val totalSections = sections.size

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 1
    ) {
        sections.forEachIndexed { index, (titleKey, items) ->
            val itemModifier = if (totalSections == 3 && index == 0) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.weight(1f)
            }

            ItemEntriesSection(
                modifier = itemModifier,
                title = i18n.tr(titleKey),
                items = items,
                cityInventory = cityInventory,
                onProductBtnClick = if (index == 0) onProductBtnClick else ({})
            )
        }
    }
}

@Composable
private fun BuildingActions(
    modifier: Modifier = Modifier,
    canBuild: Boolean,
    isBuilding: Boolean,
    onBuild: () -> Unit,
    onDetails: () -> Unit,
    i18n: I18n
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            modifier = Modifier.weight(1f),
            enabled = canBuild,
            onClick = onBuild,
        ) {
            if (isBuilding) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(text = i18n.tr("Building..."))
                }
            } else {
                Text(text = i18n.tr("Build"))
            }
        }

        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = onDetails,
            enabled = !isBuilding
        ) {
            Text(text = i18n.tr("Details"))
        }
    }
}

@Composable
private fun ItemEntriesSection(
    modifier: Modifier = Modifier,
    recipes: DataTable<Recipe> = koinInject(named<Recipe>()),
    title: String,
    cityInventory: CityInventory = koinInject(),
    items: List<ItemEntry>,
    onProductBtnClick: ((String) -> Unit)? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        items.forEach { itemEntry ->
            ConsumeItemEntry(
                consume = itemEntry,
                inventory = cityInventory.inventory
            ) {
                if (onProductBtnClick != null &&
                    recipes.all().any { recipe -> recipe.product.itemId == itemEntry.itemId }) {
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = { onProductBtnClick(itemEntry.itemId) },
                    ) {
                        Icon(
                            imageVector = Feather.Plus,
                            contentDescription = "Craft",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}