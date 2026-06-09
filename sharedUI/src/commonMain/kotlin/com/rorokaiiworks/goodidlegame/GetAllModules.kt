package com.rorokaiiworks.goodidlegame

import com.rorokaiiworks.goodidlegame.core.ResourceLoader
import com.rorokaiiworks.goodidlegame.core.achievements.Achievement
import com.rorokaiiworks.goodidlegame.core.community.SquareBuildingTemplate
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.di.coreModule
import com.rorokaiiworks.goodidlegame.core.di.uiModule
import com.rorokaiiworks.goodidlegame.core.enemies.EnemyTemplate
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.quests.Quest
import com.rorokaiiworks.goodidlegame.core.recipes.Recipe
import com.rorokaiiworks.goodidlegame.core.shop.ShopItem
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import com.rorokaiiworks.goodidlegame.core.starStore.StarStoreItem
import com.rorokaiiworks.goodidlegame.core.talents.TalentTemplate
import com.rorokaiiworks.goodidlegame.core.traits.TraitTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.PolicyCard
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

suspend fun getOtherModules(): List<Module> {
    var tablesModule: Module?

    val loader = ResourceLoader()

    val questsTable: DataTable<Quest> = DataTable.create<Quest>(
        loader,
        "files/tables/journalQuests",
        listOf(
            "journal_stage_1.yaml",
            "journal_stage_2.yaml",
            "journal_stage_3.yaml"
        )
    )

    val normalSkillActions = DataTable.createTemplates<SkillAction.NormalSkillAction>(
        loader,
        "files/tables",
        listOf(
            "data/skills_skillactions.yml",
            "data/skills_smithingactions.yml",
        )
    )

    val combatSkillActions = DataTable.createTemplates<SkillAction.CombatSkillAction>(
        loader,
        "files/tables",
        listOf(
            "data/skills_combatskillactions.yml",
        )
    )

    val archaeologySkillActions = DataTable.createTemplates<SkillAction.ArchaeologySkillAction>(
        loader,
        "files/tables",
        listOf(
            "data/skills_archaeologyskillactions.yml",
        )
    )

    val skillActionsTable = DataTable<SkillAction>(
        normalSkillActions + combatSkillActions + archaeologySkillActions,
    )

    val skillTemplatesTable = DataTable.create<SkillTemplate>(
        loader,
        "files/tables/data",
        listOf(
            "skilltemplates.yml"
        )
    )

    val itemTemplatesTable = DataTable.create<ItemTemplate>(
        loader,
        "files/tables/data",
        listOf(
            "commonitems.yml",
            "items.yml",
            "potions.yml",
            "dlcsocietalitems.yml",
            "perkequipment.yml",
            "tagsequipment.yml",
            "chests.yml"
        )
    )

    val enemyTemplatesTable = DataTable.create<EnemyTemplate>(
        loader,
        "files/tables/data",
        listOf(
            "enemies_impact.yml",
            "enemies_puncture.yml",
            "enemies_slash.yml",
        )
    )

    val recipesTable = DataTable.create<Recipe>(
        loader,
        "files/tables/data",
        listOf(
            "recipes.yml",
        )
    )

    val shopItems = DataTable.createTemplates<ShopItem.GetItemShopItem>(
        loader,
        "files/tables/shops",
        listOf(
            "shopItems.yaml"
        )
    )
    val shopItemsTable = DataTable<ShopItem>(
        shopItems
    )

    val starStoreItemsTable = DataTable.create<StarStoreItem>(
        loader,
        "files/tables/starStoreItems",
        listOf(
            "starStoreItems.yaml"
        )
    )

    val achievementsTable = DataTable.create<Achievement>(
        loader,
        "files/tables/achievements",
        listOf(
            "achievements_level_10.yaml",
            "achievements_level_40.yaml",
            "achievements_level_90.yaml"
        )
    )

    val talentTemplatesTable = DataTable.create<TalentTemplate>(
        loader,
        "files/tables/talents",
        listOf(
            "alchemyTalents.yaml",
            "combatTalents.yaml",
            "fishingAndCookingTalents.yaml",
            "huntingAndChartingTalents.yaml",
            "miningAndSmeltingTalents.yaml",
            "talentTemplates.yaml",
            "woodcuttingTalents.yaml",
        )
    )

    val buildingTemplatesTable = DataTable.create<BuildingTemplate>(
        loader,
        "files/tables/data",
        listOf(
            "buildingtemplates.yml"
        )
    )

    val policyCardsTable = DataTable.create<PolicyCard>(
        loader,
        "files/tables/policies",
        listOf(
            "policyCards.yaml"
        )
    )

    val traitTemplatesTable = DataTable.create<TraitTemplate>(
        loader,
        "files/tables/data",
        listOf(
            "traittemplates.yml"
        )
    )

    val squareBuildingsTable = DataTable.create<SquareBuildingTemplate>(
        loader,
        "files/tables/square",
        listOf(
            "square_cartography.yaml",
            "square_anvil.yaml",
            "square_cooking_pot.yaml",
            "square_furnace.yaml",
            "square_alchemy_lab.yaml",
        )
    )

    tablesModule = module {
        single<DataTable<SkillTemplate>>(named<SkillTemplate>()) { skillTemplatesTable }
        single<DataTable<SkillAction>>(named<SkillAction>()) { skillActionsTable }
        single<DataTable<ItemTemplate>>(named<ItemTemplate>()) { itemTemplatesTable }
        single<DataTable<EnemyTemplate>>(named<EnemyTemplate>()) { enemyTemplatesTable }
        single<DataTable<Recipe>>(named<Recipe>()) { recipesTable }
        single<DataTable<ShopItem>>(named<ShopItem>()) { shopItemsTable }
        single<DataTable<Quest>>(named<Quest>()) { questsTable }
        single<DataTable<StarStoreItem>>(named<StarStoreItem>()) { starStoreItemsTable }
        single<DataTable<Achievement>>(named<Achievement>()) { achievementsTable }
        single<DataTable<TalentTemplate>>(named<TalentTemplate>()) { talentTemplatesTable }
        single<DataTable<BuildingTemplate>>(named<BuildingTemplate>()) { buildingTemplatesTable }
        single<DataTable<PolicyCard>>(named<PolicyCard>()) { policyCardsTable }
        single<DataTable<TraitTemplate>>(named<TraitTemplate>()) { traitTemplatesTable }
        single<DataTable<SquareBuildingTemplate>>(named<SquareBuildingTemplate>()) { squareBuildingsTable }
    }

    return listOf(
        coreModule,
        uiModule,
        tablesModule
    )
}
