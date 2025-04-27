package org.nguh.nguhcraft.client

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
    val EMOJI_REPLACEMENTS = mapOf(
        "gold_nguh" to '\uE200',
        "rainbow_nguh" to '\uE201',
        "trangs_nguh" to '\uE202',
        "ngold_nguh" to '\uE203',
        "red_nguh" to '\uE204',
        "red_ng" to '\uE205',
        "red_schwa" to '\uE206',
        "red_amogus" to '\uE207',
        "bigyus" to '\uE208',
        "emoji_1" to '\uE209',
        "emoji_2" to '\uE20A',
        "true_anguish" to '\uE20B',
        "worse_anguish" to '\uE20C',
        "worst_anguish" to '\uE20D',
        "true_joy" to '\uE20E',
        "better_joy" to '\uE20F',
        "hellnaw" to '\uE210',
        "hotspot" to '\uE211',
        "pridespot" to '\uE212',
        "trangspot" to '\uE213',
        "ultrafrenchspot" to '\uE214',
        "belgianspot" to '\uE215',
        "lesbianspot" to '\uE216',
        "coldspot" to '\uE217',
        "toki_returna" to '\uE218',
        "trollkipona" to '\uE219',
        "ough" to '\uE21A',
        "eigh" to '\uE21B',
        "blough" to '\uE21C',
        "lol" to '\uE21D',
        "newXD" to '\uE21E',
        "TCPain" to '\uE21F',
        "EYES" to '\uE220',
        "ooooooo" to '\uE221',
        "OOOOOOO" to '\uE222',
        "ellers_estrogen" to '\uE223',
        "colon3" to '\uE224',
        "kek" to '\uE225',
        "hehH" to '\uE226',
        "ap" to '\uE227',
        "predator" to '\uE228',
        "duolingun" to '\uE229',
        "thonkening" to '\uE22A',
        "antistrut" to '\uE22B',
        "antihmidhat" to '\uE22C',
        "antierhua" to '\uE22D',
        "esperandont" to '\uE22E',
        "ngascended" to '\uE22F',
        "reaksi_gue" to '\uE230',
        "smork" to '\uE231',
        "stroke" to '\uE232',
        "uhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh" to '\uE233',
        "regional_indicator_ash" to '\uE234',
        "regional_indicator_eth" to '\uE235',
        "regional_indicator_thorn" to '\uE236',
        "silly_me" to '\uE237',
        "ramsey" to '\uE238',
        "anti_ramsey" to '\uE239',
        "nb_nguh" to '\uE23A',
        "CENSORED" to '\uE23B',
        "aight" to '\uE23C',
        "curst" to '\uE23D',
        "kjehkjeh" to '\uE23E',
        "whatdidijustread" to '\uE23F',
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