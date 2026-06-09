package com.rorokaiiworks.goodidlegame.core.traits

import com.rorokaiiworks.goodidlegame.core.actors.IActor
import com.rorokaiiworks.goodidlegame.core.combat.DamageCalculator
import com.rorokaiiworks.goodidlegame.core.combat.ProcessedAttack
import com.rorokaiiworks.goodidlegame.core.drops.DropTable
import com.rorokaiiworks.goodidlegame.core.items.Item
import com.rorokaiiworks.goodidlegame.core.skills.SkillAction
import com.rorokaiiworks.goodidlegame.core.skills.SkillType
import com.rorokaiiworks.goodidlegame.core.stats.Effect
import com.rorokaiiworks.goodidlegame.core.stats.ISourceName
import com.rorokaiiworks.goodidlegame.core.stats.StatIds
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import com.rorokaiiworks.goodidlegame.core.stats.StatModifierType
import kotlin.math.floor
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private fun PerkLogic.addEffect(
    source: String,
    sourceName: ISourceName,
    modifiers: List<StatModifier>
) {
    player.effectManager.addEffect(
        Effect(
            id = perk.id,
            source = source,
            sourceName = sourceName,
            modifiers = modifiers
        )
    )
}

private fun PerkLogic.removeEffect(source: String) {
    player.effectManager.removeAllEffectsBySource(source)
}

private fun PerkLogic.addEnemyEffect(
    source: String,
    target: IActor,
    sourceName: ISourceName,
    modifiers: List<StatModifier>
) {
    target.effectManager.addEffect(
        Effect(
            id = perk.id,
            source = source,
            sourceName = sourceName,
            modifiers = modifiers
        )
    )
}

private fun PerkLogic.removeEnemyEffect(
    source: String,
    target: IActor
) {
    target.effectManager.removeAllEffectsBySource(source)
}

private fun rollRareBonusItems(
    perkLogic: PerkLogic,
    dropTable: DropTable,
    stacks: Int,
    bonusChancePerStack: Float
): List<Item> {
    if (stacks <= 0 || bonusChancePerStack <= 0f) return emptyList()
    val rareEntries = dropTable.entries.filter { !it.isAlways }
    if (rareEntries.isEmpty()) return emptyList()

    val items = mutableListOf<Item>()
    repeat(stacks) {
        if (perkLogic.perkRandom.nextFloat() < bonusChancePerStack) {
            val entry = rareEntries.random()
            val amount = kotlin.random.Random.nextInt(entry.min, entry.max + 1)
            repeat(amount) {
                items += perkLogic.itemService.createItem(entry.itemId)
            }
        }
    }
    return items
}

private fun applyRareMultiplier(
    perkLogic: PerkLogic,
    dropTable: DropTable,
    multiplier: Float
): List<Item> {
    if (multiplier <= 1f) return emptyList()
    val extraFactor = multiplier - 1f
    val items = mutableListOf<Item>()
    dropTable.entries
        .filter { !it.isAlways }
        .forEach { entry ->
            val extraChance = (entry.chance * extraFactor) / DropTable.TOTAL_WEIGHT
            if (perkLogic.perkRandom.nextFloat() < extraChance) {
                val amount = kotlin.random.Random.nextInt(entry.min, entry.max + 1)
                repeat(amount) {
                    items += perkLogic.itemService.createItem(entry.itemId)
                }
            }
        }
    return items
}

private fun majestyPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private val critSource = "${perk.id}_crit"
    private var pending = false
    private var active = false

    override fun onCombatAttackMissed(
        attacker: IActor,
        defender: IActor
    ) {
        if (defender == player && attacker != player) {
            pending = true
        }
        if (attacker == player && active) {
            removeEffect(critSource)
            active = false
        }
    }

    override fun onCombatAttackStart(
        attacker: IActor,
        defender: IActor
    ) {
        if (attacker != player || !pending || active) return
        pending = false
        active = true
        addEffect(
            source = critSource,
            sourceName = sourceName,
            modifiers = listOf(
                StatModifier(StatIds.Actor.DamageMultiplier, 1.0f, StatModifierType.Percent),
                StatModifier(StatIds.Actor.HitChanceBonus, 1.0f, StatModifierType.Flat),
            )
        )
    }

    override fun onCombatAttackHit(processedAttack: ProcessedAttack) {
        if (processedAttack.attacker == player && active) {
            removeEffect(critSource)
            active = false
        }
    }

    override fun onDeactivate() {
        pending = false
        active = false
        removeEffect(critSource)
        super.onDeactivate()
    }
}

