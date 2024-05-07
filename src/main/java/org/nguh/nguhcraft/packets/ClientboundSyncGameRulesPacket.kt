package org.nguh.nguhcraft.packets

import io.netty.buffer.ByteBuf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload

data class ClientboundSyncGameRulesPacket (
    var Flags: Long
) : CustomPayload {
    override fun getId() = ID
    companion object {
        val ID: CustomPayload.Id<ClientboundSyncGameRulesPacket>
            = CustomPayload.id("nguhcraft:clientbound/sync_game_rules")

        val CODEC: PacketCodec<ByteBuf, ClientboundSyncGameRulesPacket> = PacketCodecs.VAR_LONG.xmap(
            ::ClientboundSyncGameRulesPacket,
            ClientboundSyncGameRulesPacket::Flags
        )
    }

}