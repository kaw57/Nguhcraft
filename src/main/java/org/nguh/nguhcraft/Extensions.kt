package org.nguh.nguhcraft

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import org.nguh.nguhcraft.client.NguhcraftClient
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor

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