private fun sincerelyPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    override fun onCombatAttackHit(processedAttack: ProcessedAttack) {
        if (processedAttack.attacker != player) return
        if (perkRandom.nextFloat() >= 0.03f) return

        val extra = ProcessedAttack(
            attacker = player,
            defender = processedAttack.defender,
            slashDamage = processedAttack.slashDamage,
            punctureDamage = processedAttack.punctureDamage,
            impactDamage = processedAttack.impactDamage,
            isOneHanded = processedAttack.isOneHanded,
            isTwoHanded = processedAttack.isTwoHanded,
            isRanged = processedAttack.isRanged,
        )
        processedAttack.defender.takeDamage(extra)
    }
}

private fun bravePerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private val effectSource = "${perk.id}_effect"

    override fun beforeSkillAction(skillAction: SkillAction) {
        if (skillAction !is SkillAction.CombatSkillAction) return
        removeEffect(effectSource)
        val roll = perkRandom.nextFloat()
        val modifiers = when {
            roll < 0.34f -> listOf(
                StatModifier(StatIds.Actor.DamageMultiplier, 0.5f, StatModifierType.Percent)
            )

            roll < 0.67f -> listOf(
                StatModifier(StatIds.Actor.DamageTakenMultiplier, -0.3f, StatModifierType.Percent)
            )

            else -> listOf(
                StatModifier(StatIds.Skills.CombatXp, 1.0f, StatModifierType.Percent)
            )
        }
        addEffect(effectSource, sourceName, modifiers)
    }

    override fun onCombatEnd(result: com.rorokaiiworks.goodidlegame.core.combat.CombatResult) {
        removeEffect(effectSource)
    }

    override fun onDeactivate() {
        removeEffect(effectSource)
        super.onDeactivate()
    }
}

private fun thinkerPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private var stacks = 0

    override fun onCombatEnemyDied(enemy: com.rorokaiiworks.goodidlegame.core.enemies.Enemy) {
        stacks += 1
    }

    override fun onCombatRewards(skillAction: SkillAction, dropTable: DropTable?, items: List<Item>): List<Item> {
        if (dropTable == null) return items
        if (player.healthRatio < 0.5f) {
            stacks = 0
            return items
        }
        val bonusItems = rollRareBonusItems(this, dropTable, stacks, 0.01f)
        if (bonusItems.isEmpty()) return items
        return items + bonusItems
    }

    override fun onDeactivate() {
        stacks = 0
        super.onDeactivate()
    }
}

private fun shocklyPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private var regenRemaining = 0f
    private var cooldownRemaining = 0f

    override fun tick(delta: Float) {
        if (cooldownRemaining > 0f) cooldownRemaining = max(0f, cooldownRemaining - delta)

        if (regenRemaining > 0f) {
            val healPerSecond = player.stats[StatIds.Actor.MaxHealth]!!.value * 0.05f
            player.stats[StatIds.Actor.Health]!!.executeFlatChange(healPerSecond * delta)
            regenRemaining -= delta
            if (regenRemaining <= 0f) regenRemaining = 0f
            return
        }

        if (cooldownRemaining <= 0f && player.healthRatio < 0.2f) {
            regenRemaining = 10f
            cooldownRemaining = 120f
        }
    }

    override fun onDeactivate() {
        regenRemaining = 0f
        cooldownRemaining = 0f
        super.onDeactivate()
    }
}

