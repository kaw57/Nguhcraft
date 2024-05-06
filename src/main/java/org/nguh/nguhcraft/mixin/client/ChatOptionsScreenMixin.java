package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.ChatOptionsScreen;
import net.minecraft.client.gui.screen.option.SimpleOptionsScreen;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatOptionsScreen.class)
public abstract class ChatOptionsScreenMixin extends SimpleOptionsScreen {
    public ChatOptionsScreenMixin(Screen parent, GameOptions gameOptions, Text title, SimpleOption<?>[] options) {
        super(parent, gameOptions, title, options);
    }

    /** Disable the ‘Only Show Secure Chat’ option; it doesn’t do anything anyway. */
    @Override
    protected void init() {
        super.init();
        var W = buttonList.getWidgetFor(MinecraftClient.getInstance().options.getOnlyShowSecureChat());
        if (W != null) W.active = false;
    }
}
