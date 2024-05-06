package org.nguh.nguhcraft.server

import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.logging.LogUtils
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager.*
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.nguh.nguhcraft.Colours
import org.nguh.nguhcraft.Commands.Exn
import org.nguh.nguhcraft.Utils.Normalised
import org.nguh.nguhcraft.toUUID
import org.slf4j.Logger
import java.util.*
import java.util.regex.PatternSyntaxException

@Environment(EnvType.SERVER)
object Commands {
    private val LOGGER: Logger = LogUtils.getLogger()

    private val NOT_LINKED: SimpleCommandExceptionType = Exn("Player is not linked to a Discord account!")
    private val ALREADY_LINKED: SimpleCommandExceptionType = Exn("Player is already linked to a Discord account!")

    fun Register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(DiscordCommand())
        }
    }

    // =========================================================================
    //  Command Implementations
    // =========================================================================
    object DiscordCommand {
        private val LIST_ENTRY = Text.literal("\n  - ")
        private val IS_LINKED_TO = Text.literal(" → ")
        private val LPAREN = Text.literal(" (")
        private val RPAREN = Text.literal(")")
        private val ERR_EMPTY_FILTER = Text.literal("Filter may not be empty!")
        private val ERR_EMPTY_QUERY = Text.literal("Query must not be empty!")

        private fun AddPlayer(List: MutableText, PD: PlayerList.Entry) {
            if (PD.isLinked) {
                List.append(LIST_ENTRY)
                    .append(Text.literal(PD.toString()).formatted(Formatting.AQUA))
                    .append(IS_LINKED_TO)
                    .append(Text.literal(PD.DiscordName).withColor(PD.DiscordColour))
                    .append(LPAREN)
                    .append(Text.literal(PD.DiscordID.toString()).formatted(Formatting.GRAY))
                    .append(RPAREN)
            } else {
                List.append(LIST_ENTRY)
                    .append(Text.literal(PD.toString()).withColor(Colours.Lavender))
            }
        }

        fun ListAllOrLinked(S: ServerCommandSource, All: Boolean): Int {
            val Players = PlayerList.AllPlayers()

            // List all players that are linked.
            val List = Text.literal("Linked players:")
            for (PD in Players) if (All || PD.isLinked) AddPlayer(List, PD)

            // Send the list to the player.
            S.sendMessage(List.formatted(Formatting.YELLOW))
            return 1
        }

        fun ListPlayers(S: ServerCommandSource, Filter: String): Int {
            try {
                if (Filter.isEmpty()) {
                    S.sendError(ERR_EMPTY_FILTER)
                    return 0
                }

                val Pat = Regex(Filter, RegexOption.IGNORE_CASE)

                // Get ALL players, not just online ones.
                val Players = PlayerList.AllPlayers()

                // List all players that match the condition.
                val List = Text.literal("Players: ")
                for (PD in Players) {
                    if (!Pat.containsMatchIn(PD.toString())) continue
                    AddPlayer(List, PD)
                }

                // Send the list to the player.
                S.sendMessage(List.formatted(Formatting.YELLOW))
                return 1
            } catch (E: PatternSyntaxException) {
                S.sendError(Text.literal("Invalid regular expression: '${E.message}'"))
                return 0
            }
        }

        fun QueryMemberInfo(S: ServerCommandSource, Message: String): Int {
            val M = Message.trim()
            val Players = PlayerList.AllPlayers()

            // Message must not be empty.
            if (M.isEmpty()) {
                S.sendError(ERR_EMPTY_QUERY)
                return 0
            }

            // We iterate separately each time to correctly handle the case of e.g. someone
            // setting their name to someone else’s ID, in which case we still return the
            // member whose *ID* matches the query, if there is one.
            val Data = M.toLongOrNull()?.let { ID -> Players.find { it.DiscordID == ID }  }
                ?: M.toUUID()?.let { Players.find(it) }
                ?: Normalised(M).let { Norm -> Players.find { it.NormalisedDiscordName == Norm } }
                ?: Players.find { M.equals(it.MinecraftName, ignoreCase = true) }

            // No player found.
            if (Data == null) {
                S.sendError(Text.literal("No player found for query: $Message"))
                return 0
            }

            // We found a player.
            return ShowLinkInfoForPlayer(S, Data)
        }

        fun ShowLinkInfoForPlayer(S: ServerCommandSource, SP: ServerPlayerEntity)
            = ShowLinkInfoForPlayer(S, PlayerList.Player(SP), SP.discordAvatarURL)

        fun ShowLinkInfoForPlayer(S: ServerCommandSource, PD: PlayerList.Entry, AvatarURL: String? = null): Int {
            if (!PD.isLinked) {
                S.sendMessage(Text.literal("Player '$PD' is not linked to a Discord account."))
                return 1
            }

            val Msg = Text.literal("""
                Player '$PD' is linked to ID ${PD.DiscordID}
                  Discord Name: ${PD.DiscordName}
                  Name Colour:  
                """.trimIndent()
            ).append(Text.literal("#%06X").withColor(PD.DiscordColour))
            if (AvatarURL != null) Msg.append("\n  Avatar URL:   $AvatarURL")
            S.sendMessage(Msg)
            return 1
        }

        @Throws(CommandSyntaxException::class)
        private fun Try(S: ServerCommandSource, R: () -> Int): Int {
            try {
                return R()
            } catch (CE: CommandSyntaxException) {
                throw CE
            } catch (E: Exception) {
                val Message = "Failed to send (un)link request: ${E.message}"
                E.printStackTrace()
                S.sendError(Text.literal("$Message\nPlease report this to the server administrator."))
                return 0
            }
        }

        @Throws(CommandSyntaxException::class)
        fun TryLink(S: ServerCommandSource, SP: ServerPlayerEntity, ID: Long): Int {
            return Try(S) {
                if (SP.isLinked) throw ALREADY_LINKED.create()
                Discord.Link(S, SP, ID)
                1
            }
        }

        @Throws(CommandSyntaxException::class)
        fun TryUnlink(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            return Try(S) {
                if (!SP.isLinked) throw NOT_LINKED.create()
                Discord.Unlink(S, SP)
                1
            }
        }
    }

    // =========================================================================
    //  Command Trees
    // =========================================================================
    private fun DiscordCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("discord")
        .then(literal("info")
            .requires { it.hasPermissionLevel(4) }
            .then(argument("player", EntityArgumentType.player())
                .executes {
                    DiscordCommand.ShowLinkInfoForPlayer(
                        it.source,
                        EntityArgumentType.getPlayer(it, "player")
                    )
                }
            )
            .executes {
                DiscordCommand.ShowLinkInfoForPlayer(
                    it.source,
                    it.source.playerOrThrow
                )
            }
        )
        .then(literal("link")
            .requires { it.entity is ServerPlayerEntity && !(it as ServerPlayerEntity).isLinked }
            .then(argument("id", LongArgumentType.longArg())
                .executes {
                    DiscordCommand.TryLink(
                        it.source,
                        it.source.playerOrThrow,
                        LongArgumentType.getLong(it, "id")
                    )
                }
            )
        )
        .then(literal("list")
            .requires { it.hasPermissionLevel(4) }
            .then(literal("all").executes { DiscordCommand.ListAllOrLinked(it.source, true) })
            .then(literal("linked").executes { DiscordCommand.ListAllOrLinked(it.source, false) })
            .then(argument("filter", StringArgumentType.string())
                .executes {
                    DiscordCommand.ListPlayers(
                        it.source,
                        StringArgumentType.getString(it, "filter")
                    )
                }
            )
        )
        .then(literal("query")
            .requires { it.hasPermissionLevel(4) }
            .then(argument("param", StringArgumentType.greedyString())
                .executes {
                    DiscordCommand.QueryMemberInfo(
                        it.source,
                        StringArgumentType.getString(it, "param")
                    )
                }
            )
        )
        .then(literal("unlink")
            .then(argument("player", EntityArgumentType.player())
                .requires { it.hasPermissionLevel(4) }
                .executes {
                    DiscordCommand.TryUnlink(
                        it.source,
                        EntityArgumentType.getPlayer(it, "player")
                    )
                }
            )
            .requires {
                (it.entity is ServerPlayerEntity && (it as ServerPlayerEntity).isLinked) ||
                it.hasPermissionLevel(4)
            }
            .executes {
                DiscordCommand.TryUnlink(
                    it.source,
                    it.source.playerOrThrow
                )
            }
        )
}