private fun panicPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private val stunSource = "${perk.id}_stun"
    private var stunRemaining = 0f
    private var stunnedEnemy: IActor? = null

    override fun tick(delta: Float) {
        if (stunRemaining > 0f) {
            stunRemaining -= delta
            if (stunRemaining <= 0f) {
                stunnedEnemy?.let { removeEnemyEffect(stunSource, it) }
                stunnedEnemy = null
                stunRemaining = 0f
            }
        }
    }

    override fun onCombatAttackHit(processedAttack: ProcessedAttack) {
        if (processedAttack.attacker != player) return
        if (stunnedEnemy != null) return
        if (perkRandom.nextFloat() >= 0.05f) return

        val defender = processedAttack.defender
        addEnemyEffect(
            source = stunSource,
            target = defender,
            sourceName = sourceName,
            modifiers = listOf(
                StatModifier(StatIds.Actor.AttackSpeed, -1.0f, StatModifierType.Percent)
            )
        )
        stunnedEnemy = defender
        stunRemaining = 5f
    }

    override fun onDeactivate() {
        stunnedEnemy?.let { removeEnemyEffect(stunSource, it) }
        stunnedEnemy = null
        stunRemaining = 0f
        super.onDeactivate()
    }
}

private fun unbearablePerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    override fun onCombatAttackHit(processedAttack: ProcessedAttack) {
        if (processedAttack.defender != player) return
        if (processedAttack.slashDamage <= 0f) return
        if (perkRandom.nextFloat() >= 0.10f) return

        val reflectDamage = processedAttack.slashDamage * 0.5f
        val attacker = processedAttack.attacker
        attacker.takeDamage(
            ProcessedAttack(
                attacker = player,
                defender = attacker,
                slashDamage = reflectDamage,
                punctureDamage = 0f,
                impactDamage = 0f,
                isOneHanded = false,
                isTwoHanded = false,
                isRanged = false,
            )
        )
    }
}

private fun evilPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private val effectSource = "${perk.id}_fishing_speed"

    override fun onInit() {
        addEffect(
            source = effectSource,
            sourceName = sourceName,
            modifiers = listOf(
                StatModifier(StatIds.Skills.FishingSpeed, 0.15f, StatModifierType.Percent)
            )
        )
    }

    override fun onSkillActionFinish(skillAction: SkillAction, yields: List<Item>): List<Item> {
        if (skillAction.skillId != "skill_fishing") return yields
        if (perkRandom.nextFloat() < 0.10f) return emptyList()
        return yields
    }

    override fun onDeactivate() {
        removeEffect(effectSource)
        super.onDeactivate()
    }
}

private fun unbelievePerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    override fun onSkillActionFinish(skillAction: SkillAction, yields: List<Item>): List<Item> {
        if (skillAction.skillId != "skill_woodcutting") return yields
        if (perkRandom.nextFloat() >= 0.05f) return yields
        return yields + itemService.createItem("charcoal")
    }
}

private fun getSomeWorkPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    override fun modifySkillActionXp(skillAction: SkillAction, xp: Long): Long {
        if (skillAction !is SkillAction.CombatSkillAction) return xp

        val reduced = (xp * 0.3f).toLong()
        val remaining = xp - reduced
        val distribute = (reduced * 0.5f).toLong()

        val nonCombatSkills = playerSkills.skills.values.filter {
            it.template.skillType != SkillType.Combat
        }
        if (nonCombatSkills.isNotEmpty() && distribute > 0) {
            val each = distribute / nonCombatSkills.size
            if (each > 0) {
                nonCombatSkills.forEach { it.addXp(each, player.stats) }
            }
        }

        return remaining
    }
}

