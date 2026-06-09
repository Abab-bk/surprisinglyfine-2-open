package com.rorokaiiworks.goodidlegame.core.items

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rorokaiiworks.goodidlegame.core.stats.ISourceName
import com.rorokaiiworks.goodidlegame.core.stats.StatModifier
import name.kropp.kotlinx.gettext.I18n
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class ItemRarity(val label: String) {
    Common("Common"),
    Uncommon("Uncommon"),
    Rare("Rare"),
    Epic("Epic"),
    Legendary("Legendary"),
    Myth("Myth");
}

@OptIn(ExperimentalUuidApi::class)
data class Item(
    val template: ItemTemplate,
    var count: Long = 1,
    var customModifiers: List<StatModifier> = listOf(),
    var enchantmentLevel: Int = 0,
): KoinComponent, ISourceName {
    private val i18n: I18n by inject()

    val uuid: Uuid = Uuid.random()
    val isUnique get() = customModifiers.isNotEmpty()

    var isLocked by mutableStateOf(false)

    override val sourceName: String get() = displayName

    val allModifiers: List<StatModifier>
        get() = (template.modifiers ?: emptyList()) + customModifiers

    val displayName: String get() {
        val name = i18n.tr(template.name)
        return if (enchantmentLevel > 0) {
            "[+$enchantmentLevel] $name"
        } else {
            name
        }
    }

    fun isSameItem(other: Item): Boolean {
        return template == other.template && customModifiers == other.customModifiers && enchantmentLevel == other.enchantmentLevel
    }

    fun toSaveData(): ItemSaveData = ItemSaveData(
        itemId = template.id,
        count = count,
        customModifiers = customModifiers,
        enchantmentLevel = enchantmentLevel,
    )
}