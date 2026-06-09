@file:OptIn(ExperimentalTime::class)

package com.rorokaiiworks.goodidlegame.core.traits

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.ITimeProvider
import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.drops.DropTable
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import com.rorokaiiworks.goodidlegame.core.players.Player
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import com.rorokaiiworks.goodidlegame.core.stats.ISourceName
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class TraitSystem : KoinComponent, IPersistable {
    private val traitTemplates: DataTable<TraitTemplate> by inject(named<TraitTemplate>())
    private val player: Player by inject()
    private val playerSkills: PlayerSkills by inject()
    private val playerInventory: PlayerInventory by inject()
    private val timeProvider: ITimeProvider by inject()

    var currentTraitId: String? by mutableStateOf(null)
        private set

    var lastSwitchAt: Instant? by mutableStateOf(null)
        private set

    private var lastTotalSkillLevel: Int = 0

    private val activePerkLogics = mutableMapOf<String, PerkLogic>()
    private val externalPerkLogics = mutableMapOf<String, PerkLogic>()
    private val selectedPerksByTrait = mutableStateMapOf<String, Map<Int, String>>()

    val activeTrait: TraitTemplate?
        get() = currentTraitId?.let { traitTemplates.findOrNull(it) }

    fun totalSkillLevel(): Int = playerSkills.skills.values.sumOf { it.level }

    fun getSwitchStatus(targetTraitId: String): TraitSwitchStatus {
        val isCurrent = targetTraitId == currentTraitId
        if (isCurrent) {
            return TraitSwitchStatus(
                canSwitch = false,
                isCurrent = true,
                requiresItem = false,
                hasItem = true,
                cooldownRemaining = Duration.ZERO
            )
        }

        if (currentTraitId == null) {
            return TraitSwitchStatus(
                canSwitch = true,
                isCurrent = false,
                requiresItem = false,
                hasItem = true,
                cooldownRemaining = Duration.ZERO
            )
        }

        val remainingCooldown = getCooldownRemaining()
        val requiresItem = true
        val hasItem = playerInventory.inventory.canConsume(listOf(ItemEntry(TRAIT_SWITCH_ITEM_ID, 1)))
        val canSwitch = remainingCooldown <= Duration.ZERO && hasItem

        return TraitSwitchStatus(
            canSwitch = canSwitch,
            isCurrent = false,
            requiresItem = requiresItem,
            hasItem = hasItem,
            cooldownRemaining = remainingCooldown
        )
    }

    fun getSelectedPerkId(traitId: String, containerIndex: Int): String? {
        return selectedPerksByTrait[traitId]?.get(containerIndex)
    }

    fun selectPerk(traitId: String, containerIndex: Int, perkId: String) {
        val trait = traitTemplates.find(traitId)
        if (traitId != currentTraitId) return

        val containers = trait.perks
        if (containerIndex !in containers.indices) return

        val container = containers[containerIndex]
        if (container.perks.none { it.id == perkId }) return

        val totalLevel = totalSkillLevel()
        if (totalLevel < container.unlockByLevel) return

        val existing = getSelectedPerkId(traitId, containerIndex)
        if (existing == perkId) return

        val required = listOf(ItemEntry(TRAIT_SWITCH_ITEM_ID, 1))
        if (!playerInventory.inventory.canConsume(required)) return

        playerInventory.inventory.removeItems(required)
        val traitSelections = selectedPerksByTrait[traitId] ?: emptyMap()
        selectedPerksByTrait[traitId] = traitSelections + (containerIndex to perkId)

        if (traitId == currentTraitId) {
            updateActivePerks()
        }
    }

    fun switchTrait(traitId: String) {
        if (traitId == currentTraitId) return

        if (currentTraitId == null) {
            applyTrait(traitId)
            return
        }

        val remainingCooldown = getCooldownRemaining()
        if (remainingCooldown > Duration.ZERO) return

        val required = listOf(ItemEntry(TRAIT_SWITCH_ITEM_ID, 1))
        if (!playerInventory.inventory.canConsume(required)) return

        playerInventory.inventory.removeItems(required)
        applyTrait(traitId)
    }

    fun tick(delta: Float) {
        val totalLevel = totalSkillLevel()
        if (totalLevel != lastTotalSkillLevel) {
            lastTotalSkillLevel = totalLevel
            updateActivePerks()
        }

        activePerkLogics.values.forEach { it.tick(delta) }
        externalPerkLogics.values.forEach { it.tick(delta) }
    }

    fun beforeSkillAction(skillAction: SkillAction) {
        activePerkLogics.values.forEach { it.beforeSkillAction(skillAction) }
        externalPerkLogics.values.forEach { it.beforeSkillAction(skillAction) }
    }

    fun afterSkillAction(skillAction: SkillAction) {
        activePerkLogics.values.forEach { it.afterSkillAction(skillAction) }
        externalPerkLogics.values.forEach { it.afterSkillAction(skillAction) }
    }

    fun onSkillActionFinish(skillAction: SkillAction, yields: List<Item>): List<Item> {
        var result = yields
        activePerkLogics.values.forEach { perkLogic ->
            result = perkLogic.onSkillActionFinish(skillAction, result)
        }
        externalPerkLogics.values.forEach { perkLogic ->
            result = perkLogic.onSkillActionFinish(skillAction, result)
        }
        return result
    }

    fun onCombatRewards(skillAction: SkillAction, dropTable: DropTable?, items: List<Item>): List<Item> {
        var result = items
        activePerkLogics.values.forEach { perkLogic ->
            result = perkLogic.onCombatRewards(skillAction, dropTable, result)
        }
        externalPerkLogics.values.forEach { perkLogic ->
            result = perkLogic.onCombatRewards(skillAction, dropTable, result)
        }
        return result
    }

    fun modifySkillActionXp(skillAction: SkillAction, xp: Long): Long {
        var result = xp
        activePerkLogics.values.forEach { perkLogic ->
            result = perkLogic.modifySkillActionXp(skillAction, result)
        }
        externalPerkLogics.values.forEach { perkLogic ->
            result = perkLogic.modifySkillActionXp(skillAction, result)
        }
        return result
    }

    fun onCombatAttackStart(
        attacker: com.rorokaiiworks.goodidlegame.core.actors.IActor,
        defender: com.rorokaiiworks.goodidlegame.core.actors.IActor
    ) {
        activePerkLogics.values.forEach { it.onCombatAttackStart(attacker, defender) }
        externalPerkLogics.values.forEach { it.onCombatAttackStart(attacker, defender) }
    }

    fun onCombatAttackHit(processedAttack: com.rorokaiiworks.goodidlegame.core.combat.ProcessedAttack) {
        activePerkLogics.values.forEach { it.onCombatAttackHit(processedAttack) }
        externalPerkLogics.values.forEach { it.onCombatAttackHit(processedAttack) }
    }

    fun onCombatAttackMissed(
        attacker: com.rorokaiiworks.goodidlegame.core.actors.IActor,
        defender: com.rorokaiiworks.goodidlegame.core.actors.IActor
    ) {
        activePerkLogics.values.forEach { it.onCombatAttackMissed(attacker, defender) }
        externalPerkLogics.values.forEach { it.onCombatAttackMissed(attacker, defender) }
    }

    fun onCombatEnemyDied(enemy: com.rorokaiiworks.goodidlegame.core.enemies.Enemy) {
        activePerkLogics.values.forEach { it.onCombatEnemyDied(enemy) }
        externalPerkLogics.values.forEach { it.onCombatEnemyDied(enemy) }
    }

    fun onCombatEnd(result: com.rorokaiiworks.goodidlegame.core.combat.CombatResult) {
        activePerkLogics.values.forEach { it.onCombatEnd(result) }
        externalPerkLogics.values.forEach { it.onCombatEnd(result) }
    }

    fun activateExternalPerk(perk: Perk, sourceName: ISourceName) {
        if (externalPerkLogics.containsKey(perk.id)) return
        val perkLogicFactory = allPerkLogics[perk.id] ?: return
        val logic = perkLogicFactory(perk, sourceName)
        externalPerkLogics[perk.id] = logic
        logic.onInit()
    }

    fun deactivateExternalPerk(perkId: String) {
        externalPerkLogics.remove(perkId)?.deactivate()
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.traitSystemSaveData = TraitSystemSaveData(
            currentTraitId = currentTraitId,
            lastSwitchAt = lastSwitchAt,
            selectedPerksByTrait = selectedPerksByTrait.toMap()
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        val saveData = gameSave.traitSystemSaveData ?: return
        currentTraitId = saveData.currentTraitId
        lastSwitchAt = saveData.lastSwitchAt
        selectedPerksByTrait.clear()
        selectedPerksByTrait.putAll(saveData.selectedPerksByTrait)
        lastTotalSkillLevel = totalSkillLevel()

        refreshActiveTrait()
    }

    private fun refreshActiveTrait() {
        clearTraitEffects()
        activeTrait?.let { trait ->
            addBaseBonus(trait)
            updateActivePerks()
        }
    }

    private fun applyTrait(traitId: String) {
        clearTraitEffects()
        currentTraitId = traitId
        lastSwitchAt = timeProvider.now()
        lastTotalSkillLevel = totalSkillLevel()

        activeTrait?.let { trait ->
            addBaseBonus(trait)
            updateActivePerks()
        }
    }

    private fun addBaseBonus(trait: TraitTemplate) {
        if (trait.baseBonus.isEmpty()) return

        player.effectManager.removeAllEffectsBySource("${trait.id}_base")
        player.effectManager.addEffect(
            Effect(
                id = "${trait.id}_base",
                source = "${trait.id}_base",
                sourceName = trait,
                modifiers = trait.baseBonus.map { modifier ->
                    StatModifier(
                        statId = modifier.statId,
                        value = modifier.value,
                        type = modifier.type,
                        channel = modifier.channel,
                    )
                }
            )
        )
    }

    private fun clearTraitEffects() {
        activePerkLogics.values.forEach { it.deactivate() }
        activePerkLogics.clear()

        currentTraitId?.let { traitId ->
            player.effectManager.removeAllEffectsBySource("${traitId}_base")
        }
    }

    private fun updateActivePerks() {
        val trait = activeTrait ?: return
        val totalLevel = totalSkillLevel()

        val desiredPerks = trait.perks
            .mapIndexedNotNull { index, container ->
                if (totalLevel < container.unlockByLevel) return@mapIndexedNotNull null
                val selectedId = getSelectedPerkId(trait.id, index) ?: return@mapIndexedNotNull null
                container.perks.firstOrNull { it.id == selectedId }
            }

        val desiredIds = desiredPerks.map { it.id }.toSet()

        val toRemove = activePerkLogics.keys.filter { it !in desiredIds }
        toRemove.forEach { perkId ->
            activePerkLogics.remove(perkId)?.deactivate()
        }

        desiredPerks.forEach { perk ->
            if (activePerkLogics.containsKey(perk.id)) return@forEach
            val perkLogicFactory = allPerkLogics[perk.id] ?: return@forEach
            val logic = perkLogicFactory(perk, trait)
            activePerkLogics[perk.id] = logic
            logic.onInit()
        }
    }

    fun getCooldownRemaining(): Duration {
        val lastSwitch = lastSwitchAt ?: return Duration.ZERO
        val elapsed = timeProvider.now() - lastSwitch
        val remaining = TRAIT_SWITCH_COOLDOWN - elapsed
        return if (remaining.isNegative()) Duration.ZERO else remaining
    }

    companion object {
        const val TRAIT_SWITCH_ITEM_ID = "formater"
        val TRAIT_SWITCH_COOLDOWN: Duration = 8.hours
    }
}

data class TraitSwitchStatus(
    val canSwitch: Boolean,
    val isCurrent: Boolean,
    val requiresItem: Boolean,
    val hasItem: Boolean,
    val cooldownRemaining: Duration
)

@Serializable
data class TraitSystemSaveData(
    val currentTraitId: String? = null,
    val lastSwitchAt: Instant? = null,
    val selectedPerksByTrait: Map<String, Map<Int, String>> = emptyMap()
)
