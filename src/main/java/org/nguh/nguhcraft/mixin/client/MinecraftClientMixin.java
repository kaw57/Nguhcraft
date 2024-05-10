package org.nguh.nguhcraft.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow @Nullable public ClientPlayerEntity player;

    @Shadow @Nullable public ClientWorld world;

    /** Prevent breaking blocks in a protected region. */
    @Inject(
        method = "doAttack()Z",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/network/ClientPlayerInteractionManager.attackBlock (Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"
        )
    )
    private void inject$doAttack(CallbackInfoReturnable<Boolean> CI, @Local BlockPos Pos) {
        if (!ProtectionManager.AllowBlockBreak(player, world, Pos)) {
            // Returning true will make the client hallucinate that we did actually
            // break something and stop further processing of this event, without
            // in fact breaking anything.
            player.swingHand(Hand.MAIN_HAND);
            CI.setReturnValue(true);
        }
    }

    /** Prevent using an item while in a hypershot context. */
    @Inject(method = "doItemUse()V", at = @At("HEAD"), cancellable = true)
    private void inject$doItemUse(CallbackInfo CI) {
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
