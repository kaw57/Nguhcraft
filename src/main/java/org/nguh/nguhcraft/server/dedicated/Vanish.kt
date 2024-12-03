package org.nguh.nguhcraft.server.dedicated

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.nguh.nguhcraft.server.Broadcast
import org.nguh.nguhcraft.server.IsVanished
import org.nguh.nguhcraft.server.ServerUtils

/**
* Prevents other players from seeing a player.
*
* Note that this is only guaranteed to work properly if two
* conditions are met: First, the vanished player is an operator,
* and second, the vanished player is in creative or spectator
* mode.
*/
@Environment(EnvType.SERVER)
object Vanish {
    fun Toggle(SP: ServerPlayerEntity) {
        if (SP.IsVanished) ShowPlayer(SP) else HidePlayer(SP)
        SP.IsVanished = !SP.IsVanished
    }

    @JvmStatic
    fun IsVanished(SP: ServerPlayerEntity): Boolean = SP.IsVanished

    private fun HidePlayer(SP: ServerPlayerEntity) {
        val P = PlayerRemoveS2CPacket(listOf(SP.uuid))

        // Detach any vehicles, otherwise, we might get into weird situations
        // where a client receives packets for a player that is to them no longer
        // on the server.
        SP.detach()

        // Broadcast the packet to all players.
        SP.server.Broadcast(SP, P)

        // As well as a fake quit message.
        SP.server.Broadcast(Text.translatable("multiplayer.player.left", SP.displayName))
        Discord.BroadcastJoinQuitMessage(SP, false)
    }

    private fun ShowPlayer(SP: ServerPlayerEntity) {
        val P = PlayerListS2CPacket.entryFromPlayer(listOf(SP))

        // Broadcast the packet to all players.
        SP.server.Broadcast(SP, P)

        // As well as a fake join message.
        SP.server.Broadcast(Text.translatable("multiplayer.player.joined", SP.displayName))
        Discord.BroadcastJoinQuitMessage(SP, true)
    }
}