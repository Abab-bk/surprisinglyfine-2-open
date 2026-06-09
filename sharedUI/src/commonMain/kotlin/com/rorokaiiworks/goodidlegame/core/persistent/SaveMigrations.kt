package com.rorokaiiworks.goodidlegame.core.persistent

import com.rorokaiiworks.goodidlegame.core.community.AltarSaveData
import com.rorokaiiworks.goodidlegame.core.community.AltarSlotSaveData
import com.rorokaiiworks.goodidlegame.core.community.CommunitySaveData
import com.rorokaiiworks.goodidlegame.core.community.EnchantingTableSaveData
import com.rorokaiiworks.goodidlegame.core.community.SquareBuildingSaveData
import com.rorokaiiworks.goodidlegame.core.community.SquareSaveData
import com.rorokaiiworks.goodidlegame.core.items.ItemSaveData
import kotlinx.serialization.json.*

fun migrateGameSave(version: GameSaveVersion, data: JsonObject): JsonObject {
    var migratedData = data
    if (version < GameSaveVersion.V2_COMMUNITY_REWORK) {
        migratedData = migrateToV2(migratedData)
    }
    return migratedData
}

private val INFRA_TO_SQUARE_MAP = mapOf(
    "infrastructure_alchemy_lab" to "square_alchemy_lab",
    "infrastructure_anvil" to "square_anvil",
    "infrastructure_cooking_pot" to "square_cooking_pot",
    "infrastructure_furnace" to "square_furnace",
    "infrastructure_cartography_table" to "square_cartography"
)

private fun migrateToV2(data: JsonObject): JsonObject {
    val mutableData = data.toMutableMap()

    val newCommunitySave = try {
        val oldCommunitySave = data["communitySaveData"]?.jsonObject
        val oldInfrastructures = oldCommunitySave?.get("infrastructureSaveData")?.jsonArray?.map { it.jsonObject } ?: emptyList()

        val squareBuildings = oldInfrastructures
            .filter { it["id"]?.jsonPrimitive?.content != "infrastructure_altar" } // 排除祭坛
            .mapNotNull { infra ->
                val oldId = infra["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val newTemplateId = INFRA_TO_SQUARE_MAP[oldId] ?: oldId.replace("infrastructure_", "square_")

                SquareBuildingSaveData(
                    templateId = newTemplateId,
                    unlockedTierCount = 1,
                    activeTierCount = 0,
                    selectedModifiersPerTier = listOf(0)
                )
            }

        val oldAltar = oldInfrastructures.firstOrNull {
            it["type"]?.jsonPrimitive?.content?.contains("Altar") == true
        }

        val oldRelicsArray = oldAltar?.get("relics")?.jsonArray
        val oldSize = oldRelicsArray?.size ?: 0

        val targetSlotCount = if (oldSize > 1) 2 + (oldSize - 1) else 1
        val altarSlots = mutableListOf<AltarSlotSaveData>()

        oldRelicsArray?.forEach { element ->
            val itemObject = if (element is JsonObject && element.isNotEmpty()) element else null
            altarSlots.add(
                AltarSlotSaveData(
                    item = itemObject?.let { Json.decodeFromJsonElement<ItemSaveData>(it) }
                )
            )
        }

        while (altarSlots.size < targetSlotCount) {
            altarSlots.add(AltarSlotSaveData(item = null))
        }

        CommunitySaveData(
            enchantingTable = EnchantingTableSaveData(),
            square = SquareSaveData(
                buildings = squareBuildings,
            ),
            altar = AltarSaveData(
                slots = if (oldRelicsArray == null) AltarSaveData.DEFAULT_SLOTS else altarSlots
            )
        ).also {
            println("Migrate to V2 success: $it")
            println("Migrate to V2 success, before: ${oldRelicsArray.toString()}")
        }
    } catch (e: Exception) {
        println("Migration to V2 failed: ${e.message}")
        CommunitySaveData()
    }

    mutableData["communitySaveData"] = Json.encodeToJsonElement(newCommunitySave)
    mutableData["version"] = Json.encodeToJsonElement(GameSaveVersion.V2_COMMUNITY_REWORK)

    return JsonObject(mutableData)
}