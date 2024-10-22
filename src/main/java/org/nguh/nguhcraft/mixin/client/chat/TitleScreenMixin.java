package org.nguh.nguhcraft.mixin.client.chat;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) { super(title); }

    /** Disable the 'Minecraft Realms' button; it wouldn’t work anyway. */
    @Inject(method = "addNormalWidgets", at = @At("RETURN"))
    private void inject$initWidgetsNormal(int y, int spacingY, CallbackInfoReturnable<Integer> cir) {
        ((ButtonWidget)children().getLast()).active = false;
    }
}
