package org.nguh.nguhcraft.server

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.logging.LogUtils
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.DimensionArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType
import net.minecraft.enchantment.Enchantment
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.command.CommandManager.*
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.Commands.Exn
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.Utils.Normalised
import org.nguh.nguhcraft.accessors.WorldAccessor
import org.nguh.nguhcraft.protect.Region
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
        CommandRegistrationCallback.EVENT.register { D, A, _ ->
            D.register(DiscordCommand())              // /discord
            D.register(EnchantCommand(A))             // /enchant
            val Msg = D.register(MessageCommand())    // /msg
            D.register(RegionCommand())               // /region
            D.register(RuleCommand())                 // /rule
            D.register(SayCommand())                  // /say
            D.register(literal("tell").redirect(Msg)) // /tell
            D.register(literal("w").redirect(Msg))    // /w
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
        private val ERR_LIST_SYNTAX = Text.literal("Syntax: /discord list (all|linked|<regex>)")

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
                    .append(Text.literal(PD.toString()).withColor(Constants.Lavender))
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

        fun ListSyntaxError(S: ServerCommandSource): Int {
            S.sendError(ERR_LIST_SYNTAX)
            return 0
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
            ).append(Text.literal("#${PD.DiscordColour.toString(16)}").withColor(PD.DiscordColour))
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

    object EnchantCommand {
        private val ERR_NO_ITEM = Exn("You must be holding an item to enchant it!")

        fun Enchant(
            S: ServerCommandSource,
            SP: ServerPlayerEntity,
            E: Enchantment,
            Lvl: Int
        ): Int {
            // This *does* work for books. Fabric’s documentation says otherwise,
            // but it’s simply incorrect about that.
            val ItemStack = SP.mainHandStack
            if (ItemStack.isEmpty) throw ERR_NO_ITEM.create()
            ItemStack.addEnchantment(E, Lvl)
            S.sendMessage(
                Text.translatable(
                    "commands.enchant.success.single", *arrayOf<Any>(
                        E.getName(Lvl),
                        SP.displayName!!,
                    )
                )
            )
            return 1
        }
    }

    object RegionCommand {
        fun AddRegion(S: ServerCommandSource, W: World, Name: String, From: BlockPos, To: BlockPos): Int {
            if (From == To) {
                S.sendError(Text.literal("Refusing to create empty region!"))
                return 0
            }

            val R = Region(
                Name,
                FromX = From.x,
                FromZ = From.z,
                ToX = To.x,
                ToZ = To.z
            )

            (W as WorldAccessor).AddRegion(R)
            S.sendMessage(Text.literal("Created region ")
                .append(Text.literal(Name).formatted(Formatting.AQUA))
                .append(" in world ")
                .append(Text.literal(W.registryKey.value.path.toString()).withColor(Constants.Lavender))
                .append(" with bounds [")
                .append(Text.literal("${R.MinX}").formatted(Formatting.GRAY))
                .append(", ")
                .append(Text.literal("${R.MinZ}").formatted(Formatting.GRAY))
                .append("] → [")
                .append(Text.literal("${R.MaxX}").formatted(Formatting.GRAY))
                .append(", ")
                .append(Text.literal("${R.MaxZ}").formatted(Formatting.GRAY))
                .append("]")
                .formatted(Formatting.GREEN)
            )
            return 1
        }

        fun DeleteRegion(S: ServerCommandSource, W: World, Name: String): Int {
            val Regions = (W as WorldAccessor).regions
            val R = Regions.find { it.Name.equals(Name, ignoreCase = true) }
            if (R == null) {
                S.sendError(Text.literal("No region found with name ")
                    .append(Text.literal(Name).formatted(Formatting.AQUA))
                    .append(" in world ")
                    .append(Text.literal(W.dimension.toString()).withColor(Constants.Lavender))
                )
                return 0
            }

            Regions.remove(R)
            S.sendMessage(Text.literal("Deleted region ")
                .append(Text.literal(Name).formatted(Formatting.AQUA))
                .append(" in world ")
                .append(Text.literal(W.dimension.toString()).withColor(Constants.Lavender))
                .formatted(Formatting.GREEN)
            )
            return 1
        }

        fun ListRegions(S: ServerCommandSource, W: World): Int {
            val Regions = (W as WorldAccessor).regions
            if (Regions.isEmpty()) {
                S.sendMessage(Text.literal("No regions defined in world ")
                    .append(Text.literal(W.dimension.toString()).withColor(Constants.Lavender))
                    .formatted(Formatting.YELLOW)
                )
                return 0
            }

            val List = Text.literal("Regions in world ")
                .append(Text.literal(W.dimension.toString()).withColor(Constants.Lavender))
                .append(":")

            for (R in Regions) {
                List.append(Text.literal("\n  - "))
                    .append(Text.literal(R.Name).formatted(Formatting.AQUA))
                    .append(Text.literal(" ["))
                    .append(Text.literal("${R.MinX}").formatted(Formatting.GRAY))
                    .append(", ")
                    .append(Text.literal("${R.MinZ}").formatted(Formatting.GRAY))
                    .append("] → [")
                    .append(Text.literal("${R.MaxX}").formatted(Formatting.GRAY))
                    .append(", ")
                    .append(Text.literal("${R.MaxZ}").formatted(Formatting.GRAY))
                    .append("]")
            }

            S.sendMessage(List.formatted(Formatting.YELLOW))
            return 1
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
            .requires { it.entity is ServerPlayerEntity && !(it.entity as ServerPlayerEntity).isLinked }
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
            .executes { DiscordCommand.ListSyntaxError(it.source) }
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
                (it.entity is ServerPlayerEntity && (it.entity as ServerPlayerEntity).isLinked) ||
                it.hasPermissionLevel(4)
            }
            .executes {
                DiscordCommand.TryUnlink(
                    it.source,
                    it.source.playerOrThrow
                )
            }
        )

    private fun EnchantCommand(A: CommandRegistryAccess): LiteralArgumentBuilder<ServerCommandSource> = literal("enchant")
        .requires { it.hasPermissionLevel(4) }
        .then(
            argument("enchantment", RegistryEntryReferenceArgumentType.registryEntry(A, RegistryKeys.ENCHANTMENT))
                .then(argument("level", IntegerArgumentType.integer())
                    .executes {
                        EnchantCommand.Enchant(
                            it.source,
                            it.source.playerOrThrow,
                            RegistryEntryReferenceArgumentType.getEnchantment(it, "enchantment").value(),
                            IntegerArgumentType.getInteger(it, "level")
                        )
                    }
                )
                .executes {
                    EnchantCommand.Enchant(
                        it.source,
                        it.source.playerOrThrow,
                        RegistryEntryReferenceArgumentType.getEnchantment(it, "enchantment").value(),
                        1
                    )
                }
        )

    private fun MessageCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("msg")
        .then(argument("targets", EntityArgumentType.players())
            .then(argument("message", StringArgumentType.greedyString())
                .executes {
                    val Players = EntityArgumentType.getPlayers(it, "targets")
                    val Message = StringArgumentType.getString(it, "message")
                    Chat.SendPrivateMessage(it.source.player, Players, Message)
                    Players.size
                }
            )
        )

    private fun RegionCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("region")
        .requires { it.hasPermissionLevel(4) }
        .then(literal("list")
            .then(argument("world", DimensionArgumentType.dimension())
                .executes { RegionCommand.ListRegions(it.source, DimensionArgumentType.getDimensionArgument(it, "world")) }
            )
            .executes { RegionCommand.ListRegions(it.source, it.source.world) }
        )
        .then(literal("add")
            .then(argument("name", StringArgumentType.word())
                .then(argument("from", BlockPosArgumentType.blockPos())
                    .then(argument("to", BlockPosArgumentType.blockPos())
                        .executes { RegionCommand.AddRegion(
                            it.source,
                            it.source.world,
                            StringArgumentType.getString(it, "name"),
                            BlockPosArgumentType.getValidBlockPos(it, "from"),
                            BlockPosArgumentType.getValidBlockPos(it, "to"),
                        ) }
                    )
                )
            )
        )
        .then(literal("del")
            .then(argument("name", StringArgumentType.word())
                .then(argument("world", DimensionArgumentType.dimension())
                    .executes { RegionCommand.DeleteRegion(
                        it.source,
                        DimensionArgumentType.getDimensionArgument(it, "world"),
                        StringArgumentType.getString(it, "name"),
                    ) }
                )
            )
        )

    private fun RuleCommand(): LiteralArgumentBuilder<ServerCommandSource> {
        var Command = literal("rule").requires { it.hasPermissionLevel(4) }
        SyncedGameRule.entries.forEach { Rule ->
            Command = Command.then(literal(Rule.Name)
                .then(argument("value", BoolArgumentType.bool())
                    .executes {
                        Rule.Set(BoolArgumentType.getBool(it, "value"))
                        it.source.sendMessage(Text.literal("Set '${Rule.Name}' to ${Rule.IsSet()}"))
                        1
                    }
                )
                .executes {
                    it.source.sendMessage(Text.literal("Rule '${Rule.Name}' is set to ${Rule.IsSet()}"))
                    1
                }
            )
        }
        return Command
    }

    private fun SayCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("say")
        .requires { it.player == null && it.hasPermissionLevel(4) } // Console only.
        .then(argument("message", StringArgumentType.greedyString())
            .executes {
                Chat.SendServerMessage(StringArgumentType.getString(it, "message"))
                1
            }
        )
}