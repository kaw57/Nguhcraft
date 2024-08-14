package org.nguh.nguhcraft.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.Style;
import net.minecraft.text.TextVisitFactory;
import org.nguh.nguhcraft.Utils;
import org.nguh.nguhcraft.client.ClientUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.Normalizer;

@Mixin(TextVisitFactory.class)
public abstract class TextVisitFactoryMixin {
    /**
     * Normalise text before rendering so combining characters work.
     */
    @Inject(
        method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
        at = @At("HEAD")
    )
    private static void inject$visitFormatted(
        String T,
        int SI,
        Style SS,
        Style RS,
        CharacterVisitor Vis,
        CallbackInfoReturnable<Boolean> CIR,
        @Local(argsOnly = true) LocalRef<String> Text
    ) {
        Text.set(ClientUtils.RenderText(Text.get()));
    }
}
