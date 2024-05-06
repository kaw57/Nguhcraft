package org.nguh.nguhcraft.packets

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload

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

    private constructor(buf: PacketByteBuf) : this(buf.readString())
    private fun write(buf: PacketByteBuf) { buf.writeString(Message) }

    companion object {
        @JvmField
        val ID: CustomPayload.Id<ServerboundChatPacket>
            = CustomPayload.id("nguhcraft:packet_chat")

        @JvmField
        val CODEC: PacketCodec<PacketByteBuf, ServerboundChatPacket> = PacketCodec.of(
            { obj: ServerboundChatPacket, buf: PacketByteBuf -> obj.write(buf) },
            { buf: PacketByteBuf -> ServerboundChatPacket(buf) })
    }
}
