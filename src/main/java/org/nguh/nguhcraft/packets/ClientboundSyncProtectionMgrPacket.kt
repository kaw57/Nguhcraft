package org.nguh.nguhcraft.packets

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import org.nguh.nguhcraft.protect.ProtectionManager

/** Packet used to update the ProtectionManager state on the client. */
class ClientboundSyncProtectionMgrPacket(
    val Data: ProtectionManager.State
) : CustomPayload {
    override fun getId() = ID

    companion object {
        @JvmField
        val ID: CustomPayload.Id<ClientboundSyncProtectionMgrPacket>
            = CustomPayload.id("nguhcraft:clientbound/protection_mgr")

        @JvmField
        val CODEC: PacketCodec<RegistryByteBuf, ClientboundSyncProtectionMgrPacket> = PacketCodec.of(
            { Packet: ClientboundSyncProtectionMgrPacket, B: RegistryByteBuf -> Packet.Data.Write(B) },
            { B: RegistryByteBuf -> ClientboundSyncProtectionMgrPacket(ProtectionManager.State(B)) })
    }
}