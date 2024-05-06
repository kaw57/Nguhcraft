package org.nguh.nguhcraft.server


import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context
import net.minecraft.server.network.ServerPlayNetworkHandler

@Environment(EnvType.SERVER)
object NetworkHandler {
    @JvmStatic fun HandleChatMessage(Message: String, Context: Context) = Chat.HandleChatMessage(Message, Context)
    @JvmStatic fun HandleCommand(Handler: ServerPlayNetworkHandler, Command: String) = Chat.HandleCommand(Handler, Command)
}