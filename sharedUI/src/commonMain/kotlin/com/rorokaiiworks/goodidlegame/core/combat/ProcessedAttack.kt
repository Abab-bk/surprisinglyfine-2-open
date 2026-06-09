package com.rorokaiiworks.goodidlegame.core.combat

import com.rorokaiiworks.goodidlegame.core.actors.IActor

data class ProcessedAttack(
    val attacker: IActor,
    val defender: IActor,

    val slashDamage: Float,
    val punctureDamage: Float,
    val impactDamage: Float,

    val isOneHanded: Boolean,
    val isTwoHanded: Boolean,
    val isRanged: Boolean,
) {
    val totalDamage = slashDamage + punctureDamage + impactDamage
}