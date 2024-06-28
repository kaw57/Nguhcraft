package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public abstract class HungerManagerMixin {
    /** Implement saturation enchantment */
    @Inject(method = "update", at = @At("HEAD"))
    private void inject$update(PlayerEntity P, CallbackInfo CI) {
        ServerUtils.HandleSaturationEnchantment(P);
    }
}
