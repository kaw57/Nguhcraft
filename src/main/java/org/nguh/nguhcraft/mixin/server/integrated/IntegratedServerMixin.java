package org.nguh.nguhcraft.mixin.server.integrated;

import net.minecraft.server.integrated.IntegratedServer;
import org.nguh.nguhcraft.server.ServerSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {
    /** Stop the discord bot on shutdown. */
    @Inject(method = "shutdown()V", at = @At("HEAD"))
    private void inject$shutdown(CallbackInfo CI) { ServerSetup.ActOnShutdown(); }

    /** Do initialisation that requires the server ot be running. */
    @Inject(method = "setupServer", at = @At("HEAD"))
    private void inject$setupServer(CallbackInfoReturnable<Boolean> CIR) { ServerSetup.ActOnStart(); }
}
