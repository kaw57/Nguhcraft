package org.nguh.nguhcraft;

import net.minecraft.text.Text;

public class Utils {
    /**
     * {@link Text Component} containing the text '[Link]'.
     * <p>
     * Used to communicate to players that a message contains a clickable
     * link. Purely cosmetic since the entire message is clickable anyway.
     */
    public static final Text LINK = Text.literal("[").withColor(Colours.Blue)
        .append(Text.literal("Link").withColor(Colours.Green))
        .append(Text.literal("]").withColor(Colours.Blue))
        .append(Text.literal(".").withColor(Colours.Green));
}
