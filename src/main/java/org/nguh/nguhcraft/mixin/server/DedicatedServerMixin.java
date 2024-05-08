package org.nguh.nguhcraft.mixin.server;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.nguh.nguhcraft.server.NguhcraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftDedicatedServer.class)
public abstract class DedicatedServerMixin {
    /** Stop the discord bot on shutdown. */
    @Inject(method = "shutdown()V", at = @At("HEAD"))
    private void inject$shutdown(CallbackInfo CI) { NguhcraftServer.Shutdown(); }

    /** Do initialisation that requires the server ot be running. */
    @Inject(method = "setupServer", at = @At("HEAD"))
    private void inject$setupServer(CallbackInfoReturnable<Boolean> CIR) { NguhcraftServer.Setup(); }

    /**
    * Disable enforcing secure profiles.
    * @author Sirraide
    * @reason We disable chat signing anyway.
    */
    @Overwrite
    public boolean shouldEnforceSecureProfile() { return false; }
}
