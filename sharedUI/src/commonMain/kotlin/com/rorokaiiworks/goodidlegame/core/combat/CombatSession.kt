package com.rorokaiiworks.goodidlegame.core.combat

import androidx.compose.runtime.*
import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.enemies.Enemy
import com.rorokaiiworks.goodidlegame.core.enemies.EnemyTemplate
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.Skill
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.traits.TraitSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import kotlin.random.Random

enum class CombatResult {
    Victory,
    Defeat,
    Cancelled
}

enum class CombatPhase {
    Idle,
    Preparing,
    Fighting,
    Reviving,
    Victory,
    Defeated
}

data class CombatConfig(
    val preparingDuration: Float = 3f,
    val playerReviveBaseTime: Float = 10f,
    val maxDodgeChance: Float = 0.95f,
    val dodgeSkillFactor: Float = 0.05f
)


sealed class CombatEvent {
    object CombatStart : CombatEvent()
    data class PhaseChanged(val phase: CombatPhase) : CombatEvent()
    data class AttackStart(val attacker: IActor) : CombatEvent()
    data class AttackHit(val processedAttack: ProcessedAttack) : CombatEvent()
    data class AttackMissed(val attacker: IActor, val defender: IActor) : CombatEvent()
    data class TraitTriggered(val label: String) : CombatEvent() // label should be untranslated
    data class EnemyDied(val enemy: Enemy) : CombatEvent()
    data class CombatEnd(val result: CombatResult) : CombatEvent()
    data class WaveChanged(val waveIndex: Int, val totalWaves: Int) : CombatEvent()
}

data class PhaseProgress(val startTime: Long, val progress: Float)

