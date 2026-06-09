package com.rorokaiiworks.goodidlegame.core.players

import com.rorokaiiworks.goodidlegame.core.Constants
import com.rorokaiiworks.goodidlegame.core.enemies.EnemyTemplate
import com.rorokaiiworks.goodidlegame.core.stats.Stat
import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.StatSet

class ActorStats : StatSet {
    constructor() : super(StatIds.Actor.Actor) {
        addStat(
            Stat(
                id = StatIds.Actor.MaxHealth,
                baseValue = 100f,
                minValue = 0f,
                channelCount = Constants.ChannelsCount,
            )
        )

        addStat(
            Stat(
                id = StatIds.Actor.Health,
                baseValue = 100f,
                maxValue = 100f,
                minValue = 0f,
                channelCount = Constants.ChannelsCount,
            )
        )

        add(StatIds.Actor.AttackSpeed, 1f)

        add(StatIds.Actor.Armor, 0f)
        add(StatIds.Actor.DamageMultiplier, 1f)
        add(StatIds.Actor.DamageTakenMultiplier, 1f)

        add(StatIds.Actor.SlashDamage, 0f)
        add(StatIds.Actor.PunctureDamage, 0f)
        add(StatIds.Actor.ImpactDamage, 0f)
        add(StatIds.Actor.HitChanceBonus, 0f)
        add(StatIds.Actor.DodgeChanceBonus, 0f)

        add(StatIds.Actor.SlashResistance, 0f)
        add(StatIds.Actor.PunctureResistance, 0f)
        add(StatIds.Actor.ImpactResistance, 0f)

        stats[StatIds.Actor.MaxHealth]?.onValueChanged = {
            stats[StatIds.Actor.Health]?.maxValue = it
            stats[StatIds.Actor.Health]?.baseValue = it
        }
    }

    constructor(enemyTemplate: EnemyTemplate) : this() {
        stats[StatIds.Actor.MaxHealth]?.baseValue = enemyTemplate.maxHealth.toFloat()

        stats[StatIds.Actor.AttackSpeed]?.baseValue = enemyTemplate.attackSpeed

        stats[StatIds.Actor.SlashDamage]?.baseValue = enemyTemplate.slashDamage.toFloat()
        stats[StatIds.Actor.PunctureDamage]?.baseValue = enemyTemplate.punctureDamage.toFloat()
        stats[StatIds.Actor.ImpactDamage]?.baseValue = enemyTemplate.impactDamage.toFloat()

        stats[StatIds.Actor.SlashResistance]?.baseValue = enemyTemplate.slashResistance
        stats[StatIds.Actor.PunctureResistance]?.baseValue = enemyTemplate.punctureResistance
        stats[StatIds.Actor.ImpactResistance]?.baseValue = enemyTemplate.impactResistance
    }
}
