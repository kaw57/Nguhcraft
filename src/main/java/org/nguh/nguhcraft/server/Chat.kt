package org.nguh.nguhcraft.server

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import com.mojang.logging.LogUtils
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.Context
import net.minecraft.network.message.ChatVisibility
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.StringHelper
import org.nguh.nguhcraft.Colours
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.packets.ClientboundChatPacket
import org.nguh.nguhcraft.server.ServerUtils.Broadcast
import org.nguh.nguhcraft.server.ServerUtils.Multicast

/** This handles everything related to chat and messages */
@Environment(EnvType.SERVER)
object Chat {
    private val LOGGER = LogUtils.getLogger()
    private val ERR_NEEDS_LINK_TO_CHAT: Text = Text.translatable("You must link your account to send messages in chat or run commands (other than /discord link)").formatted(Formatting.RED)

    /** Components used in sender names. */
    private val SERVER_COMPONENT: Text = Utils.BracketedLiteralComponent("Server", false)
    private val SRV_LIT_COMPONENT: Text = Text.literal("Server").withColor(Colours.Lavender)
    private val COLON_COMPONENT: Text = Text.literal(":")
    private val COMMA_COMPONENT = Text.literal(", ").withColor(Colours.DeepKoamaru)

    /** Actually send a message. */
    private fun DispatchMessage(Sender: ServerPlayerEntity?, Message: String) {
        val Name = (
            if (Sender == null) SERVER_COMPONENT
            else Sender.displayName!!.copy()
                .append(COLON_COMPONENT.copy().withColor(Sender.discordColour))
        )

        Broadcast(ClientboundChatPacket(Name, Message, ClientboundChatPacket.MK_PUBLIC))
        Discord.ForwardChatMessage(Sender, Message)
    }

    /** Ensure a player is linked and issue an error if they aren’t. */
    private fun EnsurePlayerIsLinked(Context: Context): Boolean {
        val SP = Context.player()
        if (!SP.isLinked) {
            Context.responseSender().sendPacket(GameMessageS2CPacket(ERR_NEEDS_LINK_TO_CHAT, false))
            return false
        }
        return true
    }

    /** Log a chat message. */
    fun LogChat(SP: ServerPlayerEntity, Message: String, IsCommand: Boolean) {
        LOGGER.info(
            "[CHAT] {}{}{}: {}{}",
            if (SP.isLinked) SP.discordName else SP.nameForScoreboard,
            if (SP.isLinked) " [${SP.nameForScoreboard}]" else "",
            if (IsCommand) " issued command" else " says",
            if (IsCommand) "/" else "",
            Message
        )
    }

    /** Handle an incoming chat message. */
    fun ProcessChatMessage(Message: String, Context: Context) {
        val SP = Context.player()
        LogChat(SP, Message, false)

        // Unlinked players cannot chat.
        if (!EnsurePlayerIsLinked(Context)) return

        // Dew it.
        DispatchMessage(SP, Message)
    }

    /** Process an incoming command. */
    fun ProcessCommand(Handler: ServerPlayNetworkHandler, Command: String) {
        val SP = Handler.player
        LogChat(SP, Command, true)

        // An unlinked player can only run /discord link.
        if (!SP.isLinkedOrOperator && !Command.startsWith("discord")) {
            Handler.sendPacket(GameMessageS2CPacket(ERR_NEEDS_LINK_TO_CHAT, false))
            return
        }

        // Dew it.
        val S = SP.server
        val ParsedCommand = S.commandManager.dispatcher.parse(Command, SP.commandSource)
        S.commandManager.execute(ParsedCommand, Command)
    }

    /** Send a private message to players. This has already been validated. */
    fun SendPrivateMessage(From: ServerPlayerEntity?, Players: Collection<ServerPlayerEntity>, Message: String) {
        // Send an incoming message to all players in the list.
        val SenderName = if (From == null) SRV_LIT_COMPONENT else From.displayName!!
        Multicast(Players, ClientboundChatPacket(
            SenderName,
            Message,
            ClientboundChatPacket.MK_INCOMING_DM
        ))

        // And the outgoing message back to the sender. We don’t need to log
        // anything if the console is the sender because the command will have
        // already been logged anyway.
        if (From == null) return
        val AllReceivers = Text.empty()
        var First = true
        for (P in Players) {
            if (First) First = false
            else AllReceivers.append(COMMA_COMPONENT)
            AllReceivers.append(P.displayName)
        }

        ServerPlayNetworking.send(From, ClientboundChatPacket(
            AllReceivers,
            Message,
            ClientboundChatPacket.MK_OUTGOING_DM
        ))
    }

    /** Send a message from the console. */
    fun SendServerMessage(Message: String) = DispatchMessage(null, Message)
}