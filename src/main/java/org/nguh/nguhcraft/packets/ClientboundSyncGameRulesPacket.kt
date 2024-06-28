package org.nguh.nguhcraft.packets

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import org.nguh.nguhcraft.Utils

data class ClientboundSyncGameRulesPacket (
    var Flags: Long
) : CustomPayload {
    override fun getId() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncGameRulesPacket>("clientbound/sync_game_rules")
        val CODEC: PacketCodec<ByteBuf, ClientboundSyncGameRulesPacket> = PacketCodecs.VAR_LONG.xmap(
            ::ClientboundSyncGameRulesPacket,
            ClientboundSyncGameRulesPacket::Flags
        )
    }

}