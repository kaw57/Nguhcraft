package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.gui.hud.MessageIndicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(MessageIndicator.class)
public abstract class MessageIndicatorMixin {
    /**
    * Remove the system message indicator.
    *
    * @author Sirraide
    * @reason The message indicator is useless. All messages are now system messages.
    */
    @Overwrite
    public static MessageIndicator system() { return null; }
}
