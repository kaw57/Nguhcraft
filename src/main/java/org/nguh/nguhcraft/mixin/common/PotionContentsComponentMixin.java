package org.nguh.nguhcraft.mixin.common;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import static org.nguh.nguhcraft.Utils.RomanNumeral;

@Mixin(net.minecraft.component.type.PotionContentsComponent.class)
public abstract class PotionContentsComponentMixin {
    @ModifyArg(
        method = "buildTooltip(Ljava/lang/Iterable;Ljava/util/function/Consumer;FF)V",
        index = 1,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/text/Text.translatable (Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/text/MutableText;",
            ordinal = 0
        )
    )
    private static Object[] inject$buildTooltip(
        Object[] Args,
        @Local MutableText T,
        @Local StatusEffectInstance Effect
    ) {
        var A = Effect.getAmplifier();
        return new Object[]{T, A == 0 ? Text.EMPTY : Text.of(RomanNumeral(A + 1))};
    }
}
