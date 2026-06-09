package com.rorokaiiworks.goodidlegame.core.traits

import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.RandomSource
import com.rorokaiiworks.goodidlegame.core.ISoundPlayer
import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.combat.CombatResult
import com.rorokaiiworks.goodidlegame.core.combat.ProcessedAttack
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.drops.DropTable
import com.rorokaiiworks.goodidlegame.core.enemies.Enemy
import com.rorokaiiworks.goodidlegame.core.events.IEvent
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemService
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.skills.SkillType
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.events.EventBus
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import com.rorokaiiworks.goodidlegame.core.stats.ISourceName
import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType
import com.rorokaiiworks.goodidlegame.tr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import kotlin.collections.plus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data class TraitTemplate(
    override val id: String,
    val name: String,
    val baseBonus: List<StatModifier>,
    val perks: List<PerkContainer>
) : Template, ISourceName {
    override val sourceName: String get() = name
}

@Serializable
data class PerkContainer(
    val unlockByLevel: Int,
    val perks: List<Perk>
)

@Serializable
data class Perk(
    val id: String,
    val desc: String,
)



private fun inventoryDrivenPerk(
    perk: Perk,
    sourceName: ISourceName,
    statSuffix: String,
    highThreshold: Int,
    highBonus: Float,
    lowThreshold: Int,
    lowPenalty: Float
) = object : PerkLogic(perk, sourceName) {
    override fun beforeSkillAction(skillAction: SkillAction) {
        val alwaysItem = skillAction.getAlwaysDropItemId() ?: return
        val stockCount = playerInventory.inventory.findItem(alwaysItem)?.count ?: 0

        val bonusValue = when {
            stockCount > highThreshold -> highBonus
            stockCount < lowThreshold -> lowPenalty
            else -> null
        }

        bonusValue?.let { value ->
            addPerkEffect(
                modifiers = listOf(
                    StatModifier(
                        statId = "${skillAction.skillId}_$statSuffix",
                        value = value,
                        type = StatModifierType.Percent
                    )
                )
            )
        }
    }

    override fun afterSkillAction(skillAction: SkillAction) {
        player.effectManager.removeAllEffectsBySource(perk.id)
    }
}

private fun timeDrivenPerk(
    perk: Perk,
    sourceName: ISourceName,
    statId: String,
    activeBonus: Float,
    inactiveBonus: Float,
    isActiveHour: (Int) -> Boolean
) = object : PerkLogic(perk, sourceName) {

    override fun onInit() {
        perform()

        CoroutineScope(Dispatchers.Default).launch {
            timeProvider.minuteTicker.collect {
                perform()
            }
        }
    }

    private fun perform() {
        player.effectManager.removeAllEffectsBySource(perk.id)

        val hour = timeProvider.localDateTime().hour
        val value = if (isActiveHour(hour)) activeBonus else inactiveBonus
        addPerkEffect(
            modifiers = listOf(
                StatModifier(
                    statId = statId,
                    value = value,
                    type = StatModifierType.Percent
                )
            )
        )
    }
}

