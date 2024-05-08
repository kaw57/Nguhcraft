package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.Keyboard;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "processF3", at = @At("HEAD"), cancellable = true)
    private void onProcessF3(int K, CallbackInfoReturnable<Boolean> CIR) {
        if (NguhcraftClient.ProcessF3(K)) CIR.setReturnValue(true);
    }
}
