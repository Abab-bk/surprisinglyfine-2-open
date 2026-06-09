
package com.rorokaiiworks.goodidlegame.core.items

import com.rorokaiiworks.goodidlegame.ui.i18nWrapper

enum class ItemType(val label: String) {
    Misc(i18nWrapper("Misc")),
    Material(i18nWrapper("Material")),

    CombatPotion(i18nWrapper("CombatPotion")),
    GatherPotion(i18nWrapper("GatherPotion")),
    CraftPotion(i18nWrapper("CraftPotion")),

    Food(i18nWrapper("Food")),
    Flower(i18nWrapper("Flower")),
    Vegetable(i18nWrapper("Vegetable")),
    Fruit(i18nWrapper("Fruit")),
    Fish(i18nWrapper("Fish")),

    // Weapons
    Sword(i18nWrapper("Sword")),
    Hammer(i18nWrapper("Hammer")),
    Scythe(i18nWrapper("Scythe")),
    Spear(i18nWrapper("Spear")),
    Bow(i18nWrapper("Bow")),
    Dart(i18nWrapper("Dart")),

    // Armor
    Shield(i18nWrapper("Shield")),
    Helmet(i18nWrapper("Helmet")),
    Armor(i18nWrapper("Armor")),
    LegArmor(i18nWrapper("LegArmor")),
    Boots(i18nWrapper("Boots")),
    Cape(i18nWrapper("Cape")),

    // Accessories
    Necklace(i18nWrapper("Necklace")),
    Ring(i18nWrapper("Ring")),
    Bracelet(i18nWrapper("Bracelet")),

    // Tools
    Axe(i18nWrapper("Axe")),
    Pickaxe(i18nWrapper("Pickaxe")),
    Spade(i18nWrapper("Spade")),
    Rod(i18nWrapper("Rod")),
    Trap(i18nWrapper("Trap")),

    Relic(i18nWrapper("Relic"));

    companion object {
        val Weapons = setOf(Sword, Hammer, Scythe, Spear, Bow, Dart)
        val Armors = setOf(Shield, Helmet, Armor, LegArmor, Boots)

        val OneHandedWeapons = setOf(Sword, Hammer)
        val TwoHandedWeapons = setOf(Spear, Scythe)
        val RangedWeapons = setOf(Bow, Dart)

        val Accessories = setOf(Necklace, Ring, Bracelet, Cape)
        val Tools = setOf(Axe, Pickaxe, Spade, Rod, Trap)

        val Potions = setOf(CombatPotion, GatherPotion, CraftPotion)

        val Equipment = Weapons + Armors + Accessories + Tools
        val Farming = setOf(Flower, Vegetable, Fruit, Fish)
        val Props = Potions + Food

        fun isWeapon(itemType: ItemType) = itemType in Weapons
        fun isArmor(itemType: ItemType) = itemType in Armors
        fun isAccessory(itemType: ItemType) = itemType in Accessories
        fun isTool(itemType: ItemType) = itemType in Tools
    }
}