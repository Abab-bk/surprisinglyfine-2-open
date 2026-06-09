package com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.data.Template
import com.rorokaiiworks.goodidlegame.core.items.ItemEntry
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.skills.PlayerSkills
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.Building
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingTemplate
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.buildings.BuildingType
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import com.rorokaiiworks.goodidlegame.tr
import com.rorokaiiworks.goodidlegame.ui.i18nWrapper
import kotlinx.serialization.Serializable
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@Serializable
enum class PolicySlotType(val label: String) {
    Economic(i18nWrapper("Economic")),
    Labor(i18nWrapper("Labor")),
    Social(i18nWrapper("Social")),
    Wildcard(i18nWrapper("Wildcard")),
}

data class PolicySlot(
    val id: String,
    val type: PolicySlotType,
    val title: String,
    val equippedPolicyId: String? = null,
)

@Serializable
data class PolicyCard(
    override val id: String,
    val name: String,
    val slotType: PolicySlotType,
    val mechanismId: String,
    val desc: String,
    val descArgs: List<String>,
    val unlockConditions: List<PolicyUnlockCondition> = emptyList(),
    val unlockCosts: List<ItemEntry> = emptyList(),
) : Template {
    fun displayText(i18n: I18n): String = i18n.tr(desc, *descArgs.toTypedArray())

}

enum class PolicyApplyResult(val label: String) {
    Success("Success"),
    NoChanges("No Changes"),
    SlotNotFound("Slot Not Found"),
    CardNotFound("Card Not Found"),
    SlotTypeMismatch("Slot Type Mismatch"),
    CardAlreadyEquipped("Card Already Equipped"),
    NotEnoughIsleBucks("Not Enough Isle Bucks"),
}


@Serializable
data class PolicySaveData(
    val slotEquippedIds: Map<String, String?> = emptyMap(),
    val ownedPolicyIds: List<String> = emptyList(),
)

private val allMechanisms = listOf<PolicyMechanism>(
    object : PolicyMechanism {
        override val mechanismId: String = "policy_investment"
        override fun getModifier(type: PolicyModifierType, template: BuildingTemplate?): Float {
            return if (type == PolicyModifierType.BuildBuildingCosts) -0.25f else 0f
        }
    },
    object : PolicyMechanism {
        override val mechanismId: String = "policy_economicform"
        override fun getModifier(type: PolicyModifierType, template: BuildingTemplate?): Float {
            return if (type == PolicyModifierType.MaintenanceCosts) -0.15f else 0f
        }
    },
    object : PolicyMechanism {
        override val mechanismId: String = "policy_tariff"
        override fun getModifier(type: PolicyModifierType, template: BuildingTemplate?): Float {
            return if (type == PolicyModifierType.SaturationDecaySpeed) 0.50f else 0f
        }
    },
    object : PolicyMechanism {
        override val mechanismId: String = "policy_automation"
        override fun getModifier(type: PolicyModifierType, template: BuildingTemplate?): Float {
            return when (type) {
                PolicyModifierType.ProductSpeed -> 0.15f
                PolicyModifierType.WorkforceDemand -> -0.20f
                else -> 0f
            }
        }
    },
    object : PolicyMechanism {
        override val mechanismId: String = "policy_standardization"
        override fun getModifier(type: PolicyModifierType, template: BuildingTemplate?): Float {
            return if (type == PolicyModifierType.ProductSpeed) 0.10f else 0f
        }
    },
    object : PolicyMechanism {
        override val mechanismId: String = "policy_forced_overtime"
        override fun getModifier(type: PolicyModifierType, template: BuildingTemplate?): Float {
            return when (type) {
                PolicyModifierType.ProductSpeed -> 0.30f
                PolicyModifierType.MaintenanceCosts -> 0.25f
                else -> 0f
            }
        }
    },
    object : PolicyMechanism {
        override val mechanismId: String = "policy_welfare_state"
        override fun getModifier(type: PolicyModifierType, template: BuildingTemplate?): Float {
            if (template?.buildingType == BuildingType.Residences && type == PolicyModifierType.MaintenanceCosts) {
                return -0.30f
            }
            return 0f
        }
    },
    object : PolicyMechanism {
        override val mechanismId: String = "policy_public_housing"
        override fun getModifier(type: PolicyModifierType, template: BuildingTemplate?): Float {
            if (template?.buildingType == BuildingType.Residences && type == PolicyModifierType.HousingCapacity) {
                return 0.25f
            }
            return 0f
        }
    }
)


