package org.nguh.nguhcraft.mixin.server.integrated;

import com.mojang.datafixers.DataFixer;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ApiServices;
import net.minecraft.world.level.storage.LevelStorage;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.nguh.nguhcraft.server.SessionSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.Proxy;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin extends MinecraftServer {
    public IntegratedServerMixin(Thread serverThread, LevelStorage.Session session, ResourcePackManager dataPackManager, SaveLoader saveLoader, Proxy proxy, DataFixer dataFixer, ApiServices apiServices, WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory) {
        super(serverThread, session, dataPackManager, saveLoader, proxy, dataFixer, apiServices, worldGenerationProgressListenerFactory);
    }

    /** Stop the discord bot on shutdown. */
    @Inject(method = "shutdown()V", at = @At("HEAD"))
    private void inject$shutdown(CallbackInfo CI) {
        NguhcraftClient.ActOnSessionShutdown((IntegratedServer)(Object)this);
        SessionSetup.ActOnShutdown(this);
    }

    /** Do initialisation that requires the server ot be running. */
    @Inject(method = "setupServer", at = @At("HEAD"))
    private void inject$setupServer(CallbackInfoReturnable<Boolean> CIR) {
        NguhcraftClient.ActOnSessionStart((IntegratedServer)(Object)this);
        SessionSetup.ActOnStart(this);
    }
}
