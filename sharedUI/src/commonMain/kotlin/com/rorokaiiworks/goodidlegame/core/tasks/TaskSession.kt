@file:OptIn(ExperimentalUuidApi::class)

package com.rorokaiiworks.goodidlegame.core.tasks

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.AppDestination
import com.rorokaiiworks.goodidlegame.core.GameFormulas
import com.rorokaiiworks.goodidlegame.core.RandomSource
import com.rorokaiiworks.goodidlegame.core.combat.CombatPhase.*
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.drops.DropTable
import com.rorokaiiworks.goodidlegame.core.enemies.EnemyTemplate
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.Inventory
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.skills.Skill
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.traits.TraitSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi

enum class TaskExecutionMode {
    Live,
    OfflineSimulation
}

sealed interface TaskRepeatConfig {
    object Infinite : TaskRepeatConfig
    object Once : TaskRepeatConfig
    data class Custom(
        val inputState: TextFieldState = TextFieldState()
    ) : TaskRepeatConfig {
        val count: Int get() {
            if (inputState.text.isEmpty()) return 0
            return inputState.text.toString().toInt()
        }
    }
}

class TaskSession(
    val title: String,
    val subTitle: String = "",
    val task: Task,
    val skill: Skill,
    val repeatCount: Int = Int.MAX_VALUE,
    val destination: AppDestination? = null,
    val isPassive: Boolean = false,
    private val executionMode: TaskExecutionMode = TaskExecutionMode.Live,
) : KoinComponent {
    private val playerInventory: PlayerInventory by inject()
    private val player: Player by inject()
    private val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    private val enemyTemplates: DataTable<EnemyTemplate> by inject(named<EnemyTemplate>())
    private val eventBus: EventBus by inject()
    private val itemService: ItemService by inject()
    private val traitSystem: TraitSystem by inject()

    private val starDropRandom: RandomSource by inject { parametersOf(RandomSource.TAG_STAR_DROP) }

    val lootResult = Inventory(
        maxSlots = Int.MAX_VALUE,
    )

    private val scope = CoroutineScope(Dispatchers.Default)

    var repeatCountLeft by mutableIntStateOf(repeatCount)

    var currentCount: Int = 0
    private var skillActionActive: Boolean = false

    var progress: Float by mutableFloatStateOf(0f)
        private set

    var nextTickAddition: Float = 0f

    var totalLootXp: Long = 0L
        private set

    private var startTime: Long = 0L

    private val offlineConsumableCounts = mutableMapOf<String, Long>()

    fun start(currentMills: Long) {
        initializeOfflineConsumables()
        if (executionMode == TaskExecutionMode.Live) {
            skill.propsContainer?.start(player, currentMills)
        }
        startSkillAction(currentMills)
        if (task is Task.Combat) {
            task.combatSession.start(currentMills)
        }
    }

    fun stop() {
        finishSkillAction()
        if (executionMode == TaskExecutionMode.Live) {
            skill.propsContainer?.stop(player)
        }
        if (task is Task.Combat) {
            task.combatSession.cancel()
        }
    }

    private fun handleRewards(dropTable: DropTable?, xp: Long) {
        val items = mutableListOf<Item>()

        dropTable?.let { table ->
            val originalResult = table.pick(itemTemplates = itemTemplates)

            val yieldMultiplier = GameFormulas.calculateSkillYieldMultiplier(skill, player.stats)
            val lootMultiplier = GameFormulas.calculateSkillLootMultiplier(skill, player.stats)
            val totalMultiplier = yieldMultiplier * lootMultiplier

            // 3.7 倍 -> 保底 3 倍，70% 概率再 +1 倍
            val guaranteedBonus = totalMultiplier.toInt()
            val chanceForExtra = totalMultiplier - guaranteedBonus

            val result = originalResult.map { item ->
                val extraFromChance = if (Random.nextFloat() < chanceForExtra) 1 else 0
                val finalCount = item.count * (guaranteedBonus + extraFromChance)
                item.copy(count = finalCount)
            }
            items.addAll(result)
        }

        val combatAdjustedItems = traitSystem.onCombatRewards(task.action, dropTable, items)
        val finalItems = traitSystem.onSkillActionFinish(task.action, combatAdjustedItems)

        lootResult.addItems(finalItems)
        val adjustedXp = if (executionMode == TaskExecutionMode.Live) {
            traitSystem.modifySkillActionXp(task.action, xp)
        } else xp
        totalLootXp += adjustedXp

        if (executionMode == TaskExecutionMode.Live) {
            playerInventory.inventory.addItems(finalItems)
            skill.addXp(adjustedXp, player.stats)
        }

        if (starDropRandom.nextFloat() >= 0.9f) {
            val star = itemService.createItem("star")
            lootResult.addItem(star)

            if (executionMode == TaskExecutionMode.Live) {
                scope.launch {
                    eventBus.emit(IEvent.StarDropped)
                }

                playerInventory.inventory.addItem(star)
            }
        }
    }

    fun tick(currentMills: Long): Boolean {
        if (currentCount >= repeatCount) {
            return false
        }

        val skillSpeedMultiplier = GameFormulas.calculateSkillSpeedMultiplier(
            skill = skill,
            stats = player.stats
        )

        val tickAdditionSeconds = nextTickAddition
        nextTickAddition = 0f

        if (executionMode == TaskExecutionMode.Live && playerInventory.inventory.isFull()) return false

        val consumeItems = task.action.consumeItems
        if (consumeItems != null && !canConsume(consumeItems)) {
            return false
        }

        if (executionMode == TaskExecutionMode.Live) {
            skill.propsContainer?.propSlotsAutoEquip?.forEach {
                if (it.value.isNotEmpty()) {
                    val slot = skill.propsContainer?.propSlots?.find { slot -> slot.id == it.key }
                    if (slot != null && slot.item == null) {
                        val item = playerInventory.inventory.filterItemsByItemId(it.value).firstOrNull()
                        if (item != null) {
                            val prop = itemService.itemToProp(item)
                            prop?.let { prop ->
                                slot.addItem(prop)
                                playerInventory.inventory.removeItem(item.copy(count = 1))
                            }
                        }
                    }
                }
            }
        }

        when (task) {
            is Task.Combat -> {
                val targetMillis = currentMills
                var safety = 0
                while (safety++ < 10_000) {
                    val processedUntil = task.combatSession.advanceTo(targetMillis)

                    while (task.combatSession.defeatedEnemies.isNotEmpty()) {
                        val enemyTemplate = task.combatSession.defeatedEnemies.removeAt(0)
                        handleRewards(task.action.dropTable, task.action.getXp)
                    }

                    if (executionMode == TaskExecutionMode.Live) {
                        skill.propsContainer?.tick(targetMillis, player)
                    }

                    progress = task.combatSession.combatProgress

                    when (task.combatSession.phase) {
                        Defeated -> return false
                        Victory -> {
                            val victoryMillis = processedUntil
                            finishSkillAction()

                            val consumeItems = task.action.consumeItems
                            if (consumeItems != null && !canConsume(consumeItems)) return false
                            removeConsumedItems(consumeItems)

                            traitSystem.afterSkillAction(task.action)

                            task.combatSession.restart(victoryMillis)

                            currentCount += 1
                            repeatCountLeft = repeatCount - currentCount
                            if (currentCount < repeatCount) {
                                startSkillAction(victoryMillis)
                            }
                            if (executionMode == TaskExecutionMode.Live) {
                                scope.launch {
                                    eventBus.emit(IEvent.FinishSkillAction(
                                        skillId = task.action.skillId,
                                        actionId = task.action.id
                                    ))
                                }
                            }

                            // If combat ended earlier than the target time, keep simulating the remaining time.
                            if (victoryMillis >= targetMillis || currentCount >= repeatCount) {
                                return true
                            }
                        }
                        else -> return true
                    }
                }

                return true
            }

            is Task.Train -> {
                if (tickAdditionSeconds != 0f && skillActionActive && skillSpeedMultiplier > 0f) {
                    // tickAdditionSeconds is in "skill seconds", so convert it back to real millis.
                    startTime -= (tickAdditionSeconds * 1000f / skillSpeedMultiplier).toLong()
                }
                val elapsedSkillSeconds = ((currentMills - startTime).coerceAtLeast(0L) / 1000f) * skillSpeedMultiplier

                if (executionMode == TaskExecutionMode.Live) {
                    skill.propsContainer?.tick(currentMills, player)
                }

                val duration = task.action.duration
                if (duration <= 0f || skillSpeedMultiplier <= 0f) {
                    progress = 0f
                    return true
                }

                val rawCompleted = (elapsedSkillSeconds / duration).let {
                    if (it > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else it.toInt()
                }
                val maxCompletable = (repeatCount - currentCount).coerceAtLeast(0)
                val completed = rawCompleted.coerceAtMost(maxCompletable)

                val actionDurationRealMillis = (duration * 1000f / skillSpeedMultiplier)
                    .toLong()
                    .coerceAtLeast(1L)

                for (i in 0 until completed) {
                    if (executionMode == TaskExecutionMode.Live && playerInventory.inventory.isFull()) return false
                    if (consumeItems != null && !canConsume(consumeItems)) return false

                    val completionMillis = startTime + actionDurationRealMillis

                    finishSkillAction()
                    currentCount += 1
                    repeatCountLeft = repeatCount - currentCount

                    removeConsumedItems(consumeItems)
                    handleRewards(task.action.dropTable, task.action.getXp)
                    traitSystem.afterSkillAction(task.action)

                    if (currentCount < repeatCount) {
                        startSkillAction(completionMillis)
                    }
                    if (executionMode == TaskExecutionMode.Live) {
                        scope.launch {
                            eventBus.emit(IEvent.FinishSkillAction(
                                skillId = task.action.skillId,
                                actionId = task.action.id
                            ))
                        }
                    }
                }

                if (currentCount >= repeatCount) {
                    progress = 1f
                    return false
                }

                // Keep startTime consistent with the remainder so progress is correct even with large time jumps.
                val remainderSkillSeconds = (elapsedSkillSeconds - (rawCompleted.coerceAtLeast(0) * duration))
                    .coerceIn(0f, duration)
                val remainderRealMillis = (remainderSkillSeconds * 1000f / skillSpeedMultiplier).toLong()
                if (skillActionActive) {
                    startTime = currentMills - remainderRealMillis
                }

                progress = (remainderSkillSeconds / duration).coerceIn(0f, 1f)

                return true
            }
        }
    }

    private fun initializeOfflineConsumables() {
        if (executionMode != TaskExecutionMode.OfflineSimulation) return
        if (offlineConsumableCounts.isNotEmpty()) return

        playerInventory.inventory.items.forEach { item ->
            offlineConsumableCounts[item.template.id] =
                (offlineConsumableCounts[item.template.id] ?: 0) + item.count
        }
    }

    private fun canConsume(consumeItems: List<ItemEntry>): Boolean {
        return if (executionMode == TaskExecutionMode.Live) {
            playerInventory.inventory.canConsume(consumeItems)
        } else {
            consumeItems.all { (offlineConsumableCounts[it.itemId] ?: 0) >= it.count }
        }
    }

    private fun removeConsumedItems(consumeItems: List<ItemEntry>?) {
        val items = consumeItems ?: return
        if (executionMode == TaskExecutionMode.Live) {
            playerInventory.inventory.removeItems(items)
        } else {
            consumeOfflineItems(items)
        }
    }

    private fun consumeOfflineItems(consumeItems: List<ItemEntry>) {
        consumeItems.forEach { entry ->
            val current = offlineConsumableCounts[entry.itemId] ?: 0
            offlineConsumableCounts[entry.itemId] = (current - entry.count).coerceAtLeast(0)
        }
    }

    private fun startSkillAction(currentMills: Long) {
        if (skillActionActive) return
        traitSystem.beforeSkillAction(task.action)
        startTime = currentMills
        skillActionActive = true
    }

    private fun finishSkillAction() {
        if (!skillActionActive) return
        startTime = 0L
        skillActionActive = false
    }
}
