package org.nguh.nguhcraft.packets

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import java.util.*

data class ClientboundLinkUpdatePacket(
    /** The player.  */
    val PlayerId: UUID,

    /** The discord colour of the player.  */
    val DiscordColour: Int,

    /** The discord display name, if any.  */
    val DiscordName: String,

    /** Whether the player is linked.  */
    val Linked: Boolean
) : CustomPayload {
    override fun getId() = ID

    private constructor(buf: PacketByteBuf) : this(
        buf.readUuid(),
        buf.readInt(),
        buf.readString(),
        buf.readBoolean(),
    )

    private fun write(buf: PacketByteBuf) {
        buf.writeUuid(PlayerId)
        buf.writeInt(DiscordColour)
        buf.writeString(DiscordName)
        buf.writeBoolean(Linked)
    }

    companion object {
        @JvmField
        val ID: CustomPayload.Id<ClientboundLinkUpdatePacket>
            = CustomPayload.id("nguhcraft:packet_link_update")

        @JvmField
        val CODEC: PacketCodec<PacketByteBuf, ClientboundLinkUpdatePacket> = PacketCodec.of(
            { obj: ClientboundLinkUpdatePacket, buf: PacketByteBuf -> obj.write(buf) },
            { buf: PacketByteBuf -> ClientboundLinkUpdatePacket(buf) })
    }
}
