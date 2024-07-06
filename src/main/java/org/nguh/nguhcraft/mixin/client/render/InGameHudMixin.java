package org.nguh.nguhcraft.mixin.client.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.nguh.nguhcraft.Utils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    /**
    * Don’t render the hunger bar if the saturation enchantment is maxed out.
    * <p>
    * If the player’s total saturation enchantment level is high enough to
    * where they will never lose hunger, and the food level is also full, then
    * there is no point in rendering the hunger bar since it conveys no useful
    * information.
    */
    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void inject$renderFood(DrawContext Ctx, PlayerEntity P, int Top, int Right, CallbackInfo CI) {
        var HM = P.getHungerManager();

        // Always render the food level if the bar isn’t full.
        if (HM.isNotFull()) return;

        // Do not render it if the saturation level is maxed out.
        if (Utils.CalculateWeightedSaturationEnchantmentValue(P) >= Utils.MAX_SATURATION_ENCHANTMENT_VALUE)
            CI.cancel();
    }
}
