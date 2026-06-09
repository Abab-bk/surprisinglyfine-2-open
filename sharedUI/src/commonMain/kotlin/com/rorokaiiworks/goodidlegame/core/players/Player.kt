package com.rorokaiiworks.goodidlegame.core.players

import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.combat.Attack
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import com.rorokaiiworks.goodidlegame.core.loadouts.PlayerLoadouts
import com.rorokaiiworks.goodidlegame.core.skills.SkillsStats
import com.rorokaiiworks.goodidlegame.core.stats.EffectManager
import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.Stats

class Player(
    private val loadouts: PlayerLoadouts
) : IActor {
    override val id: String = "player"

    override var name: String = "Player"

    override val stats: Stats = Stats(
        listOf(
            ActorStats(),
            PlayerStats(),
            SkillsStats()
        )
    )
    override val effectManager by lazy { EffectManager(this) }
    override val iconName: String get() = id

    override fun getAttack(): Attack {
        val slashDamage = stats[StatIds.Actor.SlashDamage]!!.value
        val punctureDamage = stats[StatIds.Actor.PunctureDamage]!!.value
        val impactDamage = stats[StatIds.Actor.ImpactDamage]!!.value

        if (loadouts.weaponItemSlot.item == null) {
            return Attack(
                attacker = this,
                slashDamage = slashDamage,
                punctureDamage = punctureDamage,
                impactDamage = impactDamage,
                isOneHanded = false,
                isTwoHanded = false,
                isRanged = false,
            )
        }

        val isOneHanded = loadouts.weaponItemSlot.item!!.template.type in ItemType.OneHandedWeapons
        val isTwoHanded = loadouts.weaponItemSlot.item!!.template.type in ItemType.TwoHandedWeapons
        val isRanged = loadouts.weaponItemSlot.item!!.template.type in ItemType.RangedWeapons

        return Attack(
            attacker = this,
            slashDamage = slashDamage,
            punctureDamage = punctureDamage,
            impactDamage = impactDamage,
            isOneHanded = isOneHanded,
            isTwoHanded = isTwoHanded,
            isRanged = isRanged,
        )
    }
}