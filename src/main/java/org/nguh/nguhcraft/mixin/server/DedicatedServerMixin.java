package org.nguh.nguhcraft.mixin.server;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.nguh.nguhcraft.server.Discord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftDedicatedServer.class)
public abstract class DedicatedServerMixin {
    /** Stop the discord bot on shutdown. */
    @Inject(method = "shutdown()V", at = @At("HEAD"))
    private void inject$shutdown(CallbackInfo CI) { Discord.Stop(); }

    /**
    * Disable enforcing secure profiles.
    * @author Sirraide
    * @reason We disable chat signing anyway.
    */
    @Overwrite
    public boolean shouldEnforceSecureProfile() { return false; }
}