class PolicySystem(
    val cardsTable: DataTable<PolicyCard>,
) : KoinComponent {
    private val logger: Logger by inject { parametersOf("PolicySystem") }
    private val cityInventory: CityInventory by inject()
    private val playerSkills: PlayerSkills by inject()
    private val playerInventory: PlayerInventory by inject()

    val cards: List<PolicyCard> = cardsTable.all()

    var slots: List<PolicySlot> by mutableStateOf(defaultSlots())
        private set

    val ownedPolicyIds = mutableStateListOf<String>()
    
    private val mechanismRegistry: PolicyMechanismRegistry = PolicyMechanismRegistry(
        mechanisms = allMechanisms,
    )

    private val activeCards = mutableListOf<PolicyCard>()

    fun getPolicyChangeUnlockCost(): Long = POLICY_CHANGE_UNLOCK_COST_ISLE_BUCKS

    fun isUnlockedPolicy(policyId: String): Boolean = ownedPolicyIds.contains(policyId)

    fun isConditionMet(condition: PolicyUnlockCondition): Boolean = when (condition) {
        is PolicyUnlockCondition.SkillLevel -> {
            val currentLevel = playerSkills.skills[condition.skillId]?.level ?: 0
            currentLevel >= condition.level
        }
    }

    fun isPurchasable(card: PolicyCard): Boolean {
        if (isUnlockedPolicy(card.id)) return false
        if (!card.unlockConditions.all(::isConditionMet)) return false
        return playerInventory.inventory.canConsume(card.unlockCosts)
    }

    fun isEquipped(card: PolicyCard): Boolean {
        return slots.any { it.equippedPolicyId == card.id }
    }

    fun tryPurchase(policyId: String): Boolean {
        val card = cardsTable.find(policyId)
        if (!isPurchasable(card)) return false

        playerInventory.inventory.removeItems(card.unlockCosts)
        unlockPolicy(policyId)
        return true
    }

    fun unlockPolicy(policyId: String) {
        if (ownedPolicyIds.contains(policyId)) return
        if (cardsTable.findOrNull(policyId) == null) return
        ownedPolicyIds.add(policyId)
    }

    fun applyPolicyLoadout(
        slotToPolicy: Map<String, String?>,
    ): PolicyApplyResult {
        val nextSlots = mergeLoadoutIntoSlots(slotToPolicy) ?: return PolicyApplyResult.SlotNotFound
        val validation = validateLoadout(nextSlots)
        if (validation != PolicyApplyResult.Success) return validation

        if (nextSlots == slots) return PolicyApplyResult.NoChanges

        val cost = getPolicyChangeUnlockCost()
        if (cityInventory.isleBucks < cost) return PolicyApplyResult.NotEnoughIsleBucks

        cityInventory.spendIsleBucks(cost)
        applyNextSlots(nextSlots)
        logger.i { "Applied policy loadout. Cost=$cost" }
        return PolicyApplyResult.Success
    }

    private fun PolicySlot.accepts(cardType: PolicySlotType): Boolean {
        if (type == PolicySlotType.Wildcard) return true
        return type == cardType
    }

    private fun defaultSlots(): List<PolicySlot> =
        SLOT_CAPACITY_BY_TYPE.flatMap { (type, capacity) ->
            (1..capacity).map { index ->
                val lowerName = type.name.lowercase()
                PolicySlot(
                    id = "${lowerName}_$index",
                    type = type,
                    title = "${type.name} $index",
                )
            }
        }

    companion object {
        const val POLICY_CHANGE_UNLOCK_COST_ISLE_BUCKS = 500L

        private val SLOT_CAPACITY_BY_TYPE = mapOf(
            PolicySlotType.Economic to 1,
            PolicySlotType.Labor to 1,
            PolicySlotType.Social to 1,
            PolicySlotType.Wildcard to 1,
        )
    }

    private fun validateLoadout(loadout: List<PolicySlot>): PolicyApplyResult {
        val equippedCardIds = mutableSetOf<String>()

        loadout.forEach { slot ->
            val cardId = slot.equippedPolicyId ?: return@forEach
            val card = cardsTable.findOrNull(cardId) ?: return PolicyApplyResult.CardNotFound
            if (!isUnlockedPolicy(cardId)) return PolicyApplyResult.CardNotFound
            if (!slot.accepts(card.slotType)) return PolicyApplyResult.SlotTypeMismatch
            if (!equippedCardIds.add(cardId)) return PolicyApplyResult.CardAlreadyEquipped
        }

        return PolicyApplyResult.Success
    }

    private fun sanitizeSlots(loadout: List<PolicySlot>): List<PolicySlot> {
        val equippedCardIds = mutableSetOf<String>()

        return loadout.map { slot ->
            val cardId = slot.equippedPolicyId ?: return@map slot
            val card = cardsTable.findOrNull(cardId) ?: return@map slot.copy(equippedPolicyId = null)
            if (!isUnlockedPolicy(cardId)) return@map slot.copy(equippedPolicyId = null)
            if (!slot.accepts(card.slotType)) return@map slot.copy(equippedPolicyId = null)
            if (!equippedCardIds.add(cardId)) return@map slot.copy(equippedPolicyId = null)
            slot
        }
    }

    private fun mergeLoadoutIntoSlots(
        slotToPolicy: Map<String, String?>,
    ): List<PolicySlot>? {
        val knownSlotIds = slots.map { it.id }.toSet()
        if (slotToPolicy.keys.any { it !in knownSlotIds }) return null

        return slots.map { slot ->
            if (slotToPolicy.containsKey(slot.id)) {
                slot.copy(equippedPolicyId = slotToPolicy[slot.id])
            } else {
                slot
            }
        }
    }

    private fun applyNextSlots(nextSlots: List<PolicySlot>) {
        val previousCards = activeCards.toList()
        val nextCards = nextSlots.mapNotNull { slot ->
            slot.equippedPolicyId?.let(cardsTable::findOrNull)
        }

        val prevCounts = previousCards.groupingBy { it.mechanismId }.eachCount()
        val nextCounts = nextCards.groupingBy { it.mechanismId }.eachCount()
        val allMechanismIds = (prevCounts.keys + nextCounts.keys).toSet()

        allMechanismIds.forEach { mechanismId ->
            val prev = prevCounts[mechanismId] ?: 0
            val next = nextCounts[mechanismId] ?: 0
            val mechanism = mechanismRegistry.findOrNull(mechanismId) ?: return@forEach

            if (prev <= 0 && next > 0) mechanism.onActivated()
            if (prev > 0 && next <= 0) mechanism.onDeactivated()
        }

        slots = nextSlots
        activeCards.clear()
        activeCards.addAll(nextCards)
    }

    fun onBuildingTick(building: Building) {
        activeCards.forEach { card ->
            val mechanism = mechanismRegistry.findOrNull(card.mechanismId) ?: return@forEach
            mechanism.onBuildingTick(building = building)
        }
    }

    data class BuildingPeriodTransaction(
        val costs: List<ItemEntry>,
        val yields: List<ItemEntry>,
    )

    fun applyBuildingPeriodMechanisms(
        building: Building,
        baseCosts: List<ItemEntry>,
        baseYields: List<ItemEntry>,
    ): BuildingPeriodTransaction {
        if (activeCards.isEmpty()) return BuildingPeriodTransaction(costs = baseCosts, yields = baseYields)

        val costs = baseCosts.toMutableList()
        val yields = baseYields.toMutableList()

        activeCards.forEach { card ->
            val mechanism = mechanismRegistry.findOrNull(card.mechanismId) ?: return@forEach
            mechanism.onBuildingPeriod(building = building, costs = costs, yields = yields)
        }

        return BuildingPeriodTransaction(costs = costs, yields = yields)
    }

    fun getModifier(type: PolicyModifierType, template: BuildingTemplate?): Float {
        return activeCards.sumOf { card ->
            mechanismRegistry.findOrNull(card.mechanismId)?.getModifier(type, template)?.toDouble() ?: 0.0
        }.toFloat()
    }

    fun toSave(): PolicySaveData {
        val saveMap = slots.associate { it.id to it.equippedPolicyId }
        return PolicySaveData(
            slotEquippedIds = saveMap,
            ownedPolicyIds = ownedPolicyIds.toList(),
        )
    }

    fun fromSave(
        policySaveData: PolicySaveData,
    ) {
        val inferredOwnedFromEquipped = policySaveData.slotEquippedIds.values.filterNotNull()
        val nextOwned = (policySaveData.ownedPolicyIds + inferredOwnedFromEquipped)
            .distinct()
            .filter { cardsTable.findOrNull(it) != null }

        ownedPolicyIds.clear()
        ownedPolicyIds.addAll(nextOwned)

        val loadedSlots = slots.map { slot ->
            slot.copy(equippedPolicyId = policySaveData.slotEquippedIds[slot.id])
        }
        applyNextSlots(sanitizeSlots(loadedSlots))
    }
}
