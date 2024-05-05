package org.nguh.nguhcraft.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.MinecraftClient
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.packets.ClientboundLinkUpdatePacket

@Environment(EnvType.CLIENT)
class NguhcraftClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundLinkUpdatePacket.ID) { Payload, Context ->
            Context.client().execute { UpdatePlayerName(Payload) }
        }
    }

    private fun UpdatePlayerName(Payload: ClientboundLinkUpdatePacket) {
        val C = Client()
        val NW = C.networkHandler ?: return
        NW.playerList.firstOrNull { it.profile.id == Payload.PlayerId }?.let {
            println("Updating player name for ${it.displayName?.string} to ${Payload.DiscordName}")
        }
    }
}