@OptIn(ExperimentalTime::class)
private fun focusedCraftPerk(
    perk: Perk,
    sourceName: ISourceName,
    statSuffix: String,
    minutesPerStack: Int = 10,
    stepBonus: Float = 0.05f,
    maxBonus: Float = 0.30f
) = object : PerkLogic(perk, sourceName) {
    private val secondsPerStack = minutesPerStack * 60f
    private val maxStacks = (maxBonus / stepBonus).toInt()

    private var focusedSkillId: String? = null
    private var focusStartedAt: Instant? = null
    private var focusSeconds: Float = 0f
    private var stacks: Int = 0

    override fun beforeSkillAction(skillAction: SkillAction) {
        if (!isCraftSkillAction(skillAction)) {
            resetFocus()
            return
        }

        if (focusedSkillId != skillAction.skillId) {
            resetFocus()
            focusedSkillId = skillAction.skillId
        }

        focusStartedAt = timeProvider.now()
        applyEffect()
    }

    override fun afterSkillAction(skillAction: SkillAction) {
        if (!isCraftSkillAction(skillAction)) {
            resetFocus()
            return
        }

        if (focusedSkillId != skillAction.skillId) {
            resetFocus()
            return
        }

        val startedAt = focusStartedAt ?: return
        val elapsedSeconds = (timeProvider.now() - startedAt).inWholeSeconds.toFloat()
        if (elapsedSeconds <= 0f) return

        if (stacks >= maxStacks) {
            focusSeconds = 0f
            return
        }

        focusSeconds += elapsedSeconds
        val gainedStacks = (focusSeconds / secondsPerStack).toInt()
        if (gainedStacks <= 0) return

        focusSeconds -= gainedStacks * secondsPerStack
        val newStacks = (stacks + gainedStacks).coerceAtMost(maxStacks)
        if (newStacks != stacks) {
            stacks = newStacks
            applyEffect()
        }
    }

    private fun isCraftSkillAction(skillAction: SkillAction): Boolean {
        val skill = playerSkills.skills[skillAction.skillId] ?: return false
        return skill.template.skillType == SkillType.Craft
    }

    private fun applyEffect() {
        player.effectManager.removeAllEffectsBySource(perk.id)

        val skillId = focusedSkillId ?: return
        val bonus = (stacks * stepBonus).coerceAtMost(maxBonus)
        if (bonus <= 0f) return

        addPerkEffect(
            modifiers = listOf(
                StatModifier(
                    statId = "${skillId}_$statSuffix",
                    value = bonus,
                    type = StatModifierType.Percent
                )
            )
        )
    }

    private fun resetFocus() {
        focusedSkillId = null
        focusStartedAt = null
        focusSeconds = 0f
        stacks = 0
        player.effectManager.removeAllEffectsBySource(perk.id)
    }
}

private fun gamblerPerk(
    perk: Perk,
    sourceName: ISourceName,
    totalChance: Float,
    zeroChance: Float,
    applyBasePenalty: Boolean,
    basePenalty: Float = -0.2f
) = object : PerkLogic(perk, sourceName) {

    override fun onInit() {
        if (!applyBasePenalty) return

        addPerkEffect(
            modifiers = listOf(
                StatModifier(
                    statId = StatIds.Skills.CraftYield,
                    value = basePenalty,
                    type = StatModifierType.Percent
                ),
                StatModifier(
                    statId = StatIds.Skills.GatherYield,
                    value = basePenalty,
                    type = StatModifierType.Percent
                ),
                StatModifier(
                    statId = StatIds.Skills.CombatYield,
                    value = basePenalty,
                    type = StatModifierType.Percent
                ),
            )
        )
    }

    override fun onSkillActionFinish(skillAction: SkillAction, yields: List<Item>): List<Item> {
        val value = perkRandom.nextFloat()
        if (value < totalChance) {
            if (value < zeroChance) {
                return emptyList()
            }

            return yields.map { item -> item.copy(count = item.count * 3) }
        }

        return yields
    }
}

private fun hailStreakPerk(
    perk: Perk,
    sourceName: ISourceName,
    stackBonus: Float = 0.03f,
    maxBonus: Float = 0.30f
) = object : PerkLogic(perk, sourceName) {
    private val maxStacks = (maxBonus / stackBonus).toInt()
    private var stacks = 0

    override fun onCombatAttackHit(processedAttack: ProcessedAttack) {
        if (processedAttack.defender == player) {
            if (stacks != 0) {
                stacks = 0
                applyStacks()
            }
        }
    }

    override fun onCombatEnemyDied(enemy: Enemy) {
        if (stacks >= maxStacks) return
        stacks += 1
        applyStacks()
    }

    override fun onCombatEnd(result: CombatResult) {
        if (result == CombatResult.Defeat) {
            stacks = 0
            applyStacks()
        }
    }

    override fun onDeactivate() {
        stacks = 0
        super.onDeactivate()
    }

    private fun applyStacks() {
        player.effectManager.removeAllEffectsBySource(perk.id)

        val bonus = (stacks * stackBonus).coerceAtMost(maxBonus)
        if (bonus <= 0f) return

        addPerkEffect(
            modifiers = listOf(
                StatModifier(
                    statId = StatIds.Actor.SlashDamage,
                    value = bonus,
                    type = StatModifierType.Percent
                ),
                StatModifier(
                    statId = StatIds.Actor.PunctureDamage,
                    value = bonus,
                    type = StatModifierType.Percent
                ),
                StatModifier(
                    statId = StatIds.Actor.ImpactDamage,
                    value = bonus,
                    type = StatModifierType.Percent
                ),
                StatModifier(
                    statId = StatIds.Actor.HitChanceBonus,
                    value = bonus,
                    type = StatModifierType.Percent
                ),
            )
        )
    }
}

