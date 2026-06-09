@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.ui

import com.rorokaiiworks.goodidlegame.core.items.*
import com.rorokaiiworks.goodidlegame.core.quests.Quest
import com.rorokaiiworks.goodidlegame.core.recipes.Recipe
import com.rorokaiiworks.goodidlegame.core.requirements.Requirement
import com.rorokaiiworks.goodidlegame.core.rewards.Reward
import com.rorokaiiworks.goodidlegame.core.skills.Skill
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import com.rorokaiiworks.goodidlegame.core.skills.SkillType
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType
import kotlin.uuid.ExperimentalUuidApi

object PreviewConstants {
    val testItemSlot = ItemSlot(
        id = "test_item_slot",
        name = "Weapon",
        acceptType =  setOf(ItemType.Sword)
    )

    val testQuest = Quest(
        id = "test_quest",
        rewards = listOf(
            Reward.FakeReward("Test Reward")
        ),
        conditions = listOf(
            Requirement.EnemyKilled("test_enemy", 10)
        ),
        name = "Test Quest"
    )

    val testItemTemplate = ItemTemplate(
        id = "test_item",
        name = "Test Item",
        modifiers = listOf(
            StatModifier(
                statId = "stat_test",
                value = 10f,
                type = StatModifierType.Flat,
                channel = 0
            ),
            StatModifier(
                statId = "stat_test",
                value = 10f,
                type = StatModifierType.Percent,
                channel = 0
            ),
        )
    )

    val testItem = Item(
        template = testItemTemplate,
        count = 1
    )

    val testRecipe = Recipe(
        id = "test_recipe",
        product = ItemEntry(
            itemId = "testProduct",
            count = 1
        ),
        required = ItemEntry(
            itemId = testItemTemplate.id,
            count = 1
        ),
    )

    val testSkillTemplate = SkillTemplate(
        id = "test_skill",
        name = "Test Skill",
        skillType = SkillType.Gather,
        desc = "Test Skill Description",
    )
    val testSkill = Skill(
        template = testSkillTemplate,
    )

    val testSkillAction = SkillAction.NormalSkillAction(
        id = "test_skill_action",
        name = "Test Skill Action",
        skillId = testSkillTemplate.id,
        tier = 1
    )
}