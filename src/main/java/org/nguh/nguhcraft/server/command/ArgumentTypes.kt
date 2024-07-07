package org.nguh.nguhcraft.server.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.nguh.nguhcraft.server.Home
import org.nguh.nguhcraft.server.PlayerByName
import org.nguh.nguhcraft.server.WarpManager
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor
import org.nguh.nguhcraft.server.command.Commands.Exn
import java.util.concurrent.CompletableFuture

private fun StringReader.ReadUntilWhitespace(): String {
    val Start = cursor
    while (canRead() && !Character.isWhitespace(peek())) skip()
    return string.substring(Start, cursor)
}

class HomeArgumentType : ArgumentType<String> {
    override fun parse(R: StringReader) = R.ReadUntilWhitespace()
    companion object {
        private val NO_SUCH_HOME = DynamicCommandExceptionType { Text.literal("No such home: $it") }
        private val NO_SUCH_PLAYER = DynamicCommandExceptionType { Text.literal("No such player: $it") }
        private val INVALID_HOME_NAME = Exn("Home names may not contain ':'!")

        /**
        * Helper to handle other players’ homes.
        *
        * This splits the 'RawName' into a player name and a home name,
        * if an explicit player name is given. Otherwise, the original
        * player and the home name are returned.
        *
        * If the player is not an operator, an error is raised if this
        * would access another player’s home.
        */
        fun MapOrThrow(SP: ServerPlayerEntity, RawName: String): Pair<ServerPlayerEntity, String> {
            if (":" !in RawName) return SP to RawName
            if (!SP.hasPermissionLevel(4)) throw INVALID_HOME_NAME.create()
            val (Player, Home) = RawName.split(":", limit = 2)
            val Other = SP.server.PlayerByName(Player) ?: throw NO_SUCH_PLAYER.create(Player)
            return Other to Home
        }

        fun Resolve(Ctx: CommandContext<ServerCommandSource>, ArgName: String): Home {
            val S = Ctx.source
            val (SP, Name) = MapOrThrow(S.playerOrThrow, Ctx.getArgument(ArgName, String::class.java))
            if (Name == Home.BED_HOME) return Home.Bed(SP)
            val Homes = (SP as ServerPlayerAccessor).Homes()
            return Homes.find { it.Name == Name } ?: throw NO_SUCH_HOME.create(Name)
        }

        fun Suggest(
            Ctx: CommandContext<ServerCommandSource>,
            SB: SuggestionsBuilder
        ): CompletableFuture<Suggestions> {
            val S = Ctx.source
            val SP = S.playerOrThrow
            val Homes = (SP as ServerPlayerAccessor).Homes()
            val Names = Homes.map { it.Name }.toMutableList()
            Names.add(Home.BED_HOME)

            // If the player is an operator, and the argument so far contains
            // a colon, also try to search for the homes of another player, and
            // suggest all players otherwise.
            if (SP.hasPermissionLevel(4)) {
                if (":" in SB.remaining) {
                    S.server.PlayerByName(SB.remaining.split(":")[0])?.let {
                        val OtherHomes = (it as ServerPlayerAccessor).Homes()
                        Names.addAll(OtherHomes.map { H -> "${it.name.string}:${H.Name}" })
                    }
                } else {
                    for (P in S.server.playerManager.playerList)
                        Names.add("${P.name.string}:")
                }
            }

            return CommandSource.suggestMatching(Names, SB)
        }

        fun Home() = HomeArgumentType()
    }
}

class WarpArgumentType : ArgumentType<WarpManager.Warp> {
    override fun parse(R: StringReader): WarpManager.Warp {
        val S = R.readUnquotedString()
        WarpManager.Warps[S]?.let { return it }
        throw NO_SUCH_WARP.createWithContext(R, S)
    }

    companion object {
        private val NO_SUCH_WARP = DynamicCommandExceptionType { Text.literal("No such warp: $it") }

        fun Resolve(Ctx: CommandContext<ServerCommandSource>, Name: String): WarpManager.Warp {
            return Ctx.getArgument(Name, WarpManager.Warp::class.java)
        }

        fun Suggest(
            Ctx: CommandContext<ServerCommandSource>,
            SB: SuggestionsBuilder
        ): CompletableFuture<Suggestions> = CommandSource.suggestMatching(WarpManager.Warps.keys, SB)

        fun Warp() = WarpArgumentType()
    }
}