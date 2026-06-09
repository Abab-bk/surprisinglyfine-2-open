package com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings

import androidx.compose.runtime.Stable
import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.CityFormulas
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.populations.PopulationTier
import kotlinx.serialization.Serializable

@Serializable
data class WorkforceItem(
    val tier: PopulationTier,
    val count: Int,
)


@Serializable
data class BuildingTemplate(
    override val id: String,
    val name: String,
    val buildingTier: BuildingTier,
    val buildingType: BuildingType,

    @Stable
    var buildCosts: List<ItemEntry>,

    @Stable
    var periodCosts: List<ItemEntry>,

    @Stable
    var periodYields: List<ItemEntry>,

    val dependencyCount: Int = 0,
    val dependedByCount: Int = 0,

    @Stable
    var period: Float = 1f, // seconds

    @Stable
    var maintenanceBalance: Int = 0,

    @Stable
    var maintenanceWorkforce: List<WorkforceItem> = emptyList(),
) : Template {
    init {
        if (buildingType != BuildingType.Residences) {
            period = CityFormulas.calculateBuildingPeriod(
                dependencyCount = dependencyCount,
                dependedByCount = dependedByCount,
                buildingTier = buildingTier,
                yields = periodYields
            )

            maintenanceBalance = CityFormulas.calculateMaintenanceBalance(
                dependencyCount = dependencyCount,
                dependedByCount = dependedByCount,
                buildingTier = buildingTier
            )

            maintenanceWorkforce = CityFormulas.calculateMaintenanceWorkforce(
                dependencyCount = dependencyCount,
                dependedByCount = dependedByCount,
                buildingTier = buildingTier
            )

            periodYields = CityFormulas.calculatePeriodYields(
                yields = periodYields,
                dependencyCount = dependencyCount,
                dependedByCount = dependedByCount,
                buildingTier = buildingTier
            )

            periodCosts = CityFormulas.calculatePeriodCosts(
                costs = periodCosts,
                dependencyCount = dependencyCount,
                dependedByCount = dependedByCount,
                buildingTier = buildingTier
            )

            buildCosts = CityFormulas.calculateBuildCosts(
                costs = buildCosts,
                dependencyCount = dependencyCount,
                dependedByCount = dependedByCount,
                buildingTier = buildingTier
            )
        }
    }

    fun getIconId(): String =
        if (periodYields.isNotEmpty()) periodYields.first().itemId else id
}


@Serializable
enum class BuildingType(val title: String) {
    Residences("Residences"),
    Production("Production"),
}

@Serializable
enum class BuildingTier(val id: String, val label: String) {
    Farmer("farmer", "Farmer"),
    Worker("worker", "Worker"),
    Astrologer("astrologer", "Astrologer"),
    Alchemist("alchemist", "Alchemist");

    companion object {
        fun BuildingTier.toPopulationTier(): PopulationTier = when (this) {
            Farmer     -> PopulationTier.Farmer
            Worker     -> PopulationTier.Worker
            Astrologer -> PopulationTier.Astrologer
            Alchemist  -> PopulationTier.Alchemist
        }
    }
}
