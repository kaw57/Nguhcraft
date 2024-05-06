package org.nguh.nguhcraft.server

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.message.ChatVisibility
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.StringHelper
import org.nguh.nguhcraft.packets.ServerboundChatPacket
import org.nguh.nguhcraft.server.Discord.Companion.Start
import org.nguh.nguhcraft.server.ServerUtils.Server
import kotlin.system.exitProcess


@Environment(EnvType.SERVER)
class NguhcraftServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        try {
            Start()
            Commands.Register()
            RegisterPacketHandlers()
        } catch (e: Exception) {
            e.printStackTrace()
            exitProcess(1)
        }
    }

    companion object {
        private fun RegisterPacketHandlers() {
            ServerPlayNetworking.registerGlobalReceiver(ServerboundChatPacket.ID) { Packet, Context ->
                NetworkHandler.HandleChatMessage(Packet.Message, Context)
            }
        }
    }
}
