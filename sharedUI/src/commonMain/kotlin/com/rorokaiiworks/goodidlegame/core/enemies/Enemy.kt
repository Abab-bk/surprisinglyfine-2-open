package com.rorokaiiworks.goodidlegame.core.enemies

import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.combat.CombatAffix
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.players.ActorStats
import com.rorokaiiworks.goodidlegame.core.stats.EffectManager
import com.rorokaiiworks.goodidlegame.core.stats.Stats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Enemy(
    val template: EnemyTemplate,
    private val emitEvents: Boolean = true
) : IActor, KoinComponent {
    override val id: String = template.id
    override val name: String = template.name
    override val stats: Stats = Stats(
        listOf(
            ActorStats(template)
        )
    )
    override val effectManager by lazy { EffectManager(this) }
    override val iconName: String get() = template.iconName

    private val scope = CoroutineScope(Dispatchers.Default)
    private val eventBus: EventBus by inject()

    fun applyAffix(affix: CombatAffix) {
        affix.modifiers.forEach { mod ->
            val stat = stats[mod.statId] ?: return@forEach
            // Use channel 1 for affixes to avoid clashing with base stats?
            // Actually ActorStats uses channel 0 implicitly via baseValue?
            // Yes, so we'll use channel 1 for affixes.
            if (mod.type == com.rorokaiiworks.goodidlegame.core.stats.StatModifierType.Flat) {
                stat.addFlatModifier(mod.value, 1)
            } else {
                stat.addPercentModifier(mod.value, 1)
            }
        }
    }

    override fun die() {
        if (!emitEvents) return
        scope.launch {
            eventBus.emit(IEvent.EnemyKilled(enemyId = template.id))
        }
    }
}
