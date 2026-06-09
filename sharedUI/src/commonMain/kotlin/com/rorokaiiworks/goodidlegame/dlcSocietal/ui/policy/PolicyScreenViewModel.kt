package com.rorokaiiworks.goodidlegame.dlcSocietal.ui.policy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.ItemTemplate
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.cities.CityInventory
import com.rorokaiiworks.goodidlegame.dlcSocietal.game.policies.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

enum class PolicyScreenPage(val title: String, val index: Int) {
    Loadout("Loadout", 0),
    Shop("Shop", 1),
}

class PolicyScreenViewModel : ViewModel(), KoinComponent {
    val policySystem: PolicySystem by inject()
    val cityInventory: CityInventory by inject()
    val itemTemplates: DataTable<ItemTemplate> by inject(named<ItemTemplate>())
    val playerInventory: PlayerInventory by inject()
    val i18n: I18n by inject()

    private var lastSyncedLoadout: Map<String, String?> = emptyMap()

    var draftBySlotId by mutableStateOf(currentLoadout())
        private set

    var lastApplyResult by mutableStateOf<PolicyApplyResult?>(null)
        private set

    var currentPage by mutableStateOf(PolicyScreenPage.Loadout)

    init {
        // `PolicySystem` is hydrated from save outside this VM's lifecycle. Keep the draft in sync
        // when the underlying loadout changes, but never overwrite user edits (pending changes).
        lastSyncedLoadout = draftBySlotId

        viewModelScope.launch {
            snapshotFlow { currentLoadout() }
                .distinctUntilChanged()
                .collect { nextLoadout ->
                    if (draftBySlotId == lastSyncedLoadout) {
                        draftBySlotId = nextLoadout
                        lastApplyResult = null
                    }
                    lastSyncedLoadout = nextLoadout
                }
        }
    }

    private fun currentLoadout(): Map<String, String?> =
        policySystem.slots.associate { it.id to it.equippedPolicyId }

    fun unlockedCards(): List<PolicyCard> =
        policySystem.cards.filter { policySystem.isUnlockedPolicy(it.id) && !policySystem.isEquipped(it) }

    fun assignDraft(slotId: String, cardId: String) {
        val slot = policySystem.slots.firstOrNull { it.id == slotId } ?: return
        val card = unlockedCards().firstOrNull { it.id == cardId } ?: return
        if (!slotAcceptsCard(slot, card)) return

        val next = draftBySlotId.toMutableMap()
        // Keep uniqueness: moving a card to this slot removes it from previous slot.
        next.keys.forEach { key ->
            if (key != slotId && next[key] == cardId) {
                next[key] = null
            }
        }
        next[slotId] = cardId
        draftBySlotId = next
        lastApplyResult = null
    }

    fun unequipDraft(slotId: String) {
        draftBySlotId = draftBySlotId.toMutableMap().also { it[slotId] = null }
        lastApplyResult = null
    }

    fun resetDraft() {
        draftBySlotId = currentLoadout()
        lastApplyResult = null
    }

    fun confirmDraft() {
        val result = policySystem.applyPolicyLoadout(draftBySlotId)
        lastApplyResult = result
        if (result == PolicyApplyResult.Success || result == PolicyApplyResult.NoChanges) {
            draftBySlotId = currentLoadout()
        }
    }

    fun hasPendingChanges(): Boolean = draftBySlotId != currentLoadout()

    fun draftCard(slotId: String): PolicyCard? =
        draftBySlotId[slotId]?.let { cardId -> policySystem.cards.firstOrNull { it.id == cardId } }

    fun equippedSlotForDraft(cardId: String): PolicySlot? {
        val slotId = draftBySlotId.entries.firstOrNull { it.value == cardId }?.key ?: return null
        return policySystem.slots.firstOrNull { it.id == slotId }
    }

    fun slotAcceptsCard(slot: PolicySlot, card: PolicyCard): Boolean =
        slot.type == PolicySlotType.Wildcard || slot.type == card.slotType
}
