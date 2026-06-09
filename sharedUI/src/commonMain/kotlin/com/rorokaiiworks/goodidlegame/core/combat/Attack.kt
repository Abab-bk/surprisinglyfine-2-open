package com.rorokaiiworks.goodidlegame.core.combat

import com.rorokaiiworks.goodidlegame.core.actors.IActor

data class Attack(
    val attacker: IActor,
    val slashDamage: Float,
    val punctureDamage: Float,
    val impactDamage: Float,

    val isOneHanded: Boolean = false,
    val isTwoHanded: Boolean = false,
    val isRanged: Boolean = false,
) {
    val totalDamage = slashDamage + punctureDamage + impactDamage
}