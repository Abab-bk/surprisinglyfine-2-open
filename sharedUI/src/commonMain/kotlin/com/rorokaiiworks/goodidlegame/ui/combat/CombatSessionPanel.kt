package com.rorokaiiworks.goodidlegame.ui.combat

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.combat.CombatEvent
import com.rorokaiiworks.goodidlegame.core.combat.CombatPhase.*
import com.rorokaiiworks.goodidlegame.core.combat.CombatSession
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.prettyPrint
import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.trc
import com.rorokaiiworks.goodidlegame.ui.commons.AnimatedProgressIndicator
import com.rorokaiiworks.goodidlegame.ui.commons.BaseCard
import com.rorokaiiworks.goodidlegame.ui.commons.GameImage
import name.kropp.kotlinx.gettext.I18n
import org.koin.compose.koinInject

@Composable
fun CombatSessionPanel(
    player: Player = koinInject(),
    i18n: I18n = koinInject(),
    combatSession: CombatSession
) {
    var playerMissed by remember { mutableStateOf(false) }
    var enemyMissed by remember { mutableStateOf(false) }
    var playerHit by remember { mutableStateOf(false) }
    var enemyHit by remember { mutableStateOf(false) }
    val logEntries = remember(combatSession) { mutableStateListOf<String>() }
    val traitTriggerEntries = remember(combatSession) { mutableStateListOf<String>() }

    LaunchedEffect(combatSession) {
        combatSession.events.collect { event ->
            when (event) {
                is CombatEvent.AttackMissed -> {
                    if (event.defender == combatSession.player) playerMissed = true
                    else enemyMissed = true
                    addCombatLog(
                        logEntries,
                        i18n.tr("{0} missed {1}", event.attacker.name, event.defender.name)
                    )
                }

                is CombatEvent.AttackHit -> {
                    if (event.processedAttack.defender == combatSession.player) playerHit = true
                    else enemyHit = true
                    addCombatLog(
                        logEntries,
                        i18n.tr(
                            "{0} hit {1}, damage: {2}",
                            event.processedAttack.attacker.name,
                            event.processedAttack.defender.name,
                            event.processedAttack.totalDamage.prettyPrint()
                        ),
                    )
                }

                is CombatEvent.EnemyDied -> {
                    addCombatLog(
                        logEntries,
                        message = i18n.tr("{0} was defeated", event.enemy.name),
                        )
                }

                is CombatEvent.TraitTriggered -> {
                    val line = i18n.tr("Trait triggered: {0}", event.label)
                    addCombatLog(logEntries, line)
                    addCombatLog(traitTriggerEntries, line)
                }

                is CombatEvent.CombatEnd -> {
                    addCombatLog(logEntries, i18n.tr("Combat ended: {0}", event.result))
                }

                is CombatEvent.CombatStart -> {
                    addCombatLog(logEntries, i18n.tr("Combat started"))
                }

                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = i18n.trc("Mob waves indicator","Wave {0} / {1}", combatSession.waveIndex, combatSession.totalWaves),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ActorCombatPanel(
                modifier = Modifier.weight(1f),
                actor = combatSession.player,
                name = player.name,
                attackProgress = combatSession.playerAttackProgress,
                miss = playerMissed,
                hit = playerHit,
                onMissConsumed = { playerMissed = false },
                onHitConsumed = { playerHit = false }
            ) {
                if (combatSession.phase == Reviving) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = i18n.tr("Reviving..."),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        AnimatedProgressIndicator(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .width(96.dp)
                                .height(6.dp),
                            targetValue = combatSession.reviveProgress
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (combatSession.phase) {
                    Idle,
                    Preparing -> {
                        SearchingEnemyPlaceholder(
                            progress = combatSession.prepareProgress
                        )
                    }

                    Fighting,
                    Reviving,
                    Victory -> {
                        val enemyName = remember(combatSession.enemy, combatSession.currentAffixes) {
                            val affixesPrefix = combatSession.currentAffixes.joinToString("") { "[${i18n.tr(it.name)}]" }
                            if (affixesPrefix.isNotEmpty()) "$affixesPrefix ${i18n.tr(combatSession.enemy.name)}"
                            else i18n.tr(combatSession.enemy.name)
                        }

                        ActorCombatPanel(
                            actor = combatSession.enemy,
                            name = enemyName,
                            attackProgress = combatSession.enemyAttackProgress,
                            miss = enemyMissed,
                            hit = enemyHit,
                            onMissConsumed = { enemyMissed = false },
                            onHitConsumed = { enemyHit = false }
                        )
                    }

                    Defeated -> {}
                }
            }
        }

        CombatLogPanel(
            entries = logEntries,
            modifier = Modifier.fillMaxWidth()
        )

        TraitTriggerPanel(
            entries = traitTriggerEntries,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


@Composable
fun ActorCombatPanel(
    modifier: Modifier = Modifier,
    actor: IActor,
    name: String = actor.name,
    attackProgress: Float,
    miss: Boolean = false,
    hit: Boolean = false,
    onMissConsumed: () -> Unit = {},
    onHitConsumed: () -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    val shakeOffset by animateDpAsState(
        targetValue = if (hit) 6.dp else 0.dp,
        animationSpec = keyframes {
            durationMillis = 250
            6.dp at 50
            (-6).dp at 100
            4.dp at 150
            0.dp at 250
        },
        finishedListener = {
            if (hit) onHitConsumed()
        }
    )

    var showMiss by remember { mutableStateOf(false) }

    val dodgeAlpha by animateFloatAsState(
        targetValue = if (showMiss) 1f else 0f,
        animationSpec = tween(1000)
    )

    val dodgeOffsetY by animateDpAsState(
        targetValue = if (showMiss) (-22).dp else 0.dp,
        animationSpec = tween(1000),
        finishedListener = {
            if (showMiss) {
                showMiss = false
                onMissConsumed()
            }
        }
    )

    LaunchedEffect(miss) {
        if (miss) showMiss = true
    }

    BaseCard(
        modifier = modifier.offset(x = shakeOffset)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                GameImage(
                    modifier = Modifier.fillMaxSize(),
                    iconName = actor.iconName,
                )

                content()

                if (showMiss) {
                    Text(
                        "Miss!",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = dodgeOffsetY)
                            .alpha(dodgeAlpha),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            Text(text = name)

            AnimatedProgressIndicator(
                modifier = Modifier.height(16.dp),
                targetValue = actor.stats[StatIds.Actor.Health]!!.value / actor.stats[StatIds.Actor.MaxHealth]!!.value
            )
            Text(
                text = "HP ${actor.stats[StatIds.Actor.Health]!!.value.prettyPrint()} / " +
                        actor.stats[StatIds.Actor.MaxHealth]!!.value.prettyPrint(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedProgressIndicator(
                modifier = Modifier.height(16.dp),
                targetValue = attackProgress
            )
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchingEnemyPlaceholder(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularWavyProgressIndicator(
            progress = { progress }
        )
    }
}

@Composable
private fun CombatLogPanel(
    entries: List<String>,
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject()
) {
    BaseCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = i18n.tr("Combat Log"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (entries.isEmpty()) {
                Text(
                    text = i18n.tr("No combat events yet."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entries.asReversed().forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TraitTriggerPanel(
    entries: List<String>,
    modifier: Modifier = Modifier,
    i18n: I18n = koinInject()
) {
    BaseCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = i18n.tr("Traits"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (entries.isEmpty()) {
                Text(
                    text = i18n.tr("No trait activations yet."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = 120.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entries.asReversed().forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}


private fun addCombatLog(
    entries: MutableList<String>,
    message: String
) {
    entries.add(message)
    if (entries.size > 30) {
        entries.removeAt(0)
    }
}
