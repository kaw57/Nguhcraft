package org.nguh.nguhcraft.enchantment

import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantment.constantCost
import net.minecraft.enchantment.Enchantment.properties
import net.minecraft.entity.EquipmentSlot
import net.minecraft.registry.tag.ItemTags

object NguhcraftEnchantments {
    @JvmField val HOMING = TreasureEnchantment(
        properties(
            ItemTags.BOW_ENCHANTABLE,
            1,
            1,
            constantCost(200),
            constantCost(200),
            1,
            EquipmentSlot.MAINHAND
        )
    )
}