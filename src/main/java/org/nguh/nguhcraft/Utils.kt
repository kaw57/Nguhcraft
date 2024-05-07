package org.nguh.nguhcraft

import net.minecraft.text.Text
import net.minecraft.util.Formatting
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


    /** Coloured '[' and '] ' components (the latter includes a space). */
    val LBRACK_COMPONENT: Text = Text.literal("[").withColor(Colours.DeepKoamaru)
    val RBRACK_COMPONENT: Text = Text.literal("] ").withColor(Colours.DeepKoamaru)
    val RBRACK_COMPONENT_NO_SPACE: Text = Text.literal("]").withColor(Colours.DeepKoamaru)

    /**
     * Get a component enclosed in brackets, optionally followed by a space
     * <p>
     * For example, for an input of "foo", this will return '[foo] ' with
     * appropriate formatting.
     */
    fun BracketedLiteralComponent(Content: String, SpaceAfter: Boolean = true): Text = LBRACK_COMPONENT.copy()
        .append(Text.literal(Content).withColor(Colours.Lavender))
        .append(if (SpaceAfter) RBRACK_COMPONENT else RBRACK_COMPONENT_NO_SPACE)

    /** Compute the name of a (linked) player. */
    fun ComputePlayerName(
        IsLinked: Boolean,
        ScoreboardName: String,
        DiscordName: String,
        DiscordColour: Int
    ): Text = if (!IsLinked) Text.literal(ScoreboardName).formatted(Formatting.GRAY)
              else Text.literal(DiscordName).withColor(DiscordColour)

    /** Normalise a string for fuzzy matching against another string  */
    fun Normalised(S: String) = Normalizer.normalize(S, Normalizer.Form.NFKC).lowercase(Locale.getDefault())
}

/** Parse a string into a UUID, returning null on failure. */
fun String?.toUUID(): UUID? {
    if (this == null) return null
    return try { UUID.fromString(this) }
    catch (E: IllegalArgumentException) { null }
}