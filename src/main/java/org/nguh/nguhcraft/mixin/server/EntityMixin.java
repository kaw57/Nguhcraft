package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public int timeUntilRegen;

    /** Make it so lightning ignores damage cooldown. */
    @Inject(method = "onStruckByLightning", at = @At("HEAD"))
    private void inject$onStruckByLightning(CallbackInfo CI) {
        timeUntilRegen = 0;
    }
}
