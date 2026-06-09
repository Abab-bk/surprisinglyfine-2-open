@file:OptIn(ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.core.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import com.rorokaiiworks.goodidlegame.AppViewModel
import com.rorokaiiworks.goodidlegame.LaunchSettings
import com.rorokaiiworks.goodidlegame.core.*
import com.rorokaiiworks.goodidlegame.core.codex.Codex
import com.rorokaiiworks.goodidlegame.core.community.Community
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.journey.JourneySystem
import com.rorokaiiworks.goodidlegame.core.loadouts.PlayerLoadouts
import com.rorokaiiworks.goodidlegame.core.mastery.MasteryLevel
import com.rorokaiiworks.goodidlegame.core.offline.OfflineReward
import com.rorokaiiworks.goodidlegame.core.persistent.SaveSystem
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.quests.QuestSystem
import com.rorokaiiworks.goodidlegame.core.reveal.Revealer
import com.rorokaiiworks.goodidlegame.core.settings.SettingsSaver
import com.rorokaiiworks.goodidlegame.core.shop.Shop
import com.rorokaiiworks.goodidlegame.core.shop.ShopItem
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import com.rorokaiiworks.goodidlegame.core.starStore.StarStore
import com.rorokaiiworks.goodidlegame.core.talents.TalentTree
import com.rorokaiiworks.goodidlegame.core.tasks.TaskSystem
import com.rorokaiiworks.goodidlegame.core.traits.TraitSystem
import com.rorokaiiworks.goodidlegame.core.tutorial.TutorialSystem
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.Bank
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.CityPort
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.GreatToken
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.balances.Balance
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.City
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityState
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicyCard
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicySystem
import com.rorokaiiworks.goodidlegame.dlcSocietal.ui.*
import com.rorokaiiworks.goodidlegame.dlcSocietal.ui.policy.PolicyScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.MainViewModel
import com.rorokaiiworks.goodidlegame.ui.Navigator
import com.rorokaiiworks.goodidlegame.ui.Notifier
import com.rorokaiiworks.goodidlegame.ui.StartupViewModel
import com.rorokaiiworks.goodidlegame.ui.achievements.AchievementSystemScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.cheats.CheatScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.codex.CodexScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.community.AltarScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.community.CommunityViewModel
import com.rorokaiiworks.goodidlegame.ui.community.EnchantingTableScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.community.SquareScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.inventory.InventoryScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.leaderBoard.LeaderboardScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.loadout.LoadoutsScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.mainScreen.GameLoopViewModel
import com.rorokaiiworks.goodidlegame.ui.persistent.SaveSystemPanelViewModel
import com.rorokaiiworks.goodidlegame.ui.quests.QuestScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.recipes.ProductScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.settings.SettingsScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.shop.ShopScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.skills.CachedSkillData
import com.rorokaiiworks.goodidlegame.ui.skills.SkillScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.starStore.StarStoreScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.talents.TalentTreeScreenViewModel
import com.rorokaiiworks.goodidlegame.ui.toasts.ToastHostViewModel
import com.rorokaiiworks.goodidlegame.ui.traits.TraitScreenViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.ExperimentalTime

fun preStartModule(
    launchSettings: LaunchSettings,
    isDebug: Boolean,
) = module {
    single { SettingsSaver() }

    single<ITimeProvider> {
        if (isDebug) FakeTimeProvider()
        else RealTimeProvider()
    }
    single { TimeSystem() }

    factory { (tag: String) -> Logger(
        config = loggerConfigInit(platformLogWriter()),
        tag = tag
    ) }

    single { (tag: String) -> RandomSource(
        tag = tag,
    ) }

    single<LaunchSettings> { launchSettings }
    single { Revealer() }
    single { EventBus() }

    single { SaveSystem(
        appFileWriter = get(named(DIQualifiers.AppSaveFileWriter)),
        saveFolder = get(named(DIQualifiers.AppSaveFolder)),
        gameFileWriterFactory = get(named(DIQualifiers.GameSaveFileWriterFactory)),
    ) }

    viewModel { AppViewModel(get()) }
    viewModel { StartupViewModel(get()) }
    viewModel { SaveSystemPanelViewModel() }
    single { TutorialSystem() }
}

val coreModule = module {
    single { Navigator() }
    single { Notifier() }

    single { MasteryLevel() }

    single { Codex(
        eventBus = get(),
        itemTemplates = get(named<ItemTemplate>()),
        masteryLevel = get()
    ) }

    single { TaskSystem() }

    single { ItemService(itemTemplates = get(named<ItemTemplate>()), traitSystem = get()) }

    single { PlayerSkills(get(named<SkillTemplate>())) }
    single { Player(loadouts = get()) }
    single { PlayerInventory(itemTemplates = get(named<ItemTemplate>())) }
    single { PlayerLoadouts() }
    single { Shop(
        dataTable = get(named<ShopItem>()),
        skillActionsTable = get(named<SkillAction>()),
        itemTemplates = get(named<ItemTemplate>())
    ) }
    single { StarStore() }
    single { Community() }

    single { GameState() }

    single { TalentTree() }
    single { TraitSystem() }

    // DLC Societal
    single { CityInventory() }
    single { PolicySystem(cardsTable = get(named<PolicyCard>())) }
    single { City() }
    single { CityPort() }
    single { Balance() }
    single { Bank() }
    single { CityState() }
    single { GreatToken() }

    single { JourneySystem() }
    single { QuestSystem() }

    single { GameEngine() }
}

val uiModule = module {
    viewModel { SettingsScreenViewModel() }
    viewModel { ToastHostViewModel() }

    viewModel { GameLoopViewModel() }
    viewModel { (reward: OfflineReward?) ->
        MainViewModel(
            launchSettings = get(),
            initialOfflineReward = reward
        )
    }

    viewModel { (skillData: CachedSkillData) -> SkillScreenViewModel(skillData) }
    viewModel { CodexScreenViewModel() }

    viewModel { LoadoutsScreenViewModel() }
    viewModel { QuestScreenViewModel() }
    viewModel { ProductScreenViewModel() }
    viewModel { CheatScreenViewModel() }

    viewModel { StarStoreScreenViewModel() }
    viewModel { InventoryScreenViewModel() }
    viewModel { ShopScreenViewModel() }
    viewModel { AchievementSystemScreenViewModel() }
    viewModel { LeaderboardScreenViewModel() }
    viewModel { TalentTreeScreenViewModel() }
    viewModel { TraitScreenViewModel() }
    viewModel { EnchantingTableScreenViewModel() }
    viewModel { CommunityViewModel() }
    viewModel { SquareScreenViewModel() }
    viewModel { AltarScreenViewModel() }

    viewModel { CityScreenViewModel() }
    viewModel { CityScreenBuildingScreenViewModel() }
    viewModel { BankScreenViewModel() }
    viewModel { CityInventoryScreenViewModel() }
    viewModel { PolicyScreenViewModel() }
    viewModel { CityPortScreenViewModel() }
    viewModel { GreatTokenScreenViewModel() }
}
