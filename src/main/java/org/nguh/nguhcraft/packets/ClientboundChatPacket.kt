package org.nguh.nguhcraft.packets

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import org.nguh.nguhcraft.Utils

/**
* Chat message sent from the server to the client.
* <p>
* This is used for chat messages and DMs. The message content must be
* a literal string and will be rendered as Markdown on the client.
*/
data class ClientboundChatPacket (
    /**
    * The name to display for the sender/receiver of this message.
    * <p>
    * If this is a public or incoming message, this will be the sender’s
    * name, otherwise, the receiver’s name.
    */
    var PlayerName: Text,

    /** The message content. */
    var Content: String,

    /** Whether this is a private message. */
    var MessageKind: Byte
) : CustomPayload {
    override fun getId() = ID
    companion object {
        const val MK_PUBLIC: Byte = 0
        const val MK_OUTGOING_DM: Byte = 1
        const val MK_INCOMING_DM: Byte = 2

        val ID = Utils.PacketId<ClientboundChatPacket>("clientbound/chat")
        val CODEC: PacketCodec<RegistryByteBuf, ClientboundChatPacket> = PacketCodec.tuple(
            TextCodecs.UNLIMITED_REGISTRY_PACKET_CODEC, ClientboundChatPacket::PlayerName,
            PacketCodecs.STRING, ClientboundChatPacket::Content,
            PacketCodecs.BYTE, ClientboundChatPacket::MessageKind,
            ::ClientboundChatPacket
        )
    }
}