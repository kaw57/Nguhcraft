package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow @Nullable public ClientPlayerEntity player;

    @Shadow @Nullable public ClientWorld world;

    /** Prevent using an item while in a hypershot context. */
    @Inject(method = "doItemUse()V", at = @At("HEAD"), cancellable = true)
    private void inject$doItemUse$0(CallbackInfo CI) {
        if (NguhcraftClient.InHypershotContext) CI.cancel();
    }

    /**
    * Disable profile keys.
    *
    * @author Sirraide
    * @reason We don’t do any signing, so this is useless.
    */
    @Overwrite
    public ProfileKeys getProfileKeys() { return ProfileKeys.MISSING; }

    /**
    * Also disable telemetry because why not.
    *
    * @author Sirraide
    * @reason No reason to keep this enabled.
    * */
    @Overwrite
    public boolean isTelemetryEnabledByApi() { return false; }
}
