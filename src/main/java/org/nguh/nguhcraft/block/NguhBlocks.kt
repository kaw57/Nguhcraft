package org.nguh.nguhcraft.block

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.MapColor
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.piston.PistonBehavior
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.item.TallBlockItem
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhBlocks {
    // Blocks.
    val DECORATIVE_HOPPER = Register(
        "decorative_hopper",
        DecorativeHopperBlock(AbstractBlock.Settings.copy(Blocks.HOPPER))
    )

    val LOCKED_DOOR = LockedDoorBlock(AbstractBlock.Settings.create()
        .mapColor(MapColor.GOLD)
        .requiresTool().strength(5.0f, 3600000.0F)
        .nonOpaque()
        .pistonBehavior(PistonBehavior.IGNORE)
    ).also { Register("locked_door", it, TallBlockItem(it, Item.Settings())) }

    // Block entities.
    val LOCKED_DOOR_BLOCK_ENTITY = RegisterEntity(
        "lockable_door",
        BlockEntityType.Builder.create(::LockedDoorBlockEntity, LOCKED_DOOR).build()
    )

    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register {
            it.add(DECORATIVE_HOPPER)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register {
            it.add(LOCKED_DOOR)
        }
    }

    private fun Register(Key: String, B: Block) = Register(Key, B, BlockItem(B, Item.Settings()))
    private fun Register(Key: String, B: Block, It: Item): Block {
        val I = Id(Key)
        Registry.register(Registries.ITEM, I, It)
        return Registry.register(Registries.BLOCK, I, B)
    }

    private fun <C : BlockEntity> RegisterEntity(
        Key: String,
        Type: BlockEntityType<C>
    ): BlockEntityType<C> = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Id(Key),
        Type
    )
}