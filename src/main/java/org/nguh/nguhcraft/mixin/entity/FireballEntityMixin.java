package org.nguh.nguhcraft.mixin.entity;

import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireballEntity.class)
public abstract class FireballEntityMixin {
    /** Prevent fireballs from colliding with one another. */
    @Inject(method = "onCollision", at = @At("HEAD"), cancellable = true)
    private void inject$onCollision(HitResult HR, CallbackInfo CI) {
        if (HR.getType() == HitResult.Type.ENTITY) {
            var EHR = (EntityHitResult) HR;
            if (EHR.getEntity() instanceof FireballEntity) CI.cancel();
        }
    }
}