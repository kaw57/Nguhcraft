package org.nguh.nguhcraft.enchantment

import net.minecraft.enchantment.Enchantment

class TreasureEnchantment(properties: Properties) : Enchantment(properties) {
    // Prevent the enchantment from showing up randomly.
    override fun isTreasure(): Boolean = true
    override fun isAvailableForEnchantedBookOffer(): Boolean = false
    override fun isAvailableForRandomSelection(): Boolean  = false
}