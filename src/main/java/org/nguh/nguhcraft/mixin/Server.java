package org.nguh.nguhcraft.mixin;

import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import org.nguh.nguhcraft.server.Discord;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftDedicatedServer.class)
public abstract class Server {
    @Inject(method = "shutdown()V", at = @At("HEAD"))
    private void onShutdown(CallbackInfo CI) {
        Discord.Stop();
    }
}
