package org.nguh.nguhcraft

import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.TagKey
import net.minecraft.server.network.ServerPlayerEntity
import org.nguh.nguhcraft.client.NguhcraftClient
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor

infix fun AbstractBlock.AbstractBlockState.isa(B: TagKey<Block>) = this.isIn(B)
infix fun AbstractBlock.AbstractBlockState.isa(B: RegistryEntry<Block>) = this.isOf(B)
infix fun BlockState.isa(B: Block) = this.isOf(B)

fun PlayerEntity.BypassesRegionProtection(): Boolean {
    // Server can check this directly.
    if (this is ServerPlayerEntity)
        return (this as ServerPlayerAccessor).bypassesRegionProtection

    // Clients should cache this value.
    if (this is ClientPlayerEntity)
        return NguhcraftClient.BypassesRegionProtection

    // Could get here for other clients, in which case we assume no bypass.
    return false
}