private fun spaceManagementPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private val effectSource = "${perk.id}_space"
    private var lastUsed = -1
    private var lastMax = -1

    override fun tick(delta: Float) {
        val inventory = playerInventory.inventory
        val used = inventory.usedSlots
        val maxSlots = inventory.maxSlots

        if (used == lastUsed && maxSlots == lastMax) return
        lastUsed = used
        lastMax = maxSlots

        removeEffect(effectSource)

        val empty = max(0, maxSlots - used)
        val damageBonus = empty * 0.002f
        val damageTakenReduction = used * 0.002f

        addEffect(
            source = effectSource,
            sourceName = sourceName,
            modifiers = listOf(
                StatModifier(StatIds.Actor.DamageMultiplier, damageBonus, StatModifierType.Percent),
                StatModifier(StatIds.Actor.DamageTakenMultiplier, -damageTakenReduction, StatModifierType.Percent),
            )
        )
    }

    override fun onDeactivate() {
        removeEffect(effectSource)
        super.onDeactivate()
    }
}

private fun troublePerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    override fun onCombatRewards(skillAction: SkillAction, dropTable: DropTable?, items: List<Item>): List<Item> {
        if (dropTable == null) return items
        val coinItems = items.filter { it.template.id == "coins" }
        if (coinItems.isEmpty()) return items

        val filtered = items.filterNot { it.template.id == "coins" }.toMutableList()
        val candidates = dropTable.entries.filter { it.itemId != "coins" }
        if (candidates.isEmpty()) return filtered

        coinItems.forEach { coinItem ->
            repeat(max(1, coinItem.count.toInt())) {
                val entry = candidates.random()
                val amount = kotlin.random.Random.nextInt(entry.min, entry.max + 1)
                repeat(amount) {
                    filtered += itemService.createItem(entry.itemId)
                }
            }
        }

        return filtered
    }
}

private fun distractionPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private val effectSource = "${perk.id}_archaeology"

    override fun beforeSkillAction(skillAction: SkillAction) {
        if (skillAction !is SkillAction.CombatSkillAction) return
        addEffect(
            source = effectSource,
            sourceName = sourceName,
            modifiers = listOf(
                StatModifier(StatIds.Skills.ArchaeologySpeed, 0.10f, StatModifierType.Percent)
            )
        )
    }

    override fun onCombatEnd(result: com.rorokaiiworks.goodidlegame.core.combat.CombatResult) {
        removeEffect(effectSource)
    }

    override fun onDeactivate() {
        removeEffect(effectSource)
        super.onDeactivate()
    }
}

private fun goodPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private val effectSource = "${perk.id}_interval_damage"
    private var active = false

    override fun onCombatAttackStart(
        attacker: IActor,
        defender: IActor
    ) {
        if (attacker != player || active) return
        val interval = DamageCalculator.getAttackDuration(player)
        val steps = floor(interval / 2f).toInt()
        if (steps <= 0) return

        val bonus = steps * 0.10f
        addEffect(
            source = effectSource,
            sourceName = sourceName,
            modifiers = listOf(
                StatModifier(StatIds.Actor.DamageMultiplier, bonus, StatModifierType.Percent)
            )
        )
        active = true
    }

    override fun onCombatAttackHit(processedAttack: ProcessedAttack) {
        if (processedAttack.attacker != player || !active) return
        removeEffect(effectSource)
        active = false
    }

    override fun onCombatAttackMissed(
        attacker: IActor,
        defender: IActor
    ) {
        if (attacker != player || !active) return
        removeEffect(effectSource)
        active = false
    }

    override fun onDeactivate() {
        removeEffect(effectSource)
        active = false
        super.onDeactivate()
    }
}

private fun mainRolePerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private val enemyHitSource = "${perk.id}_enemy_hit"

    override fun onCombatAttackStart(
        attacker: IActor,
        defender: IActor
    ) {
        if (defender != player || attacker == player) return
        addEnemyEffect(
            source = enemyHitSource,
            target = attacker,
            sourceName = sourceName,
            modifiers = listOf(
                StatModifier(StatIds.Actor.HitChanceBonus, 0.20f, StatModifierType.Flat)
            )
        )
    }

    override fun onCombatAttackHit(processedAttack: ProcessedAttack) {
        if (processedAttack.attacker != player) {
            removeEnemyEffect(enemyHitSource, processedAttack.attacker)
        }
    }

    override fun onCombatAttackMissed(
        attacker: IActor,
        defender: IActor
    ) {
        if (attacker != player) {
            removeEnemyEffect(enemyHitSource, attacker)
        }
    }

    override fun onCombatRewards(skillAction: SkillAction, dropTable: DropTable?, items: List<Item>): List<Item> {
        if (dropTable == null) return items
        val bonusItems = applyRareMultiplier(this, dropTable, 1.5f)
        if (bonusItems.isEmpty()) return items
        return items + bonusItems
    }
}

