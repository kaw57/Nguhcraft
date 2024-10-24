package org.nguh.nguhcraft.block

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
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
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhBlocks {
    // Blocks.
    val DECORATIVE_HOPPER = Register(
        "decorative_hopper",
        ::DecorativeHopperBlock,
        AbstractBlock.Settings.copy(Blocks.HOPPER)
    )

    val LOCKED_DOOR =  Register(
        "locked_door",
        ::LockedDoorBlock,
        AbstractBlock.Settings.create()
            .mapColor(MapColor.GOLD)
            .requiresTool().strength(5.0f, 3600000.0F)
            .nonOpaque()
            .pistonBehavior(PistonBehavior.IGNORE)
    )

    // Block entities.
    val LOCKED_DOOR_BLOCK_ENTITY = RegisterEntity(
        "lockable_door",
        FabricBlockEntityTypeBuilder
            .create(::LockedDoorBlockEntity, LOCKED_DOOR)
            .build()
    )

    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register {
            it.add(DECORATIVE_HOPPER)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register {
            it.add(LOCKED_DOOR)
        }
    }

    private fun Register(
        Key: String,
        Ctor: (S: AbstractBlock.Settings) -> Block,
        S: AbstractBlock.Settings,
        ItemCtor: (B: Block, S: Item.Settings) -> Item = ::BlockItem
    ): Block {
        // Create registry keys.
        val ItemKey = RegistryKey.of(RegistryKeys.ITEM, Id(Key))
        val BlockKey = RegistryKey.of(RegistryKeys.BLOCK, Id(Key))

        // Set the registry key for the block settings.
        S.registryKey(BlockKey)

        // Create and register the block.
        val B = Ctor(S)
        Registry.register(Registries.BLOCK, BlockKey, B)

        // Create and register the item.
        val ItemSettings = Item.Settings()
            .useBlockPrefixedTranslationKey()
            .registryKey(ItemKey)
        val I = ItemCtor(B, ItemSettings)
        Registry.register(Registries.ITEM, ItemKey, I)
        return B
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