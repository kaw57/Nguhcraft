package org.nguh.nguhcraft.packets;

import io.netty.buffer.ByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import org.nguh.nguhcraft.Utils

/** Sent to a client to tell them whether they bypass region protection. */
data class ClientboundSyncProtectionBypassPacket (
    /** Whether the player bypasses region protection. */
    val BypassesRegionProtection: Boolean
) : CustomPayload {
    override fun getId() = ID
    companion object {
        val ID = Utils.PacketId<ClientboundSyncProtectionBypassPacket>("clientbound/sync_protection_bypass")
        val CODEC: PacketCodec<ByteBuf, ClientboundSyncProtectionBypassPacket> = PacketCodecs.BOOL.xmap(
            ::ClientboundSyncProtectionBypassPacket,
            ClientboundSyncProtectionBypassPacket::BypassesRegionProtection
        )
    }
}

