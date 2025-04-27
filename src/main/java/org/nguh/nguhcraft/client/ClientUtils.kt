package org.nguh.nguhcraft.client

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.json.Json
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import java.text.Normalizer

inline fun MatrixStack.Push(Transformation: MatrixStack.() -> Unit) {
    push()
    Transformation()
    pop()
}

@Environment(EnvType.CLIENT)
object ClientUtils {
    /** 2000 is reasonable, and we can still send it to Discord this way. */
    const val MAX_CHAT_LENGTH = 2000

    /** Emojis we support. */
    val EMOJI_REPLACEMENTS: Map<String, String> = Gson().fromJson(
        ClientUtils.javaClass.getResource("/assets/nguhcraft/emoji_replacements.json")!!.readText(),
        object : TypeToken<Map<String, String>>() {}.type
    )

    /** Get the client instance. */
    @JvmStatic
    fun Client(): MinecraftClient = MinecraftClient.getInstance()

    /** Preprocess text for rendering. */
    @JvmStatic
    fun RenderText(In: String): String {
        // Normalise to NFC so we can render at least *some*
        // combining characters as precomposed glyphs.
        val Normalised = Normalizer.normalize(In, Normalizer.Form.NFC)

        // Avoid allocating a string if there are no emojis to replace.
        run {
            val First = Normalised.indexOf(':')
            if (First == -1 || Normalised.indexOf(':', First + 1) == -1) return Normalised
        }

        // Replace emojis. Fortunately, they are delimited by ':'s, which
        // means we can just do a lookup rather than having to construct
        // e.g. a trie to find arbitrary substrings.
        return buildString {
            var Pos = 0
            while (true) {
                // Find the start and end of the next emoji.
                var ColonPos = Normalised.indexOf(':', Pos)
                if (ColonPos == -1) break
                val NextColonPos = Normalised.indexOf(':', ColonPos + 1)
                if (NextColonPos == -1) break

                // Try to find the emoji replacement.
                val Emoji = EMOJI_REPLACEMENTS[Normalised.substring(ColonPos + 1, NextColonPos)]

                // If we have an emoji, append it and everything before it.
                if (Emoji != null) {
                    append(Normalised.substring(Pos, ColonPos))
                    append(Emoji)
                }

                // Otherwise, just append the text literally.
                else append(Normalised.substring(Pos, NextColonPos + 1))

                // And keep searching after the second colon.
                Pos = NextColonPos + 1
            }

            // Append any remaining text.
            if (Pos < Normalised.length) append(Normalised.substring(Pos))
        }

/*        // For our custom font, we need to do this manually.
        //
        // The strings passed to the builder here are normalised
        // by it, so we don’t need to worry about that here.
        val Trie = ReplacementTrie.Builder()
            .Add("ụ̄", "\uE59E")
            .Build()

        // Replace all occurrences of the patterns.
        return Trie.Replace(Normalised)*/
    }
}