package org.nguh.nguhcraft.packets

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import org.nguh.nguhcraft.Utils

data class ClientboundSyncHypershotStatePacket(
    var InContext: Boolean
) : CustomPayload {
    override fun getId() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncHypershotStatePacket>("clientbound/sync_hypershot_state")
        val CODEC: PacketCodec<ByteBuf, ClientboundSyncHypershotStatePacket> = PacketCodecs.BOOL.xmap(
            ::ClientboundSyncHypershotStatePacket,
            ClientboundSyncHypershotStatePacket::InContext
        )
    }
}
