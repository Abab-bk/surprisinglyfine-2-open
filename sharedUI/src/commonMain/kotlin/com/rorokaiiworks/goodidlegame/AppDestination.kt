package com.rorokaiiworks.goodidlegame

import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import com.rorokaiiworks.goodidlegame.dlcSocietal.ui.CityScreenDestination
import kotlinx.serialization.Serializable

@Serializable
sealed class AppDestination(
    val route: String,
    val group: String,
    val title: String,
) {
    @Serializable
    object LoadoutDestination : AppDestination(
        route = "loadout",
        title = i18nWrapper("Loadout"),
        group = i18nWrapper("Character")
    )

    @Serializable
    object InventoryDestination : AppDestination(
        route = "inventory",
        title = i18nWrapper("Inventory"),
        group = i18nWrapper("Character")
    )

    @Serializable
    object JourneyDestination : AppDestination(
        route = "journey",
        title = i18nWrapper("Journey"),
        group = i18nWrapper("Character")
    )

    @Serializable
    object QuestDestination : AppDestination(
        route = "quests",
        title = i18nWrapper("Quests"),
        group = i18nWrapper("Character")
    )

    @Serializable
    object CommunityDestination : AppDestination(
        route = "community",
        title = i18nWrapper("Community"),
        group = i18nWrapper("Character")
    )

    @Serializable
    object CheatDestination : AppDestination(
        route = "cheat",
        title = i18nWrapper("Cheat"),
        group = i18nWrapper("Dev")
    )

    @Serializable
    object ShopDestination : AppDestination(
        route = "shop",
        title = i18nWrapper("Shop"),
        group = i18nWrapper("Character")
    )

    @Serializable
    object StarStoreDestination : AppDestination(
        route = "starStore",
        title = i18nWrapper("StarStore"),
        group = i18nWrapper("Character")
    )

    @Serializable
    object SettingsDestination : AppDestination(
        route = "settings",
        title = i18nWrapper("Settings"),
        group = i18nWrapper("Profile")
    )

    @Serializable
    object CodexDestination : AppDestination(
        route = "codex",
        title = i18nWrapper("Codex"),
        group = i18nWrapper("Profile")
    )


    @Serializable
    object LeaderboardDestination : AppDestination(
        route = "leaderboard",
        title = i18nWrapper("Leaderboard"),
        group = i18nWrapper("Profile")
    )

    @Serializable
    object AchievementDestination : AppDestination(
        route = "achievements",
        title = i18nWrapper("Achievements"),
        group = i18nWrapper("Profile")
    )


    object TalentDestination : AppDestination(
        route = "talents",
        title = i18nWrapper("Talents"),
        group = i18nWrapper("Character")
    )


    @Serializable
    object TraitDestination : AppDestination(
        route = "traits",
        title = i18nWrapper("Traits"),
        group = i18nWrapper("Character")
    )

    @Serializable
    object CityDestination : AppDestination(
        route = "city",
        title = i18nWrapper("City"),
        group = i18nWrapper("Character")
    )

    @Serializable
    data class CitySubDestination(
        val cityDestination: CityScreenDestination,
    ) : AppDestination(
        route = cityDestination.id,
        title = "",
        group = i18nWrapper("Character")
    )

    @Serializable
    class SkillDestination(
        val skillId: String,
        val skillTitle: String,
        val selectedCategory: String? = null,
    ) : AppDestination(
        route = skillId,
        title = skillTitle,
        group = i18nWrapper("Skills"),
    )
}
