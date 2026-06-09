package com.rorokaiiworks.goodidlegame.core.talents

import com.rorokaiiworks.goodidlegame.core.Resource
import com.rorokaiiworks.goodidlegame.core.data.DataTable
import com.rorokaiiworks.goodidlegame.core.items.PlayerInventory
import com.rorokaiiworks.goodidlegame.core.persistent.GameSave
import com.rorokaiiworks.goodidlegame.core.persistent.IPersistable
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class TalentTree : KoinComponent, IPersistable {
    private val talentTemplates: DataTable<TalentTemplate> by inject(named<TalentTemplate>())
    private val playerInventory: PlayerInventory by inject()

    val talentMap: Map<String, Talent> = talentTemplates.all()
        .associate { it.id to Talent(it) }

    val allTalents get() = talentMap.values.toList()

    init {
        levelUp("root_talent")
    }

    fun canLevelUp(talent: Talent): Boolean {
        if (talent.locked) return false
        if (talent.isMaxLevel) return false

        val cost = talent.nextLevelCost ?: return false
        return playerInventory.stars >= cost
    }

    fun levelUp(talentId: String): Resource<Unit> {
        val talent = talentMap[talentId] ?: return Resource.Error(404, "Talent not found")

        if (!canLevelUp(talent)) {
            return Resource.Error(NOT_ENOUGH_STARS_ERROR, "Cannot level up (Locked or not enough stars)")
        }

        val cost = talent.nextLevelCost!!
        playerInventory.spendStars(cost)

        val result = talent.levelUp()

        if (result is Resource.Success) {
            unlockConnections(talent)
        }

        return result
    }

    private fun unlockConnections(talent: Talent) {
        talent.template.connections.forEach { connectionId ->
            talentMap[connectionId]?.let { child ->
                if (child.locked) {
                    child.locked = false
                }
            }
        }
    }

    override fun doSave(gameSave: GameSave, currentMills: Long) {
        gameSave.talentTreeSaveData = TalentTreeSaveData(
            talentMap = allTalents
                .filter { !it.locked }
                .associateBy({ it.template.id }, { it.level })
        )
    }

    override fun doLoad(gameSave: GameSave, currentMills: Long) {
        gameSave.talentTreeSaveData?.talentMap?.forEach { (talentId, level) ->
            talentMap[talentId]?.level = level
            talentMap[talentId]?.locked = false
            unlockConnections(talentMap[talentId]!!)
            talentMap[talentId]?.applyEffects()
        }
    }

    companion object {
        private const val NOT_ENOUGH_STARS_ERROR = 1002
    }
}


@Serializable
data class TalentTreeSaveData(
    val talentMap: Map<String, Int> = emptyMap()
)