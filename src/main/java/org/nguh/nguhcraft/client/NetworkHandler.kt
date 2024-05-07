package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.packets.ClientboundLinkUpdatePacket

@Environment(EnvType.CLIENT)
object NetworkHandler {
    @JvmStatic fun HandleLinkUpdatePacket(Packet: ClientboundLinkUpdatePacket) {
        println("Got ClientboundLinkUpdatePacket: $Packet")

        val C = Client()
        val NW = C.networkHandler ?: return
        C.execute {
            NW.playerList.firstOrNull { it.profile.id == Packet.PlayerId }?.let {
                // The player list also shows the Minecraft name.
                it.displayName = if (!Packet.Linked) Text.literal(Packet.MinecraftName).formatted(Formatting.GRAY)
                else Text.literal(Packet.DiscordName).withColor(Packet.DiscordColour)
                    .append(Text.literal(" "))
                    .append(Utils.BracketedLiteralComponent(Packet.MinecraftName, false))
            }
        }
    }
}