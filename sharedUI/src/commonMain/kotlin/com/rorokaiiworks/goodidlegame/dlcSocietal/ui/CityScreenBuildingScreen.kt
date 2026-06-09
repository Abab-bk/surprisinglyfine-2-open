package com.rorokaiiworks.goodidlegame.dlcSocietal.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.Building
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTier
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.BuildingStats
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.City
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import com.rorokaiiworks.goodidlegame.ui.recipes.ProductScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


sealed class CityScreenBuildingScreenState {
    data class Idle(
        val detailsBuilding: Building? = null
    ) : CityScreenBuildingScreenState()

    data class Product(
        val productItemId: String,
    ) : CityScreenBuildingScreenState()
}


class CityScreenBuildingScreenViewModel : ViewModel(), KoinComponent {
    private val _uiState = MutableStateFlow<CityScreenBuildingScreenState>(CityScreenBuildingScreenState.Idle(null))
    val uiState = _uiState.asStateFlow()

    val city: City by inject()
    val cityInventory: CityInventory by inject()
    val playerInventory: PlayerInventory by inject()

    fun onProductBtnClick(itemId: String) {
        _uiState.value = CityScreenBuildingScreenState.Product(
            productItemId = itemId
        )
    }

    fun onCloseProductScreen() {
        _uiState.value = CityScreenBuildingScreenState.Idle(
            detailsBuilding = null
        )
    }

    fun onCloseDetails() {
        _uiState.value = CityScreenBuildingScreenState.Idle(
            detailsBuilding = null
        )
    }

    fun onDetails(building: Building?) {
        if (building == null) return

        _uiState.value = CityScreenBuildingScreenState.Idle(
            detailsBuilding = building
        )
    }
}


@Composable
fun CityScreenBuildingScreen(
    itemTemplates: DataTable<ItemTemplate>,
    buildingTemplates: List<BuildingTemplate>,
    type: BuildingTier,
    viewModel: CityScreenBuildingScreenViewModel = koinViewModel(key = type.toString()),
    buildingStats: List<BuildingStats>,
    onBuild: (BuildingTemplate, Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is CityScreenBuildingScreenState.Idle -> {
                LazyVerticalStaggeredGrid(
                    modifier = Modifier.padding(16.dp),
                    columns = StaggeredGridCells.Adaptive(
                        minSize = 400.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                ) {
                    items(buildingTemplates.filter { it.buildingTier == type }, key = { it.id }) { buildingTemplate ->
                        val building = viewModel.city.buildings[buildingTemplate.id]

                        CityScreenBuildingItem(
                            itemTemplates = itemTemplates,
                            buildingTemplate = buildingTemplate,
                            canBuild = viewModel.city.canAddBuilding(buildingTemplate, 1),
                            onBuild = onBuild,
                            onDetails = { viewModel.onDetails(building) },
                            onProductBtnClick = viewModel::onProductBtnClick,
                            buildingStats = buildingStats.firstOrNull { it.id == buildingTemplate.id },
                        )
                    }
                }

                AnimatedVisibility(
                    visible = state.detailsBuilding != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { viewModel.onCloseDetails() }
                            .background(Color.Black.copy(alpha = 0.6f))
                    )
                }

                AnimatedVisibility(
                    visible = state.detailsBuilding != null,
                    enter = slideInHorizontally(initialOffsetX = { it }),
                    exit = slideOutHorizontally(targetOffsetX = { it }),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.6f),
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp
                    ) {
                        state.detailsBuilding?.let { building ->
                            BuildingDetails(
                                building = building,
                                onClose = viewModel::onCloseDetails
                            )
                        }
                    }
                }
            }

            is CityScreenBuildingScreenState.Product -> {
                ProductScreen(
                    productItemId = state.productItemId,
                    fromInventory = viewModel.playerInventory.inventory,
                    toInventory = viewModel.cityInventory.inventory,
                    onClose = viewModel::onCloseProductScreen
                )
            }
        }
    }
}