private fun adjPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private val effectSource = "${perk.id}_adjust"

    override fun onInit() {
        addEffect(
            source = effectSource,
            sourceName = sourceName,
            modifiers = listOf(
                StatModifier(StatIds.Actor.HitChanceBonus, -0.20f, StatModifierType.Flat),
                StatModifier(StatIds.Skills.CombatYield, 0.10f, StatModifierType.Percent),
                StatModifier(StatIds.Skills.GatherYield, 0.10f, StatModifierType.Percent),
                StatModifier(StatIds.Skills.CraftYield, 0.10f, StatModifierType.Percent),
            )
        )
    }

    override fun onDeactivate() {
        removeEffect(effectSource)
        super.onDeactivate()
    }
}

private fun lolPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    override fun onSkillActionFinish(skillAction: SkillAction, yields: List<Item>): List<Item> {
        if (perkRandom.nextFloat() < 0.10f) {
            soundPlayer.playSound("lol")
        }
        return yields
    }
}

private fun steamPerk(perk: Perk, sourceName: ISourceName) = object : PerkLogic(perk, sourceName) {
    private var subscribed = false

    override fun onInit() {
        if (subscribed) return
        subscribed = true
        CoroutineScope(Dispatchers.Default).launch {
            eventBus.events.collect { event ->
                if (event is com.rorokaiiworks.goodidlegame.core.events.IEvent.SteamOverlayOpened) {
                    playerInventory.inventory.addItem(itemService.createItem("coins", 1))
                }
            }
        }
    }
}

val equipmentPerkLogics: Map<String, (Perk, ISourceName) -> PerkLogic> = mapOf(
    "pe_majesty_perk" to { perk, sourceName -> majestyPerk(perk, sourceName) },
    "pe_sincerely_perk" to { perk, sourceName -> sincerelyPerk(perk, sourceName) },
    "pe_brave_perk" to { perk, sourceName -> bravePerk(perk, sourceName) },
    "pe_thinker_perk" to { perk, sourceName -> thinkerPerk(perk, sourceName) },
    "pe_shockly_perk" to { perk, sourceName -> shocklyPerk(perk, sourceName) },
    "pe_panic_perk" to { perk, sourceName -> panicPerk(perk, sourceName) },
    "pe_unbearable_perk" to { perk, sourceName -> unbearablePerk(perk, sourceName) },
    "pe_evil_perk" to { perk, sourceName -> evilPerk(perk, sourceName) },
    "pe_unbelieve_perk" to { perk, sourceName -> unbelievePerk(perk, sourceName) },
    "pe_get_some_work_perk" to { perk, sourceName -> getSomeWorkPerk(perk, sourceName) },
    "pe_space_management_perk" to { perk, sourceName -> spaceManagementPerk(perk, sourceName) },
    "pe_trouble_perk" to { perk, sourceName -> troublePerk(perk, sourceName) },
    "pe_distraction_perk" to { perk, sourceName -> distractionPerk(perk, sourceName) },
    "pe_good_perk" to { perk, sourceName -> goodPerk(perk, sourceName) },
    "pe_main_role_perk" to { perk, sourceName -> mainRolePerk(perk, sourceName) },
    "pe_adj_perk" to { perk, sourceName -> adjPerk(perk, sourceName) },
    "pe_lol_perk" to { perk, sourceName -> lolPerk(perk, sourceName) },
    "pe_steam_perk" to { perk, sourceName -> steamPerk(perk, sourceName) },
)