private fun hailRetaliationPerk(
    perk: Perk,
    sourceName: ISourceName,
    damageBonus: Float = 0.20f,
    hitBonus: Float = 0.10f,
    maxHealthPenalty: Float = -0.15f
) = object : PerkLogic(perk, sourceName) {
    private val nextAttackSource = "${perk.id}_next_attack"
    private var pending = false
    private var active = false

    override fun onInit() {
        addPerkEffect(
            modifiers = listOf(
                StatModifier(
                    statId = StatIds.Actor.MaxHealth,
                    value = maxHealthPenalty,
                    type = StatModifierType.Percent
                )
            )
        )
    }

    override fun onCombatAttackHit(processedAttack: ProcessedAttack) {
        if (processedAttack.defender == player) {
            pending = true
        }

        if (processedAttack.attacker == player && active) {
            removeNextAttackBuff()
        }
    }

    override fun onCombatAttackMissed(
        attacker: IActor,
        defender: IActor
    ) {
        if (attacker == player && active) {
            removeNextAttackBuff()
        }
    }

    override fun onCombatAttackStart(
        attacker: IActor,
        defender: IActor
    ) {
        if (attacker != player || !pending || active) return
        pending = false
        active = true
        addPerkEffect(
            sourceOverride = nextAttackSource,
            modifiers = listOf(
                StatModifier(
                    statId = StatIds.Actor.SlashDamage,
                    value = damageBonus,
                    type = StatModifierType.Percent
                ),
                StatModifier(
                    statId = StatIds.Actor.PunctureDamage,
                    value = damageBonus,
                    type = StatModifierType.Percent
                ),
                StatModifier(
                    statId = StatIds.Actor.ImpactDamage,
                    value = damageBonus,
                    type = StatModifierType.Percent
                ),
                StatModifier(
                    statId = StatIds.Actor.HitChanceBonus,
                    value = hitBonus,
                    type = StatModifierType.Percent
                ),
            )
        )
    }

    override fun onDeactivate() {
        pending = false
        active = false
        player.effectManager.removeAllEffectsBySource(nextAttackSource)
        super.onDeactivate()
    }

    private fun removeNextAttackBuff() {
        active = false
        player.effectManager.removeAllEffectsBySource(nextAttackSource)
    }
}


