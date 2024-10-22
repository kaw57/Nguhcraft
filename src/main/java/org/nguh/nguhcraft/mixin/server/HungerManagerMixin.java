package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.nguh.nguhcraft.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public abstract class HungerManagerMixin {
    @Shadow private float exhaustion;

    /** Implement saturation enchantment */
    @Inject(method = "update", at = @At("HEAD"))
    private void inject$update(ServerPlayerEntity P, CallbackInfo CI) {
        // If the player’s exhaustion is not yet at the point where they
        // would start losing hunger, don’t bother checking anything else.
        if (exhaustion < 4F) return;

        // Prevent hunger loss with a probability proportional to the total
        // weighted saturation level. Don’t bother rolling if the total is 0
        // or 8 (= 100%).
        var Total = Utils.CalculateWeightedSaturationEnchantmentValue(P);
        if (Total == 0) return;
        if (
            Total >= Utils.MAX_SATURATION_ENCHANTMENT_VALUE ||
            P.getWorld().random.nextFloat() < Total * .125F
        ) exhaustion = 0.F;
    }
}
