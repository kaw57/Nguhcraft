package org.nguh.nguhcraft.server


import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context
import net.minecraft.network.message.ChatVisibility
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.StringHelper
import org.nguh.nguhcraft.server.ServerUtils.Server

/** This runs on the network thread. */
@Environment(EnvType.SERVER)
object NetworkHandler {
    private val ERR_ILLEGAL_CHARS: Text = Text.translatable("multiplayer.disconnect.illegal_characters")
    private val ERR_CHAT_DISABLED: Text = Text.translatable("chat.disabled.options").formatted(Formatting.RED)
    private val ERR_EMPTY_MESSAGE: Text = Text.literal("Client attempted to send an empty message.")

    private fun Execute(CB: () -> Unit) = Server().execute(CB)

    @JvmStatic fun HandleChatMessage(Message: String, Context: Context) {
        if (!ValidateIncomingMessage(Message, Context.player())) return
        Execute { Chat.ProcessChatMessage(Message, Context) }
    }

    @JvmStatic fun HandleCommand(Handler: ServerPlayNetworkHandler, Command: String) {
        if (!ValidateIncomingMessage(Command, Handler.player)) return
        Execute { Chat.ProcessCommand(Handler, Command) }
    }

    /** Validate an incoming chat message or command. */
    private fun ValidateIncomingMessage(Incoming: String, SP: ServerPlayerEntity): Boolean {
        val S = SP.server

        // Server is shutting down.
        if (S.isStopped) return false

        // Player has disconnected.
        if (SP.isDisconnected) return false

        // Check for illegal characters.
        if (Incoming.any { !StringHelper.isValidChar(it) }) {
            SP.networkHandler.disconnect(ERR_ILLEGAL_CHARS)
            return false
        }

        // Disallow empty messages.
        if (Incoming.isEmpty()) {
            SP.networkHandler.disconnect(ERR_EMPTY_MESSAGE)
            return false
        }

        // Player has disabled chat.
        if (SP.clientChatVisibility == ChatVisibility.HIDDEN) {
            SP.networkHandler.sendPacket(GameMessageS2CPacket(ERR_CHAT_DISABLED, false))
            return false
        }

        // Ok.
        //
        // Unlinked players are allowed to run exactly one command: /discord link, so
        // we can’t check for that here. Our caller needs to do that.
        SP.updateLastActionTime()
        return true
    }
}