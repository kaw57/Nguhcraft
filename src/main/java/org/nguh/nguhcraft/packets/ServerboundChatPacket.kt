package org.nguh.nguhcraft.packets

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import org.nguh.nguhcraft.Utils

/**
* This is a chat message packet. It contains a chat message. That’s
* it. No signing, no encryption, no nonsense size limits; just a blob
* of text; that’s all it needs to be. If you want privacy, go talk
* somewhere that isn’t a goddamn Minecraft server ffs.
*/
data class ServerboundChatPacket(
    var Message: String
) : CustomPayload {
    override fun getId() = ID
    companion object {
        val ID = Utils.PacketId<ServerboundChatPacket>("serverbound/chat")
        val CODEC: PacketCodec<ByteBuf, ServerboundChatPacket>
            = PacketCodecs.STRING.xmap(::ServerboundChatPacket, ServerboundChatPacket::Message)
    }
}
