package org.nguh.nguhcraft.protect

import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.accessors.WorldAccessor
import org.nguh.nguhcraft.client.accessors.AbstractClientPlayerEntityAccessor
import org.nguh.nguhcraft.server.isLinked

object ProtectionManager {
    /** Check if a player is allowed to break or start breaking a certain block. */
    @JvmStatic
    fun AllowBlockBreak(
        PE: PlayerEntity,
        W: World,
        Pos: BlockPos
    ) : Boolean {
        // Player is operator. Always allow.
        if (PE.hasPermissionLevel(4)) return true

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return false

        // Block is within the bounds of a protected region. Deny.
        if (IsProtectedBlock(W, Pos)) return false

        // Otherwise, allow.
        return true
    }

    /** Check if a player is linked. */
    fun IsLinked(PE: PlayerEntity) = when (PE) {
        is ServerPlayerEntity -> PE.isLinked
        is ClientPlayerEntity -> (PE as AbstractClientPlayerEntityAccessor).isLinked
        else -> false
    }

    /** Check if a block is within a protected region. */
    fun IsProtectedBlock(W: World, Pos: BlockPos): Boolean {
        val Regions = (W as WorldAccessor).regions
        val R = Regions.find { it.Contains(Pos) } ?: return false
        return !R.AllowsBlockModification()
    }
}
