package org.nguh.nguhcraft.block

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhBlocks {
    val DECORATIVE_HOPPER = Register(
        "decorative_hopper",
        DecorativeHopperBlock(AbstractBlock.Settings.copy(Blocks.HOPPER))
    )

    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register {
            it.add(DECORATIVE_HOPPER)
        }
    }

    private fun Register(Key: String, B: Block): Block {
        val I = Id(Key)
        Registry.register(Registries.ITEM, I, BlockItem(B, Item.Settings()))
        return Registry.register(Registries.BLOCK, I, B)
    }
}