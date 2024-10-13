package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
* Disable mouse grabbing so we can actually run Minecraft in
* the debugger without it locking up the entire IDE. Do not
* use in production.
*/
@Mixin(Mouse.class)
public abstract class MOUSEPLUGIN {
    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
    private void $$$(CallbackInfo ci) {
        ci.cancel();
    }
}
