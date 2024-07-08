package org.nguh.nguhcraft.mixin.server.dedicated;

import com.mojang.datafixers.DataFixer;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.util.ApiServices;
import net.minecraft.world.level.storage.LevelStorage;
import org.nguh.nguhcraft.server.SessionSetup;
import org.nguh.nguhcraft.server.dedicated.Discord;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.Proxy;

@Mixin(MinecraftDedicatedServer.class)
public abstract class DedicatedServerMixin extends MinecraftServer {
    @Shadow @Final private static Logger LOGGER;

    public DedicatedServerMixin(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory) {
        super(serverThread, session, dataPackManager, saveLoader, proxy, dataFixer, apiServices, worldGenerationProgressListenerFactory);
    }

    /** Stop the discord bot on shutdown. */
    @Inject(method = "shutdown()V", at = @At("HEAD"))
    private void inject$shutdown(CallbackInfo CI) {
        SessionSetup.ActOnShutdown(this);
        Discord.Stop();
    }

    /** Do initialisation that requires the server ot be running. */
    @Inject(method = "setupServer", at = @At("HEAD"))
    private void inject$setupServer(CallbackInfoReturnable<Boolean> CIR) {
        try {
            LOGGER.info("Initialising server");
            Discord.Start((MinecraftDedicatedServer)(Object)this);
        } catch (Exception E) {
            E.printStackTrace();
            System.exit(1);
        }

        SessionSetup.ActOnStart(this);
    }

    /**
    * Disable enforcing secure profiles.
    * @author Sirraide
    * @reason We disable chat signing anyway.
    */
    @Overwrite
    public boolean shouldEnforceSecureProfile() { return false; }
}
