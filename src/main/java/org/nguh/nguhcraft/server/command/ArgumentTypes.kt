package org.nguh.nguhcraft.server.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.command.CommandSource
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import org.nguh.nguhcraft.server.WarpManager
import java.util.concurrent.CompletableFuture

class WarpArgumentType : ArgumentType<WarpManager.Warp> {
    private val NO_SUCH_WARP = DynamicCommandExceptionType { Text.literal("No such warp: $it") }

    override fun <S> listSuggestions(
        Ctx: CommandContext<S>,
        SB: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val S = Ctx.source
        if (S !is CommandSource) return Suggestions.empty()
        return CommandSource.suggestMatching(WarpManager.Warps.keys, SB)
    }

    override fun parse(R: StringReader): WarpManager.Warp {
        val S = R.readUnquotedString()
        WarpManager.Warps[S]?.let { return it }
        throw NO_SUCH_WARP.createWithContext(R, S)
    }

    companion object {
        fun resolve(Ctx: CommandContext<ServerCommandSource>, Name: String): WarpManager.Warp {
            return Ctx.getArgument(Name, WarpManager.Warp::class.java)
        }

        fun warp() = WarpArgumentType()
    }
}