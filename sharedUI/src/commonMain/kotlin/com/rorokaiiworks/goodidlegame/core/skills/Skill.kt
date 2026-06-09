package com.rorokaiiworks.goodidlegame.core.skills

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.GameFormulas
import com.rorokaiiworks.goodidlegame.core.ISoundPlayer
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.ItemType
import com.rorokaiiworks.goodidlegame.core.props.PropSlot
import com.rorokaiiworks.goodidlegame.core.props.PropsContainer
import com.rorokaiiworks.goodidlegame.core.stats.Stats
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi

class Skill(
    val template: SkillTemplate
) : KoinComponent {
    companion object {
        const val MAX_LEVEL = 100
    }

    var level: Int by mutableIntStateOf(1)

    var currentXp by mutableLongStateOf(0L)

    var maxXp by mutableLongStateOf(1L)
        private set

    var propsContainer: PropsContainer? = null
        private set

    var totalXp: Long = 0L

    private val eventBus: EventBus by inject()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val i18n: I18n by inject()
    private val soundPlayer: ISoundPlayer by inject()

    fun getTier(): Int {
        return when (level) {
            in 1..10 -> 1
            in 11..20 -> 2
            in 21..30 -> 3
            in 31..40 -> 4
            in 41..50 -> 5
            in 51..60 -> 6
            in 61..70 -> 7
            in 71..80 -> 8
            in 81..Int.MAX_VALUE -> 8
            else -> -1
        }
    }

    init {
        maxXp = GameFormulas.calculateSkillXpNeededForLevel(level)

        val propSlots = mutableListOf<PropSlot>()
        if (template.skillType == SkillType.Combat) {
            propSlots += PropSlot(
                id = "slot_food",
                name = i18nWrapper("Food"),
                acceptType = setOf(ItemType.Food)
            )
        }

        propsContainer = PropsContainer(
            propSlots = listOf(
                PropSlot(
                    id = "slot_potion",
                    name = when (template.skillType) {
                        SkillType.Combat -> i18nWrapper("Combat Potion")
                        SkillType.Gather -> i18nWrapper("Gather Potion")
                        SkillType.Craft -> i18nWrapper("Craft Potion")
                    },
                    acceptType = setOf(template.getPotionType())
                ),
            ) + propSlots
        )
    }

    fun addOneLevel() {
        level++
        maxXp = GameFormulas.calculateSkillXpNeededForLevel(level)
        scope.launch {
            eventBus.emit(IEvent.SkillLevelUp(template.id))
        }
        soundPlayer.playSound("skillLevelUp")
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addXp(xp: Long, stats: Stats) {
        if (level >= MAX_LEVEL) return

        val xpMultiplier = GameFormulas.calculateSkillXpMultiplier(this, stats)
        val amount = (xp * xpMultiplier).toLong()

        currentXp += amount
        totalXp += amount

        val previousLevel = level
        while (currentXp >= maxXp) {
            currentXp -= maxXp

            addOneLevel()

            scope.launch {
                eventBus.emit(IEvent.ToastMessage(
                    msg = i18n.tr(
                        "Skill {0} level up to {1}",
                        i18n.tr(template.name),
                        level
                    ),
                    iconId = template.id,
                ))
            }
        }

        if (previousLevel != level) {
            maxXp = GameFormulas.calculateSkillXpNeededForLevel(level)
        }
    }
}