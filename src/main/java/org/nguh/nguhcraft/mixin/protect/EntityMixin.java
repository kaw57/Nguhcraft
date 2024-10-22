package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract boolean isOnFire();
    @Shadow public abstract void extinguish();

    @Unique private Entity This() { return (Entity)(Object)this; }

    /** Prevent damage to protected entities. */
    @Inject(method = "isAlwaysInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void inject$isAlwaysInvulnerableTo(DamageSource DS, CallbackInfoReturnable<Boolean> CIR) {
        if (ProtectionManager.IsProtectedEntity(This(), DS))
            CIR.setReturnValue(true);
    }

    /** Clear fire ticks if we’re in a protected region. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void inject$tick(CallbackInfo CI) {
        if (isOnFire() && ProtectionManager.IsProtectedEntity(This()))
            extinguish();
    }
}
