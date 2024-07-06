package org.nguh.nguhcraft.server

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.DimensionArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.enchantment.Enchantment
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.Heightmap
import net.minecraft.world.World
import net.minecraft.world.chunk.ChunkStatus
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.item.NguhItems
import org.nguh.nguhcraft.network.ClientboundSyncProtectionBypassPacket
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor
import org.nguh.nguhcraft.server.accessors.ServerPlayerDiscordAccessor

object Commands {
    fun Register() {
        CommandRegistrationCallback.EVENT.register { D, A, E ->
            if (E.dedicated) {
                D.register(DiscordCommand())          // /discord
            }

            D.register(BypassCommand())               // /bypass
            D.register(EnchantCommand(A))             // /enchant
            D.register(KeyCommand())                  // /key
            val Msg = D.register(MessageCommand())    // /msg
            D.register(RegionCommand())               // /region
            D.register(RuleCommand())                 // /rule
            D.register(SayCommand())                  // /say
            D.register(literal("tell").redirect(Msg)) // /tell
            D.register(literal("w").redirect(Msg))    // /w
            D.register(WildCommand())                 // /wild
        }
    }

    fun Exn(message: String): SimpleCommandExceptionType {
        return SimpleCommandExceptionType(Text.literal(message))
    }

    // =========================================================================
    //  Command Implementations
    // =========================================================================
    object BypassCommand {
        private val BYPASSING = Text.literal("Now bypassing region protection.").formatted(Formatting.YELLOW)
        private val NOT_BYPASSING = Text.literal("No longer bypassing region protection.").formatted(Formatting.YELLOW)

        fun Toggle(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            val A = SP as ServerPlayerAccessor
            val NewState = !A.bypassesRegionProtection
            A.bypassesRegionProtection = NewState
            ServerPlayNetworking.send(SP, ClientboundSyncProtectionBypassPacket(NewState))
            S.sendMessage(if (NewState) BYPASSING else NOT_BYPASSING)
            return 1
        }
    }

    object EnchantCommand {
        private val ERR_NO_ITEM = Exn("You must be holding an item to enchant it!")

