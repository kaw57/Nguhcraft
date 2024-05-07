package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListEntry;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Supplier;

@Mixin(SocialInteractionsPlayerListEntry.class)
public abstract class SocialInteractionsPlayerListEntryMixin {
    @Shadow private ButtonWidget reportButton;

    /** Disable report button. */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void inject$init(
        MinecraftClient client,
        SocialInteractionsScreen parent,
        UUID uuid,
        String name,
        Supplier<SkinTextures> skinTexture,
        boolean reportable,
        CallbackInfo ci
    ) {
        reportButton.active = false;
    }
}
