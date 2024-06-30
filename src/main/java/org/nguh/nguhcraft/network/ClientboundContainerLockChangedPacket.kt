package org.nguh.nguhcraft.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.math.BlockPos
import org.nguh.nguhcraft.Utils

/**
* Used to inform client(s) that a chest has been (un)locked.
*
* This should REALLY use TrackedData instead, but I don’t
* think that works too well with components; if that’s not
* possible, instead queue up all updates and send them at
* the end of a tick. Thankfully, people won’t be (un)locking
* chests so often that this would become a bottleneck...
*/
data class ClientboundContainerLockChangedPacket(
    /** Block position of the container block entity. */
    val Pos: BlockPos,

    /** The new key (empty if unlocked). */
    val NewKey: String
) : CustomPayload {
    override fun getId() = ID

    private constructor(buf: RegistryByteBuf) : this(
        buf.readBlockPos(),
        buf.readString()
    )

    private fun write(buf: RegistryByteBuf) {
        buf.writeBlockPos(Pos)
        buf.writeString(NewKey)
    }

    companion object {
        val ID = Utils.PacketId<ClientboundContainerLockChangedPacket>("clientbound/container_lock_changed")
        val CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, ClientboundContainerLockChangedPacket::Pos,
            PacketCodecs.STRING, ClientboundContainerLockChangedPacket::NewKey,
            ::ClientboundContainerLockChangedPacket
        )
    }
}