class CombatSession(
    val player: IActor,
    val action: SkillAction.CombatSkillAction,
    val skill: Skill,
    private val config: CombatConfig = CombatConfig(),
    private val damageCalculator: DamageCalculator = DamageCalculator,
    private val random: Random = Random
) : KoinComponent {
    private val logger: Logger by inject { parametersOf("CombatSession") }

    private val playerSkills: PlayerSkills by inject()
    private val traitSystem: TraitSystem by inject()
    private val enemyTemplates: DataTable<EnemyTemplate> by inject(named<EnemyTemplate>())

    private val eventBus: EventBus by inject()

    private var _phase: CombatPhase by mutableStateOf(CombatPhase.Idle)
    val phase: CombatPhase get() = _phase

    private var _result: CombatResult? = null
    val result: CombatResult? get() = _result

    private val _progress = mutableStateMapOf<String, PhaseProgress>()

    val prepareProgress get() = _progress["prepare"]?.progress ?: 0f
    val playerAttackProgress get() = _progress["player_attack"]?.progress ?: 0f
    val enemyAttackProgress get() = _progress["enemy_attack"]?.progress ?: 0f

    var reviveProgress by mutableFloatStateOf(0f)

    private var _enemy: Enemy by mutableStateOf(Enemy(enemyTemplates.find(action.enemyIds.first()), emitEvents = true))
    val enemy: Enemy get() = _enemy

    var waveIndex by mutableIntStateOf(1)
    var totalWaves by mutableIntStateOf(Random.nextInt(action.minWaves, action.maxWaves + 1).coerceAtLeast(1))
    val dungeonEnemies = mutableStateListOf<EnemyTemplate>()
    val currentAffixes = mutableStateListOf<CombatAffix>()
    val defeatedEnemies = mutableListOf<EnemyTemplate>()

    val combatProgress: Float
        get() = when (phase) {
            CombatPhase.Victory -> 1f
            CombatPhase.Idle -> 0f
            CombatPhase.Preparing -> prepareProgress
            CombatPhase.Defeated -> 0f
            else -> {
                val waveProgress = (waveIndex - 1).toFloat() / totalWaves
                val currentEnemyProgress = (1f - enemy.healthPercent) / totalWaves
                waveProgress + currentEnemyProgress
            }
        }

    private val _events = MutableSharedFlow<CombatEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<CombatEvent> = _events.asSharedFlow()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            eventBus.events.collect {
                if (it is IEvent.TraitTriggered) {
                    _events.tryEmit(CombatEvent.TraitTriggered(label = it.name))
                }
            }
        }
        generateDungeon()
    }

    private fun generateDungeon() {
        dungeonEnemies.clear()
        val pool = action.enemyIds.map { enemyTemplates.find(it) }
        for (i in 0 until totalWaves) {
            dungeonEnemies.add(pool.random())
        }
    }

    private fun spawnEnemy(wave: Int) {
        val template = dungeonEnemies[wave - 1]
        _enemy = Enemy(template, emitEvents = true)
        currentAffixes.clear()
        
        // Elite or Boss wave
        if (wave == totalWaves && action.isLastWaveBoss) {
            val affixes = CombatAffix.getRandom(1)
            currentAffixes.addAll(affixes)
            affixes.forEach { _enemy.applyAffix(it) }
            _enemy.revive() // Heal to full after applying MaxHealth modifiers
        } else if (wave > 1 && Random.nextFloat() < 0.2f) {
            // Random elite minion
            val affixes = CombatAffix.getRandom(1)
            currentAffixes.addAll(affixes)
            affixes.forEach { _enemy.applyAffix(it) }
            _enemy.revive()
        }
        _events.tryEmit(CombatEvent.WaveChanged(wave, totalWaves))
    }

    fun start() {
        if (phase != CombatPhase.Idle) return

        player.revive()
        waveIndex = 1
        spawnEnemy(waveIndex)
        
        transitionTo(CombatPhase.Preparing)
        resetAllProgress()
    }

    fun start(currentMills: Long) {
        if (phase != CombatPhase.Idle) return

        player.revive()
        waveIndex = 1
        spawnEnemy(waveIndex)

        transitionTo(CombatPhase.Preparing)
        resetAllProgress()
        _progress["prepare"] = PhaseProgress(startTime = currentMills, progress = 0f)
    }

    fun restart() {
        transitionTo(CombatPhase.Idle)
        start()
    }

    fun restart(currentMills: Long) {
        transitionTo(CombatPhase.Idle)
        start(currentMills)
    }

    fun cancel(): CombatResult {
        if (phase == CombatPhase.Victory || phase == CombatPhase.Idle) {
            return result ?: CombatResult.Cancelled
        }
        player.revive()
        endCombat(CombatResult.Cancelled)
        return CombatResult.Cancelled
    }

    fun tick(currentMills: Long) {
        advanceTo(currentMills)
    }

    /**
     * Advances combat state to [targetMills] without requiring fixed-step simulation.
     * Returns the millisecond timestamp that the simulation actually advanced to.
     * If combat ends during the advance, the returned value is the time the combat ended.
     */
    fun advanceTo(targetMills: Long): Long {
        var safety = 0
        while (safety++ < 100) {
            when (phase) {
                CombatPhase.Preparing -> {
                    val prepareStart = _progress["prepare"]?.startTime?.takeIf { it > 0L } ?: run {
                        _progress["prepare"] = PhaseProgress(startTime = targetMills, progress = 0f)
                        targetMills
                    }
                    val prepareEnd = prepareStart + (config.preparingDuration * 1000f).toLong().coerceAtLeast(0L)
                    if (targetMills < prepareEnd) {
                        updateProgress("prepare", targetMills, config.preparingDuration)
                        return targetMills
                    }

                    // Finish preparing at prepareEnd, then continue into fighting.
                    _progress["prepare"] = PhaseProgress(startTime = prepareStart, progress = 1f)
                    transitionTo(CombatPhase.Fighting)
                    resetProgress("player_attack", prepareEnd)
                    resetProgress("enemy_attack", prepareEnd)
                }

                CombatPhase.Fighting -> return advanceFighting(targetMills)

                CombatPhase.Reviving -> {
                    // Reviving is now used for "Completely Defeated" restart or prop revive
                    val reviveStart = reviveStartTime.takeIf { it > 0L } ?: run {
                        reviveStartTime = targetMills
                        targetMills
                    }
                    val reviveEnd = reviveStart + (config.playerReviveBaseTime * 1000f).toLong().coerceAtLeast(0L)
                    if (targetMills < reviveEnd) {
                        tickReviving(targetMills)
                        return targetMills
                    }

                    // Finish reviving at reviveEnd, then continue into fighting from Wave 1
                    reviveStartTime = reviveEnd
                    reviveRemaining = 0f
                    updateRevivingProgress()
                    
                    // Restart dungeon from wave 1
                    player.revive()
                    waveIndex = 1
                    spawnEnemy(waveIndex)
                    
                    transitionTo(CombatPhase.Fighting)
                    resetProgress("player_attack", reviveEnd)
                    resetProgress("enemy_attack", reviveEnd)
                }

                else -> return targetMills
            }
        }

        return targetMills
    }

    private fun transitionTo(newPhase: CombatPhase) {
        if (_phase == newPhase) return
        _phase = newPhase

        logger.d { "transitionTo: $newPhase" }

        _events.tryEmit(CombatEvent.PhaseChanged(newPhase))
        when (newPhase) {
            CombatPhase.Fighting -> _events.tryEmit(CombatEvent.CombatStart)
            else -> Unit
        }
    }

    private fun advanceFighting(targetMills: Long): Long {
        val playerKey = "player_attack"
        val enemyKey = "enemy_attack"

        if (_progress[playerKey] == null) resetProgress(playerKey, targetMills)
        if (_progress[enemyKey] == null) resetProgress(enemyKey, targetMills)

        var lastEventMillis = targetMills
        var safety = 0
        while (safety++ < 100_000) {
            if (enemy.isDead) {
                handleWaveVictory(lastEventMillis)
                if (phase != CombatPhase.Fighting) return lastEventMillis
            }
            if (player.isDead) {
                handlePlayerDeath(lastEventMillis)
                return lastEventMillis
            }

            val playerDurationMs = (damageCalculator.getAttackDuration(player) * 1000f).toLong().coerceAtLeast(1L)
            val enemyDurationMs = (damageCalculator.getAttackDuration(enemy) * 1000f).toLong().coerceAtLeast(1L)

            val playerStart = _progress[playerKey]?.startTime ?: targetMills
            val enemyStart = _progress[enemyKey]?.startTime ?: targetMills

            val playerNext = playerStart + playerDurationMs
            val enemyNext = enemyStart + enemyDurationMs

            val nextEventTime = minOf(playerNext, enemyNext)
            if (nextEventTime > targetMills) break

            if (playerNext <= enemyNext) {
                resetProgress(playerKey, nextEventTime)
                performAttack(nextEventTime, player, enemy)
                lastEventMillis = nextEventTime
            } else {
                resetProgress(enemyKey, nextEventTime)
                performAttack(nextEventTime, enemy, player)
                lastEventMillis = nextEventTime
            }

            if (phase != CombatPhase.Fighting) return lastEventMillis
        }

        // Update UI progress bars for the final target time.
        updateAttackProgress(player, playerKey, targetMills)
        updateAttackProgress(enemy, enemyKey, targetMills)
        return targetMills
    }


    private fun updateAttackProgress(actor: IActor, key: String, currentMills: Long) {
        if (actor.isDead) return
        val duration = damageCalculator.getAttackDuration(actor)
        updateProgress(key, currentMills, duration)
    }

    private fun performAttack(currentMills: Long, attacker: IActor, defender: IActor) {
        _events.tryEmit(CombatEvent.AttackStart(attacker))

        // 计算命中率
        traitSystem.onCombatAttackStart(attacker, defender)

        val hitChance = calculateHitChance(attacker, defender)

        // 判断是否命中，先判定闪避，再判定命中
        val dodgeChance = calculateDodgeChance(attacker, defender)
        val roll = random.nextFloat()
        if (roll !in dodgeChance..<hitChance) {
            _events.tryEmit(CombatEvent.AttackMissed(attacker, defender))
            traitSystem.onCombatAttackMissed(attacker, defender)
            return
        }

        // 命中，计算伤害
        val attack = attacker.getAttack()
        val processed = damageCalculator.process(attack, defender, playerSkills)
        defender.takeDamage(processed)
        _events.tryEmit(CombatEvent.AttackHit(processed))
        traitSystem.onCombatAttackHit(processed)

        if (defender.isDead) {
            defender.die()
            if (defender is Enemy) {
                _events.tryEmit(CombatEvent.EnemyDied(defender))
                traitSystem.onCombatEnemyDied(defender)
                // handleWaveVictory is called in advanceFighting
            } else {
                // handlePlayerDeath is called in advanceFighting
            }
        }
    }

    private fun calculateHitChance(attacker: IActor, defender: IActor): Float {
        val attackerLevel = if (attacker is Enemy) {
            attacker.template.level
        } else skill.level

        val defenderLevel = if (defender is Enemy) {
            defender.template.level
        } else {
            playerSkills.skills["skill_defense"]?.level ?: 1
        }

        val baseChance = damageCalculator.calculateHitChance(attackerLevel, defenderLevel)
        val bonus = attacker.stats[StatIds.Actor.HitChanceBonus]?.value ?: 0f
        return (baseChance + bonus).coerceIn(0.05f, 0.95f)
    }

    private fun calculateDodgeChance(attacker: IActor, defender: IActor): Float {
        val attackerLevel = if (attacker is Enemy) {
            attacker.template.level
        } else skill.level

        val defenderLevel = if (defender is Enemy) {
            defender.template.level
        } else {
            playerSkills.skills["skill_defense"]?.level ?: 1
        }

        val levelDelta = (defenderLevel - attackerLevel).coerceIn(-20, 20)
        val levelBonus = levelDelta * config.dodgeSkillFactor
        val bonus = defender.stats[StatIds.Actor.DodgeChanceBonus]?.value ?: 0f
        val base = 0f
        return (base + levelBonus + bonus).coerceIn(0f, config.maxDodgeChance)
    }


    private fun handleWaveVictory(currentMills: Long) {
        defeatedEnemies.add(enemy.template)
        if (waveIndex < totalWaves) {
            waveIndex++
            spawnEnemy(waveIndex)
            resetProgress("enemy_attack", currentMills)
            _events.tryEmit(CombatEvent.TraitTriggered("Wave Complete!"))
        } else {
            endCombat(CombatResult.Victory)
        }
    }

    private fun handlePlayerDeath(currentMills: Long) {
        // Try to use prop to avoid defeat
        val props = skill.propsContainer
        if (props != null) {
            val oldHealth = player.stats[StatIds.Actor.Health]!!.value
            props.tick(currentMills, player)
            val newHealth = player.stats[StatIds.Actor.Health]!!.value
            
            if (newHealth > oldHealth && !player.isDead) {
                _events.tryEmit(CombatEvent.TraitTriggered("Saved by Prop!"))
                return
            }
        }

        // Completely defeated, restart from wave 1 after a delay
        startReviving(currentMills)
    }


    private fun endCombat(result: CombatResult) {
        if (phase == CombatPhase.Victory || phase == CombatPhase.Defeated) return

        if (result == CombatResult.Victory) {
            transitionTo(CombatPhase.Victory)
        } else {
            transitionTo(CombatPhase.Defeated)
        }

        _result = result
        _events.tryEmit(CombatEvent.CombatEnd(result))
        traitSystem.onCombatEnd(result)
    }

    private fun updateProgress(key: String, currentMills: Long, duration: Float) {
        val existing = _progress[key]
        val startTime = when {
            existing == null -> currentMills
            existing.startTime <= 0L -> currentMills
            existing.startTime > currentMills -> currentMills
            else -> existing.startTime
        }
        val progress = if (duration <= 0f) {
            1f
        } else {
            (((currentMills - startTime).coerceAtLeast(0L)) / 1000f / duration).coerceIn(0f, 1f)
        }
        _progress[key] = PhaseProgress(
            startTime = startTime,
            progress = progress
        )
    }

    private fun resetProgress(key: String, currentMills: Long) {
        _progress[key] = PhaseProgress(
            startTime = currentMills,
            progress = 0f
        )
    }

    private fun resetAllProgress() {
        _progress.clear()
        reviveRemaining = 0f
        updateRevivingProgress()
    }

    private var reviveRemaining by mutableFloatStateOf(0f)
    private var reviveStartTime: Long = 0L

    private fun updateRevivingProgress() {
        reviveProgress = if (phase == CombatPhase.Reviving && config.playerReviveBaseTime > 0f) {
            1f - (reviveRemaining / config.playerReviveBaseTime).coerceIn(0f, 1f)
        } else 0f
    }

    private fun startReviving(currentMills: Long) {
        if (phase == CombatPhase.Reviving) return

        // 玩家阵亡时，敌人清除战斗中的临时效果。
        clearAllEffects(enemy)
        clearPlayerCombatTraitEffects()

        reviveRemaining = config.playerReviveBaseTime
        reviveStartTime = currentMills

        updateRevivingProgress()
        transitionTo(CombatPhase.Reviving)
        resetProgress("player_attack", currentMills)
        resetProgress("enemy_attack", currentMills)
    }

    private fun tickReviving(currentMills: Long) {
        val reviveElapsedSeconds = ((currentMills - reviveStartTime).coerceAtLeast(0L)) / 1000f
        reviveRemaining = (config.playerReviveBaseTime - reviveElapsedSeconds).coerceAtLeast(0f)
        updateRevivingProgress()

        if (reviveRemaining <= 0f) {
            // Restart dungeon from wave 1
            player.revive()
            waveIndex = 1
            spawnEnemy(waveIndex)
            reviveRemaining = 0f
            transitionTo(CombatPhase.Fighting)
        }
    }

    private val IActor.healthPercent: Float
        get() {
            val maxHealth = stats[StatIds.Actor.MaxHealth]!!.value
            if (maxHealth <= 0f) return 0f
            return stats[StatIds.Actor.Health]!!.value / maxHealth
        }

    private fun clearAllEffects(actor: IActor) {
        actor.effectManager.effects
            .map { it.source }
            .toSet()
            .forEach { source -> actor.effectManager.removeAllEffectsBySource(source) }
    }

    private fun clearPlayerCombatTraitEffects() {
        player.effectManager.effects
            .mapNotNull { it.source as? String }
            .filter { it.startsWith("perk_") }
            .toSet()
            .forEach { source -> player.effectManager.removeAllEffectsBySource(source) }
    }

}
