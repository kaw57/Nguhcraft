package org.nguh.nguhcraft

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import org.nguh.nguhcraft.packets.ClientboundLinkUpdatePacket

class Nguhcraft : ModInitializer {
    override fun onInitialize() {
        PayloadTypeRegistry.playS2C().register(ClientboundLinkUpdatePacket.ID, ClientboundLinkUpdatePacket.CODEC)
    }
}
