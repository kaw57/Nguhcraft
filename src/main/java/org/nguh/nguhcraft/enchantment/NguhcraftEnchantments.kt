package org.nguh.nguhcraft.enchantment

import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantment.constantCost
import net.minecraft.enchantment.Enchantment.properties
import net.minecraft.entity.EquipmentSlot
import net.minecraft.registry.tag.ItemTags

object NguhcraftEnchantments {
    val HOMING = TreasureEnchantment(
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

    val HYPERSHOT = TreasureEnchantment(
        properties(
            ItemTags.CROSSBOW_ENCHANTABLE,
            1,
            100,
            constantCost(200),
            constantCost(200),
            1,
            EquipmentSlot.MAINHAND
        )
    )

    val SMELTING = TreasureEnchantment(
        properties(
            ItemTags.MINING_ENCHANTABLE,
            1,
            1,
            constantCost(200),
            constantCost(200),
            1,
            EquipmentSlot.MAINHAND
        )
    )
}