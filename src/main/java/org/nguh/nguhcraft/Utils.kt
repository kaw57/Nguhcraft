package org.nguh.nguhcraft

import net.minecraft.text.Text
import java.text.Normalizer
import java.util.*


object Utils {
    /**
     * [Text] containing the text '[Link]'.
     *
     *
     * Used to communicate to players that a message contains a clickable
     * link. Purely cosmetic since the entire message is clickable anyway.
     */
    val LINK: Text = Text.literal("[").withColor(Colours.Blue)
        .append(Text.literal("Link").withColor(Colours.Green))
        .append(Text.literal("]").withColor(Colours.Blue))
        .append(Text.literal(".").withColor(Colours.Green))

    /** Normalise a string for fuzzy matching against another string  */
    fun Normalised(S: String) = Normalizer.normalize(S, Normalizer.Form.NFKC).lowercase(Locale.getDefault())
}

/** Parse a string into a UUID, returning null on failure. */
fun String?.toUUID(): UUID? {
    if (this == null) return null
    return try { UUID.fromString(this) }
    catch (E: IllegalArgumentException) { null }
}