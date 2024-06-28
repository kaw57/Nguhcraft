package org.nguh.nguhcraft.packets

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.protect.ProtectionManager

/** Packet used to update the ProtectionManager state on the client. */
class ClientboundSyncProtectionMgrPacket(
    val Data: ProtectionManager.State
) : CustomPayload {
    override fun getId() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncProtectionMgrPacket>("clientbound/sync_protection_mgr")
        val CODEC: PacketCodec<RegistryByteBuf, ClientboundSyncProtectionMgrPacket> = PacketCodec.of(
            { Packet: ClientboundSyncProtectionMgrPacket, B: RegistryByteBuf -> Packet.Data.Write(B) },
            { B: RegistryByteBuf -> ClientboundSyncProtectionMgrPacket(ProtectionManager.State(B)) })
    }
}