package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingBobberEntity.class)
public abstract class FishingBobberEntityMixin {
    @Shadow public abstract @Nullable PlayerEntity getPlayerOwner();

    @Inject(method = "onEntityHit", at = @At("HEAD"), cancellable = true)
    private void inject$onEntityHit(EntityHitResult EHR, CallbackInfo CI) {
        var PE = getPlayerOwner();
        if (PE == null) return;
        if (ProtectionManager.AllowEntityAttack(PE, EHR.getEntity())) CI.cancel();
    }
}
