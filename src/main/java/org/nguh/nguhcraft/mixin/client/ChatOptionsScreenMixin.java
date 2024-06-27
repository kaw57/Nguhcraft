package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ChatOptionsScreen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatOptionsScreen.class)
public abstract class ChatOptionsScreenMixin extends GameOptionsScreen {
    public ChatOptionsScreenMixin(Screen parent, GameOptions gameOptions, Text title) {
        super(parent, gameOptions, title);
    }

    /** Disable the ‘Only Show Secure Chat’ option; it doesn’t do anything anyway. */
    @Override
    protected void init() {
        super.init();
        var W = body.getWidgetFor(client.options.getOnlyShowSecureChat());
        if (W != null) W.active = false;
    }
}