val basePerkLogics: Map<String, (Perk, ISourceName) -> PerkLogic> = mapOf(
    "perk_survivalist_tier_1_1" to { perk, sourceName ->
        inventoryDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "speed",
            highThreshold = 200,
            highBonus = 0.20f,
            lowThreshold = 50,
            lowPenalty = -0.12f
        )
    },

    "perk_survivalist_tier_1_2" to { perk, sourceName ->
        inventoryDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "XpMultiplier",
            highThreshold = 300,
            highBonus = 0.20f,
            lowThreshold = 100,
            lowPenalty = -0.08f
        )
    },

    "perk_survivalist_tier_2_1" to { perk, sourceName ->
        inventoryDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "speed",
            highThreshold = 320,
            highBonus = 0.24f,
            lowThreshold = 80,
            lowPenalty = -0.08f
        )
    },

    "perk_survivalist_tier_2_2" to { perk, sourceName ->
        inventoryDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "XpMultiplier",
            highThreshold = 420,
            highBonus = 0.24f,
            lowThreshold = 140,
            lowPenalty = -0.06f
        )
    },

    "perk_survivalist_tier_3_1" to { perk, sourceName ->
        inventoryDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "speed",
            highThreshold = 500,
            highBonus = 0.30f,
            lowThreshold = 180,
            lowPenalty = -0.04f
        )
    },

    "perk_survivalist_tier_3_2" to { perk, sourceName ->
        inventoryDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "XpMultiplier",
            highThreshold = 650,
            highBonus = 0.30f,
            lowThreshold = 220,
            lowPenalty = -0.04f
        )
    },

    "perk_zgen_tier_1_1" to { perk, sourceName ->
        timeDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statId = StatIds.Skills.GatherSpeed,
            activeBonus = 0.18f,
            inactiveBonus = -0.12f,
            isActiveHour = { hour -> hour !in 6..<22 }
        )
    },


    "perk_zgen_tier_1_2" to { perk, sourceName ->
        timeDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statId = StatIds.Skills.CraftSpeed,
            activeBonus = 0.18f,
            inactiveBonus = -0.12f,
            isActiveHour = { hour -> hour !in 6..<22 }
        )
    },

    "perk_zgen_tier_2_1" to { perk, sourceName ->
        timeDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statId = StatIds.Skills.GatherSpeed,
            activeBonus = 0.24f,
            inactiveBonus = -0.10f,
            isActiveHour = { hour -> hour !in 6..<22 }
        )
    },

    "perk_zgen_tier_2_2" to { perk, sourceName ->
        timeDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statId = StatIds.Skills.CraftSpeed,
            activeBonus = 0.24f,
            inactiveBonus = -0.10f,
            isActiveHour = { hour -> hour !in 6..<22 }
        )
    },

    "perk_zgen_tier_3_1" to { perk, sourceName ->
        timeDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statId = StatIds.Skills.GatherSpeed,
            activeBonus = 0.28f,
            inactiveBonus = -0.08f,
            isActiveHour = { hour -> hour !in 6..<22 }
        )
    },

    "perk_zgen_tier_3_2" to { perk, sourceName ->
        timeDrivenPerk(
            perk = perk,
            sourceName = sourceName,
            statId = StatIds.Skills.CraftSpeed,
            activeBonus = 0.28f,
            inactiveBonus = -0.08f,
            isActiveHour = { hour -> hour !in 6..<22 }
        )
    },

    "perk_gambler_tier_1_1" to { perk, sourceName ->
        gamblerPerk(
            perk = perk,
            sourceName = sourceName,
            totalChance = 0.26f,
            zeroChance = 0.13f,
            applyBasePenalty = true,
            basePenalty = -0.18f
        )
    },

    "perk_gambler_tier_1_2" to { perk, sourceName ->
        gamblerPerk(
            perk = perk,
            sourceName = sourceName,
            totalChance = 0.10f,
            zeroChance = 0.04f,
            applyBasePenalty = false
        )
    },

    "perk_gambler_tier_2_1" to { perk, sourceName ->
        gamblerPerk(
            perk = perk,
            sourceName = sourceName,
            totalChance = 0.30f,
            zeroChance = 0.10f,
            applyBasePenalty = true,
            basePenalty = -0.12f
        )
    },

    "perk_gambler_tier_2_2" to { perk, sourceName ->
        gamblerPerk(
            perk = perk,
            sourceName = sourceName,
            totalChance = 0.14f,
            zeroChance = 0.05f,
            applyBasePenalty = false
        )
    },

    "perk_gambler_tier_3_1" to { perk, sourceName ->
        gamblerPerk(
            perk = perk,
            sourceName = sourceName,
            totalChance = 0.34f,
            zeroChance = 0.08f,
            applyBasePenalty = true,
            basePenalty = -0.10f
        )
    },

    "perk_gambler_tier_3_2" to { perk, sourceName ->
        gamblerPerk(
            perk = perk,
            sourceName = sourceName,
            totalChance = 0.12f,
            zeroChance = 0.02f,
            applyBasePenalty = false
        )
    },

    "perk_tomato_tier_1_1" to { perk, sourceName ->
        focusedCraftPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "lootMultiplier"
        )
    },

    "perk_tomato_tier_1_2" to { perk, sourceName ->
        focusedCraftPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "XpMultiplier"
        )
    },

    "perk_tomato_tier_2_1" to { perk, sourceName ->
        focusedCraftPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "lootMultiplier",
            minutesPerStack = 8,
            stepBonus = 0.04f,
            maxBonus = 0.36f
        )
    },

    "perk_tomato_tier_2_2" to { perk, sourceName ->
        focusedCraftPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "XpMultiplier",
            minutesPerStack = 8,
            stepBonus = 0.04f,
            maxBonus = 0.36f
        )
    },

    "perk_tomato_tier_3_1" to { perk, sourceName ->
        focusedCraftPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "lootMultiplier",
            minutesPerStack = 6,
            stepBonus = 0.04f,
            maxBonus = 0.48f
        )
    },

    "perk_tomato_tier_3_2" to { perk, sourceName ->
        focusedCraftPerk(
            perk = perk,
            sourceName = sourceName,
            statSuffix = "XpMultiplier",
            minutesPerStack = 6,
            stepBonus = 0.04f,
            maxBonus = 0.48f
        )
    },

    "perk_hail_tier_1_1" to { perk, sourceName ->
        hailStreakPerk(
            perk = perk,
            sourceName = sourceName,
            stackBonus = 0.025f,
            maxBonus = 0.25f
        )
    },

    "perk_hail_tier_1_2" to { perk, sourceName ->
        hailRetaliationPerk(
            perk = perk,
            sourceName = sourceName,
            damageBonus = 0.18f,
            hitBonus = 0.08f,
            maxHealthPenalty = -0.12f
        )
    },

    "perk_hail_tier_2_1" to { perk, sourceName ->
        hailStreakPerk(
            perk = perk,
            sourceName = sourceName,
            stackBonus = 0.03f,
            maxBonus = 0.36f
        )
    },

    "perk_hail_tier_2_2" to { perk, sourceName ->
        hailRetaliationPerk(
            perk = perk,
            sourceName = sourceName,
            damageBonus = 0.24f,
            hitBonus = 0.12f,
            maxHealthPenalty = -0.12f
        )
    },

    "perk_hail_tier_3_1" to { perk, sourceName ->
        hailStreakPerk(
            perk = perk,
            sourceName = sourceName,
            stackBonus = 0.035f,
            maxBonus = 0.45f
        )
    },

    "perk_hail_tier_3_2" to { perk, sourceName ->
        hailRetaliationPerk(
            perk = perk,
            sourceName = sourceName,
            damageBonus = 0.30f,
            hitBonus = 0.15f,
            maxHealthPenalty = -0.10f
        )
    }
)

