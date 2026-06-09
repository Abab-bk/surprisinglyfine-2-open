package com.rorokaiiworks.goodidlegame.dlcSocietal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.tutorial.TutorialSystem
import com.rorokaiiworks.goodidlegame.core.tutorial.allTutorials
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTier
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.City
import com.rorokaiiworks.goodidlegame.dlcSocietal.ui.CityScreenDestination.*
import com.rorokaiiworks.goodidlegame.dlcSocietal.ui.policy.PolicyScreen
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

@Serializable
enum class CityScreenDestination(val id: String, val title: String, val buildingTier: BuildingTier?) {
    CityPort("city_port", i18nWrapper("Port"), null),
    Policies("city_policy", i18nWrapper("Policies"), null),
    Banking("city_banking", i18nWrapper("Banking"), null),
    Inventory("city_inventory", i18nWrapper("Repository"), null),
    Farmers("farmer", i18nWrapper("Farmers"), BuildingTier.Farmer),
    Workers("worker", i18nWrapper("Workers"), BuildingTier.Worker),
    Astrologers("astrologer", i18nWrapper("Astrologers"), BuildingTier.Astrologer),
    Alchemists("alchemist", i18nWrapper("Alchemists"), BuildingTier.Alchemist),
    GreatToken("great_token", i18nWrapper("GreatToken"), null),
    TheOffice("the_office", i18nWrapper("The Office"), null),
}


class CityScreenViewModel : ViewModel(), KoinComponent {
    val buildingTemplates: DataTable<BuildingTemplate> by inject(named<BuildingTemplate>())
    val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    var currentDestination by mutableStateOf(CityPort)
    val tutorialSystem: TutorialSystem by inject()
    val city: City by inject()

    val buildingScreenCaches = mutableMapOf<BuildingTier, BuildingScreenCache>()

    fun getBuildingScreenCache(type: BuildingTier): BuildingScreenCache {
        if (buildingScreenCaches.containsKey(type)) return buildingScreenCaches[type]!!
        val cache = BuildingScreenCache(
            buildingTemplates = buildingTemplates.all().filter { it.buildingTier == type }
        )

        buildingScreenCaches[type] = cache

        return cache
    }
}


data class BuildingScreenCache(
    val buildingTemplates: List<BuildingTemplate>,
)


@Composable
fun CityScreen(
    modifier: Modifier = Modifier,
    initialDestination: CityScreenDestination = CityPort,
    viewModel: CityScreenViewModel = koinViewModel()
) {
    val stats = viewModel.city.stats

    LaunchedEffect(initialDestination) {
        viewModel.currentDestination = initialDestination
    }

    LaunchedEffect(Unit) {
        if (!viewModel.city.finishedWelcomeTutorial) {
            viewModel.tutorialSystem.start(allTutorials.first { it.id == "tutorial_city_start" })
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        ) {
            when (viewModel.currentDestination) {
                Farmers,
                Workers,
                Astrologers,
                Alchemists -> CityScreenBuildingScreen(
                    itemTemplates = viewModel.itemTemplates,
                    buildingTemplates = viewModel
                        .getBuildingScreenCache(viewModel.currentDestination.buildingTier!!)
                        .buildingTemplates,
                    type = viewModel.currentDestination.buildingTier!!,
                    onBuild = { buildingTemplate, count ->
                        viewModel.city.addBuilding(buildingTemplate, count)
                    },
                    buildingStats = stats.buildings,
                )

                Banking -> {
                    BankScreen()
                }

                Inventory -> CityInventoryScreen()
                Policies -> PolicyScreen()
                CityPort -> CityPortScreen()
                GreatToken -> GreatTokenScreen()
                TheOffice -> CityOfficeScreen()
            }
        }
    }
}
