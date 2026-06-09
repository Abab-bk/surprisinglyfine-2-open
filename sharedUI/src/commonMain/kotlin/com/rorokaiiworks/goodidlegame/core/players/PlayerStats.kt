package com.rorokaiiworks.goodidlegame.core.players

import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.StatSet

class PlayerStats : StatSet(StatIds.Player.Player) {
    init {
        add(StatIds.Player.FoodEffect, 1f)

        add(StatIds.Player.OfflineRewardMultiplier, 0.5f)
    }
}