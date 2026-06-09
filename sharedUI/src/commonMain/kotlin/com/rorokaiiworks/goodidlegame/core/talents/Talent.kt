package com.rorokaiiworks.goodidlegame.core.talents

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Talent(
    val template: TalentTemplate
) : KoinComponent {
    private val player: Player by inject()

    var level: Int by mutableIntStateOf(0)
    var locked: Boolean = !template.initialUnlock

    val isMaxLevel: Boolean
        get() = level >= template.effects.size
    val nextLevelCost: Long?
        get() = if (isMaxLevel) null else template.effects[level - 1 + 1].cost

    fun levelUp(): Resource<Unit> {
        if (isMaxLevel) {
            return Resource.Error(MAX_LEVEL_ERROR, "Talent is already max level")
        }

        level += 1
        applyEffects()
        return Resource.Success(Unit)
    }

    fun applyEffects() {
        player.effectManager.removeAllEffectsBySource(template)

        val effect = Effect(
            id = template.id,
            source = template,
            sourceName = template,
            modifiers = template.effects[level - 1].effects
        )

        player.effectManager.addEffect(effect)
    }

    companion object {
        private const val MAX_LEVEL_ERROR = 1001
    }
}
