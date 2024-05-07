package org.nguh.nguhcraft.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.packets.ClientboundChatPacket
import org.nguh.nguhcraft.packets.ClientboundLinkUpdatePacket

@Environment(EnvType.CLIENT)
class NguhcraftClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundLinkUpdatePacket.ID) { Payload, _ ->
            NetworkHandler.HandleLinkUpdatePacket(Payload)
        }

        ClientPlayNetworking.registerGlobalReceiver(ClientboundChatPacket.ID) { Payload, _ ->
            NetworkHandler.HandleChatPacket(Payload)
        }
    }
}
