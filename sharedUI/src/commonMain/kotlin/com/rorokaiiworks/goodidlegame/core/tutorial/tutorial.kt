package com.rorokaiiworks.goodidlegame.core.tutorial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.requirements.Requirement
import com.rorokaiiworks.goodidlegame.core.requirements.handleEvent
import com.rorokaiiworks.goodidlegame.core.reveal.Revealer
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.TradeMode
import com.rorokaiiworks.goodidlegame.dlcSocietal.ui.CityScreenDestination
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

data class TutorialRevealKey(
    val id: String,
)

val allTutorials = listOf(
    Tutorial(
        id = "tutorial_city_start",
        steps = listOf(
            TutorialStep(
                text = i18nWrapper("我知道你！大名鼎鼎，多才多艺的精通大师。我知道以你的聪明才智已经知道了城市的运转方式，但我必须向你介绍！你没有选择！"),
                dialog = true,
                conditions = listOf()
            ),
            TutorialStep(
                text = i18nWrapper("你的第一步是获得人口，这可以通过建造农民住宅来实现。请选择农民标签。"),
                conditions = listOf(
                    Requirement.CityBuiltBuilding(
                        buildingId = "farmer_residences",
                        need = 5
                    )
                ),
            ),
            TutorialStep(
                text = i18nWrapper("伟大的尝试！现在，建造一些树厂和锯木厂来生产木板。"),
                conditions = listOf(
                    Requirement.CityBuiltBuilding(
                        buildingId = "farmer_tree_farm",
                        need = 1
                    ),
                    Requirement.CityBuiltBuilding(
                        buildingId = "farmer_sawmill",
                        need = 1
                    )
                ),
            ),
            TutorialStep(
                text = i18nWrapper("太棒了，现在让我们把这些东西卖掉。请先到港口。找到木板，然后选择交易模式为售卖，在时机合适时会自动卖掉它。就这么简单！。"),
                conditions = listOf(
                    Requirement.CityItemTradeMode(
                        itemId = "plank",
                        tradeMode = TradeMode.Sell,
                    )
                ),
            ),
            TutorialStep(
                text = i18nWrapper("伟大的工作！你的终极目标就是合成出伟大代币，彰显出你超脱凡俗的城市治理能力，向着这个目标努力吧！"),
                conditions = listOf(),
                dialog = true
            ),
            TutorialStep(
                text = i18nWrapper("你已经完成了！去实现你的宏图大志吧！"),
                conditions = listOf(),
                dialog = true
            ),
        )
    ),
)


data class TutorialStep(
    val text: String,
    val highlight: TutorialRevealKey? = null,
    val dialog: Boolean = false,
    val conditions: List<Requirement>
)


data class Tutorial(
    val id: String,
    val steps: List<TutorialStep>
)


class TutorialSystem : KoinComponent {
    private val eventBus: EventBus by inject()
    private val logger: Logger by inject { parametersOf("TutorialSystem") }
    private val revealer: Revealer by inject()

    var currentStep: TutorialStep? by mutableStateOf(null)
        private set
    var currentTutorial: Tutorial? = null
        private set

    init {
        CoroutineScope(Dispatchers.Default).launch {
            eventBus.events.collect { event ->
                currentStep?.let {
                    it.conditions.handleEvent(event)
                    if (it.conditions.all { condition -> condition.isMet() }) {
                        logger.i { "condition ${it.conditions} is met" }
                        nextStep()
                    }
                }
            }
        }
    }

    fun nextStep() {
        val tutorial = currentTutorial ?: return
        val steps = tutorial.steps

        val currentIndex = steps.indexOf(currentStep)
        val nextIndex = currentIndex + 1

        if (nextIndex >= steps.size) {
            finishTutorial()
            return
        }

        val nextStep = steps[nextIndex]
        currentStep = nextStep

        nextStep.highlight?.let {
            revealer.tryReveal(it)
        }
    }

    private fun finishTutorial() {
        eventBus.tryEmit(IEvent.TutorialFinished(currentTutorial?.id ?: ""))
        currentStep = null
        currentTutorial = null
        logger.i { "Tutorial finished and cleared." }
    }

    fun start(tutorial: Tutorial) {
        if (tutorial.id == currentTutorial?.id) {
            logger.i { "tutorial $tutorial is already started" }
            return
        }

        currentTutorial = tutorial
        currentStep = tutorial.steps.firstOrNull()
        currentStep?.highlight?.let {
            revealer.tryReveal(it)
        }
    }
}