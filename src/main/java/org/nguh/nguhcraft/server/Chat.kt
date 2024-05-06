package org.nguh.nguhcraft.server

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import com.mojang.logging.LogUtils
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context
import net.minecraft.network.message.ChatVisibility
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.StringHelper
import org.nguh.nguhcraft.Colours

/** This handles everything related to chat and messages */
@Environment(EnvType.SERVER)
object Chat {
    private val LOGGER = LogUtils.getLogger()
    private val ERR_ILLEGAL_CHARS: Text = Text.translatable("multiplayer.disconnect.illegal_characters")
    private val ERR_EMPTY_MESSAGE: Text = Text.translatable("Client attempted to send an empty message.")
    private val ERR_NEEDS_LINK_TO_CHAT: Text = Text.translatable("You must link your account to send messages in chat or run commands (other than /discord link)").formatted(Formatting.RED)

    /** Coloured components used in chat messages. */
    private val ARROW_COMPONENT: Text = Text.literal(" → ").withColor(Colours.DeepKoamaru)
    private val SRV_LIT_COMPONENT: Text = Text.literal("Server").withColor(Colours.Lavender)
    private val ME_COMPONENT = Text.literal("me").withColor(Colours.Lavender)
    private val COMMA_COMPONENT = Text.literal(", ").withColor(Colours.DeepKoamaru)

    /** Coloured '[' and '] ' components (the latter includes a space). */
    val LBRACK_COMPONENT: Text = Text.literal("[").withColor(Colours.DeepKoamaru)
    val RBRACK_COMPONENT: Text = Text.literal("] ").withColor(Colours.DeepKoamaru)

    /** Coloured '[Server] ' Component. */
    val SERVER_COMPONENT: Text = BracketedLiteralComponent("Server")

    /** Coloured '[Server -> me]' component. */
    private val SERVER_PRIVATE_MESSAGE_COMPONENT: Text = LBRACK_COMPONENT.copy()
        .append(SRV_LIT_COMPONENT)
        .append(ARROW_COMPONENT)
        .append(ME_COMPONENT)
        .append(RBRACK_COMPONENT)

    /**
    * Get a component enclosed in brackets, followed by a space
    * <p>
    * For example, for an input of "foo", this will return '[foo] ' with
    * appropriate formatting.
    */
    fun BracketedLiteralComponent(Content: String): Text = LBRACK_COMPONENT.copy()
        .append(Text.literal(Content).withColor(Colours.Lavender))
        .append(RBRACK_COMPONENT)

    /** Ensure a player is linked and issue an error if they aren’t. */
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
        if (Sender == null) return SERVER_COMPONENT.copy().append(Message)

        // Unlinked players cannot send chat messages, so discord name must not be null.
        return Sender.displayName!!.copy()
            .append(": ").withColor(Sender.discordColour)
            .append(Text.literal(Message).formatted(Formatting.WHITE))
    }

    /** Format a DM. */
    private fun FormatPrivateMessage(Sender: ServerPlayerEntity?, Message: Text): Text {
        // Server message.
        if (Sender == null) return SERVER_PRIVATE_MESSAGE_COMPONENT.copy().append(Message)

        // Unlinked players cannot use any commands that send DMs, so discord name must not be null.
        return LBRACK_COMPONENT.copy()
            .append(Sender.discordName!!)
            .append(ARROW_COMPONENT)
            .append(ME_COMPONENT)
            .append(RBRACK_COMPONENT)
            .append(Message)
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
    fun HandleCommand(Handler: ServerPlayNetworkHandler, Command: String) {
        if (!ValidateIncomingMessage(Command, Handler.player)) return

        // An unlinked player can only run /discord link.
        val SP = Handler.player
        if (!SP.isLinkedOrOperator && !Command.startsWith("discord")) {
            Handler.sendPacket(GameMessageS2CPacket(ERR_NEEDS_LINK_TO_CHAT, false))
            return
        }

        // Log the command to the console.
        LOGGER.info("Player {} ran command: /{}", SP.displayName!!.string, Command)

        // Dew it.
        val S = SP.server
        S.execute {
            val ParsedCommand = S.commandManager.dispatcher.parse(Command, SP.commandSource)
            S.commandManager.execute(ParsedCommand, Command)
        }
    }

    /** Send a private message to players. This has already been validated. */
    fun SendPrivateMessage(From: ServerPlayerEntity?, Players: Collection<ServerPlayerEntity>, Message: Text) {
        // Send an incoming message to all players in the list.
        val Msg = FormatPrivateMessage(From, Message)
        for (P in Players) P.networkHandler.sendPacket(GameMessageS2CPacket(Msg, false))

        // And the outgoing message back to the sender. We don’t need to log
        // anything if the console is the sender because the command will have
        // already been logged anyway.
        if (From == null) return
        val OutgoingMsg = LBRACK_COMPONENT.copy()
            .append(ME_COMPONENT)
            .append(ARROW_COMPONENT)

        // Append all players that we’re messaging.
        var First = true
        for (P in Players) {
            if (First) First = false
            else OutgoingMsg.append(COMMA_COMPONENT)
            OutgoingMsg.append(P.displayName)
        }

        // Append the message.
        OutgoingMsg.append(RBRACK_COMPONENT).append(Message)
        From.networkHandler.sendPacket(GameMessageS2CPacket(OutgoingMsg, false))
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