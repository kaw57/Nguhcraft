package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import org.nguh.nguhcraft.Utils;
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
        var HM = P.getHungerManager();

        // If the player’s exhaustion is not yet at the point where they
        // would start losing hunger, don’t bother checking anything else.
        if (HM.getExhaustion() < 4F) return;

        // Prevent hunger loss with a probability proportional to the total
        // weighted saturation level. Don’t bother rolling if the total is 0
        // or 8 (= 100%).
        var Total = Utils.CalculateWeightedSaturationEnchantmentValue(P);
        if (Total == 0) return;
        if (
            Total >= Utils.MAX_SATURATION_ENCHANTMENT_VALUE ||
            P.getWorld().random.nextFloat() < Total * .125F
        ) HM.setExhaustion(0.F);
    }
}
