package com.rorokaiiworks.goodidlegame.ui.mainScreen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import com.rorokaiiworks.goodidlegame.ui.MainViewModel
import com.rorokaiiworks.goodidlegame.ui.cheats.CheatScreen
import com.rorokaiiworks.goodidlegame.ui.codex.CodexScreen
import com.rorokaiiworks.goodidlegame.ui.community.CommunityScreen
import com.rorokaiiworks.goodidlegame.ui.inventory.InventoryScreen
import com.rorokaiiworks.goodidlegame.ui.journey.JourneyScreen
import com.rorokaiiworks.goodidlegame.ui.loadout.LoadoutsScreen
import com.rorokaiiworks.goodidlegame.ui.quests.QuestScreen
import com.rorokaiiworks.goodidlegame.ui.settings.SettingsScreen
import com.rorokaiiworks.goodidlegame.ui.shop.ShopScreen
import com.rorokaiiworks.goodidlegame.ui.skills.CachedSkillData
import com.rorokaiiworks.goodidlegame.ui.skills.SkillScreen
import com.rorokaiiworks.goodidlegame.ui.starStore.StarStoreScreen
import com.composables.icons.feather.Feather
import com.composables.icons.feather.Loader
import com.rorokaiiworks.goodidlegame.DLC
import com.rorokaiiworks.goodidlegame.DLCService
import com.rorokaiiworks.goodidlegame.LaunchSettings
import com.rorokaiiworks.goodidlegame.dlcSocietal.ui.CityScreen
import com.rorokaiiworks.goodidlegame.ui.achievements.AchievementSystemScreen
import com.rorokaiiworks.goodidlegame.ui.leaderBoard.LeaderboardScreen
import com.rorokaiiworks.goodidlegame.ui.talents.TalentTreeScreen
import com.rorokaiiworks.goodidlegame.ui.traits.TraitScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GameNavHost(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    backStack: SnapshotStateList<AppDestination>,
) {
    NavDisplay(
        modifier = modifier,
        backStack = backStack,

        transitionSpec = {
            val duration = 220
            slideInHorizontally(
                animationSpec = tween(duration),
                initialOffsetX = { it / 4 }
            ) + fadeIn(
                animationSpec = tween(duration)
            ) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(duration),
                        targetOffsetX = { -it / 6 }
                    ) + fadeOut(
                animationSpec = tween(duration)
            )
        },

        entryProvider = { key ->
            when (key) {
                AppDestination.LoadoutDestination -> NavEntry(key) {
                    LoadoutsScreen(
                        loadouts = viewModel.loadouts.loadouts,
                        actor = viewModel.player,
                        inventory = viewModel.inventory.inventory
                    )
                }

                AppDestination.InventoryDestination -> NavEntry(key) {
                    InventoryScreen(viewModel.inventory.inventory)
                }

                AppDestination.QuestDestination -> NavEntry(key) {
                    QuestScreen()
                }

                AppDestination.CheatDestination -> NavEntry(key) {
                    CheatScreen()
                }

                AppDestination.ShopDestination -> NavEntry(key) {
                    ShopScreen()
                }

                AppDestination.SettingsDestination -> NavEntry(key) {
                    SettingsScreen()
                }

                AppDestination.CommunityDestination -> NavEntry(key) {
                    CommunityScreen()
                }

                AppDestination.StarStoreDestination -> NavEntry(key) {
                    StarStoreScreen()
                }

                is AppDestination.SkillDestination -> NavEntry(key) {
                    val data = rememberSkillData(viewModel, key.skillId)

                    if (data == null) {
                        Icon(
                            imageVector = Feather.Loader,
                            contentDescription = "loading",
                        )
                    } else {
                        SkillScreen(
                            viewModel = koinViewModel(key = key.skillId) { parametersOf(data) },
                            cachedSkillData = data,
                        )
                    }
                }

                AppDestination.CodexDestination -> NavEntry(key) {
                    CodexScreen()
                }

                AppDestination.JourneyDestination -> NavEntry(key) {
                    JourneyScreen(viewModel.journeySystem)
                }

                AppDestination.LeaderboardDestination -> NavEntry(key) {
                    LeaderboardScreen()
                }

                AppDestination.AchievementDestination -> NavEntry(key) {
                    AchievementSystemScreen()
                }

                AppDestination.TalentDestination -> NavEntry(key) {
                    TalentTreeScreen()
                }

                AppDestination.CityDestination -> NavEntry(key) {
                    CityScreen()
                }

                is AppDestination.CitySubDestination -> NavEntry(key) {
                    CityScreen(
                        initialDestination = key.cityDestination
                    )
                }

                AppDestination.TraitDestination -> NavEntry(key) {
                    TraitScreen()
                }
            }
        }
    )
}

@Composable
fun rememberSkillData(mainViewModel: MainViewModel, skillId: String): CachedSkillData? {
    var data by remember { mutableStateOf(mainViewModel.skillDataCache[skillId]) }

    if (data == null) {
        LaunchedEffect(skillId) {
            withContext(Dispatchers.Default) {
                data = mainViewModel.getSkillData(skillId)
            }
        }
    }

    return data
}

fun buildScreenList(
    skillsTemplates: DataTable<SkillTemplate>,
    launchSettings: LaunchSettings,
    dlcService: DLCService
): List<AppDestination> = buildList {
    add(AppDestination.LoadoutDestination)
    add(AppDestination.InventoryDestination)
    add(AppDestination.ShopDestination)

    add(AppDestination.JourneyDestination)
    add(AppDestination.QuestDestination)

    if (dlcService.enabled(DLC.Societal)) {
        add(AppDestination.CityDestination)
    }

    add(AppDestination.CommunityDestination)
    add(AppDestination.TraitDestination)

    addAll(skillsTemplates.all().map { skill ->
        AppDestination.SkillDestination(
            skillId = skill.id,
            skillTitle = skill.name,
            selectedCategory = skill.selectedCategory,
        )
    })

    add(AppDestination.CodexDestination)
    add(AppDestination.AchievementDestination)
    add(AppDestination.LeaderboardDestination)
    add(AppDestination.SettingsDestination)

    addAll(checkCheatMenu(launchSettings))
}

private fun checkCheatMenu(launchSettings: LaunchSettings): List<AppDestination> {
    if (launchSettings.debugMenu) return listOf(AppDestination.CheatDestination)
    return listOf()
}
