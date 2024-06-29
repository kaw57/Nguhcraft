package org.nguh.nguhcraft.mixin.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** Prevent damage to protected entities. */
    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void inject$isInvulnerableTo(DamageSource DS, CallbackInfoReturnable<Boolean> CIR) {
        if (ProtectionManager.IsProtectedEntity((Entity)(Object)this, DS))
            CIR.setReturnValue(true);
    }
}