val allPerkLogics: Map<String, (Perk, ISourceName) -> PerkLogic> =
    basePerkLogics + equipmentPerkLogics

abstract class PerkLogic(val perk: Perk, val sourceName: ISourceName) : KoinComponent {
    val player: Player by inject()
    val playerInventory: PlayerInventory by inject()
    val playerSkills: PlayerSkills by inject()
    private val traitTemplates: DataTable<TraitTemplate> by inject(named<TraitTemplate>())
    val itemService: ItemService by inject()
    val eventBus: EventBus by inject()
    val soundPlayer: ISoundPlayer by inject()
    val timeProvider: ITimeProvider by inject()
    val perkRandom: RandomSource by inject { parametersOf(RandomSource.TAG_PERK) }
    private var isActive: Boolean = true
    private val i18n: I18n by inject()

    private val traitName: String by lazy {
        traitTemplates
            .all()
            .firstOrNull { trait ->
                trait.perks.any { container ->
                    container.perks.any { p -> p.id == perk.id }
                }
            }
            ?.name ?: perk.id
    }

    @OptIn(ExperimentalUuidApi::class)
    protected fun addPerkEffect(
        modifiers: List<StatModifier>,
        sourceOverride: String? = null
    ) {
        if (!isActive) return
        eventBus.tryEmit(
            IEvent.ToastMessage(
                msg = i18n.tr("Trait triggered: {0}", traitName),
                iconId = player.id
            )
        )
        player.effectManager.addEffect(
            Effect(
                id = perk.id,
                source = sourceOverride ?: perk.id,
                sourceName = sourceName,
                modifiers = modifiers
            )
        )
    }

    open fun tick(delta: Float) {}

    open fun onInit() {}

    open fun onDeactivate() {
        player.effectManager.removeAllEffectsBySource(perk.id)
    }

    fun deactivate() {
        if (!isActive) return
        isActive = false
        onDeactivate()
    }

    open fun beforeSkillAction(skillAction: SkillAction) {}

    open fun afterSkillAction(skillAction: SkillAction) {}

    open fun onSkillActionFinish(skillAction: SkillAction, yields: List<Item>): List<Item> {
        return yields
    }

    open fun onCombatRewards(
        skillAction: SkillAction,
        dropTable: DropTable?,
        items: List<Item>
    ): List<Item> {
        return items
    }

    open fun modifySkillActionXp(skillAction: SkillAction, xp: Long): Long {
        return xp
    }

    open fun onCombatAttackStart(
        attacker: IActor,
        defender: IActor
    ) {
    }

    open fun onCombatAttackHit(processedAttack: ProcessedAttack) {}

    open fun onCombatAttackMissed(
        attacker: IActor,
        defender: IActor
    ) {
    }

    open fun onCombatEnemyDied(enemy: Enemy) {}

    open fun onCombatEnd(result: CombatResult) {}
}
