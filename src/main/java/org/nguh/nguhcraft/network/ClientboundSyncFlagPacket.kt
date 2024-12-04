package org.nguh.nguhcraft.network

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import org.nguh.nguhcraft.Utils

enum class ClientFlags {
    IN_HYPERSHOT_CONTEXT,
    BYPASSES_REGION_PROTECTION,
    VANISHED,
}

/**
* Sent to a client to tell inform them of various state changes.
*/
data class ClientboundSyncFlagPacket(
    /** The flag that is being synchronized. */
    val Flag: ClientFlags,

    /** The value of the flag. */
    val Value: Boolean
) : CustomPayload {
    fun write(buf: ByteBuf) {
        buf.writeByte(Flag.ordinal)
        buf.writeBoolean(Value)
    }

    override fun getId() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncFlagPacket>("clientbound/sync_protection_bypass")
        val CODEC: PacketCodec<ByteBuf, ClientboundSyncFlagPacket> = PacketCodec.of(
            { obj: ClientboundSyncFlagPacket, buf: ByteBuf -> obj.write(buf) },
            { buf: ByteBuf -> ClientboundSyncFlagPacket(ClientFlags.entries[buf.readByte().toInt()], buf.readBoolean()) }
        )
    }
}

