package org.nguh.nguhcraft.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKeys
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.XZRect
import org.nguh.nguhcraft.server.Barrier

class ClientboundSyncBarriersPacket(
    val Barriers : List<Barrier>
) : CustomPayload {
    override fun getId() = ID

    private constructor(B: RegistryByteBuf) : this(
        List(B.readInt()) {
            Barrier(
                B.readInt(),
                B.readRegistryKey(RegistryKeys.WORLD),
                XZRect.ReadXZRect(B)
            )
        }
    )

    private fun Write(B: RegistryByteBuf) {
        B.writeInt(Barriers.size)
        for (Barrier in Barriers) {
            B.writeInt(Barrier.Colour)
            B.writeRegistryKey(Barrier.W)
            Barrier.XZ.WriteXZRect(B)
        }
    }

    companion object {
        val ID = Utils.PacketId<ClientboundSyncBarriersPacket>("clientbound/sync_barriers")
        val CODEC = MakeCodec(ClientboundSyncBarriersPacket::Write, ::ClientboundSyncBarriersPacket)
    }
}