        fun Enchant(
            S: ServerCommandSource,
            SP: ServerPlayerEntity,
            E: RegistryEntry<Enchantment>,
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
                        Enchantment.getName(E, Lvl),
                        SP.displayName!!,
                    )
                )
            )
            return 1
        }
    }

    object KeyCommand {
        val ERR_EMPTY: Text = Text.literal("Key may not be empty!")

        fun Generate(S: ServerCommandSource, SP: ServerPlayerEntity, Key: String): Int {
            if (Key.isEmpty()) {
                S.sendError(ERR_EMPTY)
                return 0
            }

            val St = ItemStack(NguhItems.KEY)
            St.set(DataComponentTypes.LOCK, ContainerLock(Key))
            SP.inventory.insertStack(St)
            SP.currentScreenHandler.sendContentUpdates()
            S.sendMessage(
                Text.literal("Generated key ").formatted(Formatting.YELLOW)
                .append(Text.literal(Key).formatted(Formatting.LIGHT_PURPLE))
            )
            return 1
        }
    }

    object RegionCommand {
        val NOT_IN_ANY_REGION: Text = Text.literal("You are not in any region!")

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

            try {
                ProtectionManager.AddRegionToWorld(W, R)
            } catch (E: IllegalArgumentException) {
                S.sendError(Text.literal("Region with name ")
                    .append(Text.literal(Name).formatted(Formatting.AQUA))
                    .append(" already exists in world ")
                    .append(Text.literal(W.registryKey.value.path.toString()).withColor(Constants.Lavender))
                )
                return 0
            }

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

        fun AppendRegionBounds(MT: MutableText, R:Region): MutableText = MT.append(Text.literal(" ["))
            .append(Text.literal("${R.MinX}").formatted(Formatting.GRAY))
            .append(", ")
            .append(Text.literal("${R.MinZ}").formatted(Formatting.GRAY))
            .append("] → [")
            .append(Text.literal("${R.MaxX}").formatted(Formatting.GRAY))
            .append(", ")
            .append(Text.literal("${R.MaxZ}").formatted(Formatting.GRAY))
            .append("]")

        fun AppendWorldAndRegionName(MT: MutableText, W: World, Name: String): MutableText = MT
            .append(Text.literal(W.registryKey.value.path.toString()).withColor(Constants.Lavender))
            .append("::")
            .append(Text.literal(Name).formatted(Formatting.AQUA))

        fun DeleteRegion(S: ServerCommandSource, W: World, Name: String): Int {
            if (!ProtectionManager.DeleteRegionFromWorld(W, Name)) {
                S.sendError(AppendWorldAndRegionName(Text.literal("No such region: "), W, Name))
                return 0
            }

            S.sendMessage(AppendWorldAndRegionName(Text.literal("Deleted region "), W, Name)
                .formatted(Formatting.GREEN)
            )
            return 1
        }

        fun GetRegionByName(S: ServerCommandSource, W: World, Name: String): Region? {
            val Regions = ProtectionManager.GetRegions(W)
            val R = Regions.find { it.Name == Name }
            if (R == null) S.sendError(AppendWorldAndRegionName(Text.literal("No such region: "), W, Name))
            return R
        }

        fun ListRegions(S: ServerCommandSource, W: World): Int {
            val Regions = ProtectionManager.GetRegions(W)
            if (Regions.isEmpty()) {
                S.sendMessage(Text.literal("No regions defined in world ")
                    .append(Text.literal(W.registryKey.value.path.toString()).withColor(Constants.Lavender))
                    .formatted(Formatting.YELLOW)
                )
                return 0
            }

            val List = Text.literal("Regions in world ")
                .append(Text.literal(W.registryKey.value.path.toString()).withColor(Constants.Lavender))
                .append(":")

            for (R in Regions) {
                List.append(Text.literal("\n  - "))
                    .append(Text.literal(R.Name).formatted(Formatting.AQUA))
                AppendRegionBounds(List, R)
            }

            S.sendMessage(List.formatted(Formatting.YELLOW))
            return 1
        }

        fun PrintRegionInfo(S: ServerCommandSource, W: World, R: Region): Int {
            val Stats = AppendWorldAndRegionName(Text.literal("Region "), W, R.Name)
            AppendRegionBounds(Stats, R)
            Stats.append(R.Stats)
            S.sendMessage(Stats.formatted(Formatting.YELLOW))
            return 1
        }

        fun PrintRegionInfo(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            val W = SP.world
            val Regions = ProtectionManager.GetRegions(W)
            val R = Regions.find { it.Contains(SP.blockPos) }
            if (R == null) {
                S.sendError(NOT_IN_ANY_REGION)
                return 0
            }

            return PrintRegionInfo(S, W, R)
        }

        fun PrintRegionInfo(S: ServerCommandSource, W: World, Name: String): Int {
            val R = GetRegionByName(S, W, Name) ?: return 0
            return PrintRegionInfo(S, W, R)
        }

        fun SetFlag(
            S: ServerCommandSource,
            W: World,
            Name: String,
            Flag: Region.Flags,
            Allow: Boolean
        ): Int {
            val R = GetRegionByName(S, W, Name) ?: return 0
            R.SetFlag(Flag, Allow)
            val Mess = Text.literal("Set region flag ")
                .append(Text.literal(Flag.name.lowercase()).withColor(Constants.Orange))
                .append(" to ")
                .append(
                    if (Allow) Text.literal("allow").formatted(Formatting.GREEN)
                    else Text.literal("deny").formatted(Formatting.RED)
                )
                .append(" for region ")

            AppendWorldAndRegionName(Mess, W, Name)
            S.sendMessage(Mess.formatted(Formatting.YELLOW))
            return 1
        }
    }

    object WildCommand {
        const val MAX_ATTEMPTS = 50
        val TELEPORT_FAILED: Text = Text.literal("Sorry, couldn’t find a suitable teleport location. Please try again.")

        /**
         * Teleport a player to a random position in the world
         *
         * The destination must not be within a region, and it must be within
         * the border.
         *
         * We also don't want to send players into a lava lake or ocean. For
         * this, we need to make sure the block they land on is a solid block.
         *
         * This is achieved by iterating downwards from the top of the world
         * once random x and z coordinates have been computed and choosing
         * different x and z coordinates should no suitable block be found
         * at that location.
         */
        fun RandomTeleport(S: ServerCommandSource, SP: ServerPlayerEntity): Int {
            val SW = SP.world as ServerWorld
            val WB = SW.worldBorder
            val MinY = SW.bottomY
            val Sz = (WB.size * .6).toInt()
            val Dim = SW.dimension
            val Nether = Dim.hasCeiling()
            val Y = SW.logicalHeight

            // We may get really bad rolls, so don’t try for ever.
            for (tries in 0..<MAX_ATTEMPTS) {
                // Pick any position inside the world border and not too close
                // to the edge.
                val X = SP.random.nextInt(Sz) - Sz / 2
                val Z = SP.random.nextInt(Sz) - Sz / 2
                var Pos = BlockPos.Mutable(X, Y, Z)

                // Check that it is not in a region.
                if (!ProtectionManager.IsLegalTeleportTarget(SW, Pos)) continue

                // Check if we can place the player somewhere in this XZ columns.
                //
                // In anything that is not the nether, getTopY() is the fastest way
                // of accomplishing this; however, that won’t create the chunk if it
                // doesn’t already exist, and we also need to do some post-processing
                // in the nether, so grab the chunk first.
                //
                // But first, check if the chunk already exists; if not, we need to
                // generate it; since this is expensive, only allow doing this once.
                val Chunk = SW.getChunk(X, Z, ChunkStatus.SURFACE, false) ?: continue

                // Helpers we’ll need below.
                fun IsAir() = Chunk.getBlockState(Pos).isAir
                fun MoveDownWhile(Cond: () -> Boolean) {
                    while (Pos.y > MinY && Cond()) Pos = Pos.move(Direction.DOWN)
                }

                // Get the top-most solid block. In the nether, continue through
                // the roof, and then keep going again until we no longer hit air;
                // do the last part even if we’re not in the nether since TopY may
                // be in the air.
                Pos.y = SW.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, X, Z)
                if (Nether) MoveDownWhile { !IsAir() } // Scan through ceiling.
                MoveDownWhile(::IsAir) // Scan to ground.

                // Try teleporting if we’re still in bounds and not in a liquid.
                val Up1 = Pos.up()
                val Up2 = Up1.up()
                if (Pos.y > MinY &&
                    Chunk.getBlockState(Pos).isSideSolidFullSquare(SW, Pos, Direction.UP) &&
                    Chunk.getBlockState(Up1).isAir &&
                    Chunk.getBlockState(Up2).isAir
                ) {
                    SP.Teleport(SW, Pos)
                    return 1
                }
            }

            // Couldn’t find a suitable location.
            S.sendError(TELEPORT_FAILED)
            return 0
        }
    }

    // =========================================================================
    //  Command Trees
    // =========================================================================
    private fun BypassCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("bypass")
        .requires { it.isExecutedByPlayer && it.hasPermissionLevel(4) }
        .executes { BypassCommand.Toggle(it.source, it.source.playerOrThrow) }

    @Environment(EnvType.SERVER)
    private fun DiscordCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("discord")
        .then(literal("info")
            .requires { it.hasPermissionLevel(4) }
            .then(argument("player", EntityArgumentType.player())
                .executes {
                    org.nguh.nguhcraft.server.dedicated.DiscordCommand.ShowLinkInfoForPlayer(
                        it.source,
                        EntityArgumentType.getPlayer(it, "player")
                    )
                }
            )
            .executes {
                org.nguh.nguhcraft.server.dedicated.DiscordCommand.ShowLinkInfoForPlayer(
                    it.source,
                    it.source.playerOrThrow
                )
            }
        )
        .then(literal("link")
            .requires { it.entity is ServerPlayerEntity && !(it.entity as ServerPlayerDiscordAccessor).isLinked }
            .then(argument("id", LongArgumentType.longArg())
                .executes {
                    org.nguh.nguhcraft.server.dedicated.DiscordCommand.TryLink(
                        it.source,
                        it.source.playerOrThrow,
                        LongArgumentType.getLong(it, "id")
                    )
                }
            )
        )
        .then(literal("list")
            .requires { it.hasPermissionLevel(4) }
            .then(literal("all").executes { org.nguh.nguhcraft.server.dedicated.DiscordCommand.ListAllOrLinked(it.source, true) })
            .then(literal("linked").executes { org.nguh.nguhcraft.server.dedicated.DiscordCommand.ListAllOrLinked(it.source, false) })
            .then(argument("filter", StringArgumentType.string())
                .executes {
                    org.nguh.nguhcraft.server.dedicated.DiscordCommand.ListPlayers(
                        it.source,
                        StringArgumentType.getString(it, "filter")
                    )
                }
            )
            .executes { org.nguh.nguhcraft.server.dedicated.DiscordCommand.ListSyntaxError(it.source) }
        )
        .then(literal("query")
            .requires { it.hasPermissionLevel(4) }
            .then(argument("param", StringArgumentType.greedyString())
                .executes {
                    org.nguh.nguhcraft.server.dedicated.DiscordCommand.QueryMemberInfo(
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
                    org.nguh.nguhcraft.server.dedicated.DiscordCommand.TryUnlink(
                        it.source,
                        EntityArgumentType.getPlayer(it, "player")
                    )
                }
            )
            .requires {
                (it.entity is ServerPlayerEntity && (it.entity as ServerPlayerDiscordAccessor).isLinked) ||
                it.hasPermissionLevel(4)
            }
            .executes {
                org.nguh.nguhcraft.server.dedicated.DiscordCommand.TryUnlink(
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
                            RegistryEntryReferenceArgumentType.getEnchantment(it, "enchantment"),
                            IntegerArgumentType.getInteger(it, "level")
                        )
                    }
                )
                .executes {
                    EnchantCommand.Enchant(
                        it.source,
                        it.source.playerOrThrow,
                        RegistryEntryReferenceArgumentType.getEnchantment(it, "enchantment"),
                        1
                    )
                }
        )

    private fun KeyCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("key")
        .requires { it.hasPermissionLevel(4) }
        .then(argument("key", StringArgumentType.string())
            .executes { KeyCommand.Generate(
                it.source,
                it.source.playerOrThrow,
                StringArgumentType.getString(it, "key")
            ) }
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

    private fun RegionCommand(): LiteralArgumentBuilder<ServerCommandSource> {
        val RegionFlagsNameNode = argument("name", StringArgumentType.word())
        Region.Flags.entries.forEach { flag ->
            RegionFlagsNameNode.then(literal(flag.name.lowercase())
                .then(literal("allow").executes {
                    RegionCommand.SetFlag(
                        it.source,
                        it.source.world,
                        StringArgumentType.getString(it, "name"),
                        flag,
                        true
                    )
                })
                .then(literal("deny").executes {
                    RegionCommand.SetFlag(
                        it.source,
                        it.source.world,
                        StringArgumentType.getString(it, "name"),
                        flag,
                        false
                    )
                })
            )
        }

        return literal("region")
            .requires { it.hasPermissionLevel(4) }
            .then(literal("list")
                .then(argument("world", DimensionArgumentType.dimension())
                    .executes {
                        RegionCommand.ListRegions(
                            it.source,
                            DimensionArgumentType.getDimensionArgument(it, "world")
                        )
                    }
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
            .then(literal("info")
                .then(argument("name", StringArgumentType.word())
                    .executes { RegionCommand.PrintRegionInfo(
                        it.source,
                        it.source.playerOrThrow.world,
                        StringArgumentType.getString(it, "name")
                    ) }
                )
                .executes { RegionCommand.PrintRegionInfo(it.source, it.source.playerOrThrow) }
            )
            .then(literal("flags").then(RegionFlagsNameNode))
    }

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

    private fun WildCommand(): LiteralArgumentBuilder<ServerCommandSource> = literal("wild")
        .executes { WildCommand.RandomTeleport(it.source, it.source.playerOrThrow) }
}