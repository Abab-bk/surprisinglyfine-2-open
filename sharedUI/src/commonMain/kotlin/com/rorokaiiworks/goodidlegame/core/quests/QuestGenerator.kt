package com.rorokaiiworks.goodidlegame.core.quests

import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.enemies.EnemyTemplate
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.requirements.Requirement
import com.rorokaiiworks.goodidlegame.core.rewards.Reward
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.SkillTemplate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class QuestGenerator(val random: Random) : KoinComponent {
    private val enemyTemplates: DataTable<EnemyTemplate> by inject(named<EnemyTemplate>())
    private val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    private val skillTemplates: DataTable<SkillTemplate> by inject(named<SkillTemplate>())
    private val playerSkills: PlayerSkills by inject()

    private val questGenerators: List<() -> Quest> = listOf(
        ::generateEnemyKilledQuest,
        ::generateItemCollectedQuest,
        ::generateFinishSkillActionQuest
    )

    fun generateDailyQuests(): List<Quest> {
        val dailyQuests = mutableListOf<Quest>()

        repeat(9) {
            val quest = questGenerators.random(random).invoke()
            dailyQuests.add(quest)
        }

        return dailyQuests
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateEnemyKilledQuest(): Quest {
        val tier = pickQuestTier()
        val enemy = enemyTemplates
            .all()
            .filter { it.tier in 0..tier }
            .random(random)

        val count = random.nextInt(getEnemyKillCountRange(tier))
        val reward = getCoinsReward(
            questTier = tier,
            difficultyScore = count * (0.8f + enemy.tier.coerceAtLeast(1) * 0.35f)
        )

        return Quest(
            id = "EnemyKilled_${enemy.id}_${Uuid.random()}",
            rewards = listOf(Reward.ItemReward("coins", reward)),
            conditions = listOf(Requirement.EnemyKilled(enemy.id, count))
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateItemCollectedQuest(): Quest {
        val tier = pickQuestTier()
        val item = itemTemplates
            .all()
            .filter { it.tier in 0..tier }
            .random(random)

        val count = random.nextInt(getItemCollectCountRange(tier))
        val reward = getCoinsReward(
            questTier = tier,
            difficultyScore = count * (0.55f + item.tier.coerceAtLeast(1) * 0.25f)
        )

        return Quest(
            id = "ItemCollected_${item.id}_${Uuid.random()}",
            rewards = listOf(Reward.ItemReward("coins", reward)),
            conditions = listOf(Requirement.ItemCollected(item.id, count))
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateFinishSkillActionQuest(): Quest {
        val tier = pickQuestTier()
        val skill = skillTemplates.all().random(random)
        val need = random.nextInt(getSkillActionCountRange(tier))
        val reward = getCoinsReward(
            questTier = tier,
            difficultyScore = need * (1.0f + tier * 0.30f)
        )

        return Quest(
            id = "FinishSkillAction_${skill.id}_${Uuid.random()}",
            rewards = listOf(Reward.ItemReward("coins", reward)),
            conditions = listOf(Requirement.FinishSkillAction(
                skillId = skill.id,
                need = need
            )),
        )
    }

    private fun pickQuestTier(): Int {
        val maxTier = playerSkills.skills.values.maxOf { it.getTier() }.coerceIn(1, 8)
        val offset = listOf(-1, 0, 0, 1).random(random)
        return (maxTier + offset).coerceIn(1, 8)
    }

    private fun getEnemyKillCountRange(tier: Int): IntRange = when (tier) {
        in 1..2 -> 6..10
        in 3..4 -> 9..14
        in 5..6 -> 12..18
        else -> 15..22
    }

    private fun getItemCollectCountRange(tier: Int): IntRange = when (tier) {
        in 1..2 -> 10..16
        in 3..4 -> 14..22
        in 5..6 -> 18..28
        else -> 24..34
    }

    private fun getSkillActionCountRange(tier: Int): IntRange = when (tier) {
        in 1..2 -> 3..5
        in 3..4 -> 4..7
        in 5..6 -> 6..9
        else -> 8..12
    }

    private fun getCoinsReward(questTier: Int, difficultyScore: Float): Long {
        val maxSkillLevel = playerSkills.skills.values.maxBy { it.level }
        val levelFactor = sqrt(maxSkillLevel.level.toFloat().coerceAtLeast(1f))
        val baseReward = 60f + questTier * 40f + levelFactor * 22f + difficultyScore * 18f
        val variance = random.nextDouble(0.92, 1.08).toFloat()
        return (baseReward * variance).roundToLong().coerceAtLeast(80)
    }

    companion object {
        val QUEST_GENERATE_INTERVAL: Duration = 24.toDuration(
            unit = DurationUnit.HOURS
        )
    }
}
