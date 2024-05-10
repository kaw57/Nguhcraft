package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.Utils.LBRACK_COMPONENT
import org.nguh.nguhcraft.Utils.RBRACK_COMPONENT
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.client.accessors.ClientPlayerListEntryAccessor
import org.nguh.nguhcraft.packets.ClientboundChatPacket
import org.nguh.nguhcraft.packets.ClientboundLinkUpdatePacket
import org.nguh.nguhcraft.packets.ClientboundSyncGameRulesPacket
import org.nguh.nguhcraft.packets.ClientboundSyncHypershotStatePacket

/** This runs on the network thread. */
@Environment(EnvType.CLIENT)
object NetworkHandler {
    /** Coloured components used in chat messages. */
    private val ARROW_COMPONENT: Text = Text.literal(" → ").withColor(Constants.DeepKoamaru)
    private val ME_COMPONENT = Text.literal("me").withColor(Constants.Lavender)
    private val SPACE_COMPONENT = Text.literal(" ")

    private fun Execute(CB: () -> Unit) = Client().execute(CB)

    /** Incoming chat message. */
    fun HandleChatPacket(Packet: ClientboundChatPacket) {
        // Render content as Markdown.
        var Message = MarkdownParser.Render(Packet.Content).formatted(Formatting.WHITE)
        Message = when (Packet.MessageKind) {
            // Player: Message content
            ClientboundChatPacket.MK_PUBLIC -> Packet.PlayerName.copy()
                .append(SPACE_COMPONENT)
                .append(Message)

            // [Player → me] Message content
            ClientboundChatPacket.MK_INCOMING_DM -> LBRACK_COMPONENT.copy()
                .append(Packet.PlayerName)
                .append(ARROW_COMPONENT)
                .append(ME_COMPONENT)
                .append(RBRACK_COMPONENT)
                .append(SPACE_COMPONENT)
                .append(Message)

            // [me → Player] Message content
            ClientboundChatPacket.MK_OUTGOING_DM -> LBRACK_COMPONENT.copy()
                .append(ME_COMPONENT)
                .append(ARROW_COMPONENT)
                .append(Packet.PlayerName)
                .append(RBRACK_COMPONENT)
                .append(SPACE_COMPONENT)
                .append(Message)

            // Should never happen, but render the message anyway.
            else -> Text.literal("<ERROR: Invalid Message Format>\n").formatted(Formatting.RED)
                .append(Packet.PlayerName)
                .append(SPACE_COMPONENT)
                .append(Message)
        }

        Execute { Client().messageHandler.onGameMessage(Message, false) }
    }

    /** Notification to update a player’s Discord name. */
    fun HandleLinkUpdatePacket(Packet: ClientboundLinkUpdatePacket) {
        val C = Client()
        val NW = C.networkHandler ?: return
        Execute {
            NW.playerList.firstOrNull { it.profile.id == Packet.PlayerId }?.let {
                it as ClientPlayerListEntryAccessor
                it.isLinked = Packet.Linked

                // Player is not linked.
                if (!Packet.Linked) {
                    it.nameAboveHead = Text.literal(Packet.MinecraftName)
                    it.displayName = Text.literal(Packet.MinecraftName).formatted(Formatting.GRAY)
                }

                // If the player is linked, the player list shows the Minecraft name as well.
                else {
                    val DiscordName = Text.literal(Packet.DiscordName).withColor(Packet.DiscordColour)
                    it.nameAboveHead = DiscordName
                    it.displayName = DiscordName.copy()
                        .append(Text.literal(" "))
                        .append(Utils.BracketedLiteralComponent(Packet.MinecraftName, false))
                }
            }
        }
    }

    /** Update the game rules. */
    fun HandleSyncGameRulesPacket(Packet: ClientboundSyncGameRulesPacket) = SyncedGameRule.Update(Packet)

    /** Sync hypershot state. */
    fun HandleSyncHypershotStatePacket(Packet: ClientboundSyncHypershotStatePacket) {
        NguhcraftClient.InHypershotContext = Packet.InContext
    }
}