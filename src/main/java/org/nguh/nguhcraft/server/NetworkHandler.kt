package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
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

@Environment(EnvType.SERVER)
object NetworkHandler {
    private val LOGGER = LogUtils.getLogger()
    private val ERR_ILLEGAL_CHARS: Text = Text.translatable("multiplayer.disconnect.illegal_characters")
    private val ERR_EMPTY_MESSAGE: Text = Text.translatable("Client attempted to send an empty message.")
    private val ERR_NEEDS_LINK_TO_CHAT: Text = Text.translatable("You must link your account to send messages in chat or run commands (other than /discord link)").formatted(Formatting.RED)

    /** Ensure a player is linked and issue an error if they aren’t */
    private fun EnsurePlayerIsLinked(Context: Context): Boolean {
        val SP = Context.player()
        if (!SP.isLinked) {
            Context.responseSender().sendPacket(GameMessageS2CPacket(ERR_NEEDS_LINK_TO_CHAT, false))
            return false
        }
        return true
    }

    /** Format a chat message.  */
    private fun FormatMessage(Sender: ServerPlayerEntity?, Message: String): Text {
        // Server message.
        if (Sender == null) return Text.literal("[Server] ").append(Message)

        // Unlinked players cannot send chat messages, so discord name must not be null.
        return Sender.displayName!!.copy()
            .append(": ").withColor(Sender.discordColour)
            .append(Text.literal(Message).formatted(Formatting.WHITE))
    }

    /** Handle an incoming chat message. */
    fun HandleChatMessage(Message: String, Context: Context) {
        if (
            !ValidateIncomingMessage(Message, Context.player()) ||
            !EnsurePlayerIsLinked(Context)
        ) return

        // Dew it.
        val SP = Context.player()
        val S = SP.server
        S.execute {
            val Msg = FormatMessage(SP, Message)
            S.playerManager.broadcast(Msg, false)
            Discord.ForwardChatMessage(SP, Message)
        }
    }

    /** Handle an incoming command. */
    @JvmStatic
    fun HandleCommand(Handler: ServerPlayNetworkHandler, Command: String) {
        if (!ValidateIncomingMessage(Command, Handler.player)) return

        // An unlinked player can only run /discord link.
        val SP = Handler.player
        if (!SP.isLinkedOrOperator && !Command.startsWith("discord")) {
            Handler.sendPacket(GameMessageS2CPacket(ERR_NEEDS_LINK_TO_CHAT, false))
            return
        }

        // Log the command to the console.
        LOGGER.info("Player {} ran command: /{}", SP.name, Command)

        // Dew it.
        val S = SP.server
        S.execute {
            val ParsedCommand = S.commandManager.dispatcher.parse(Command, SP.commandSource)
            S.commandManager.execute(ParsedCommand, Command)
        }
    }

    /** Validate an incoming chat message or command. */
    private fun ValidateIncomingMessage(Incoming: String, SP: ServerPlayerEntity): Boolean {
        val S = SP.server

        // Server is shutting down.
        if (S.isStopped) return false

        // Player has disconnected.
        if (SP.isDisconnected || SP.isRemoved) return false

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
            SP.networkHandler.sendPacket(
                GameMessageS2CPacket(
                    Text.translatable("chat.disabled.options").formatted(Formatting.RED),
                    false
                )
            )
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