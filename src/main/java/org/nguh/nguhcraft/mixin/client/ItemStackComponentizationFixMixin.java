package org.nguh.nguhcraft.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.fix.ItemStackComponentizationFix;
import org.nguh.nguhcraft.PaperDataFixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ItemStackComponentizationFix.class, priority = 1)
public abstract class ItemStackComponentizationFixMixin {
    /** Run lore data fix. */
    @Inject(method = "fixStack", at = @At("HEAD"))
    private static void inject$fixStack(
        ItemStackComponentizationFix.StackData Data,
        Dynamic<?> Dyn,
        CallbackInfo CI
    ) {
        PaperDataFixer.FixSavedLore(Data);
    }

    /** Run enchantments namespace migration fix. */
    @Inject(
        method = "fixEnchantments",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;isEmpty()Z"
        )
    )
    private static void inject$fixEnchantments(
        ItemStackComponentizationFix.StackData Data,
        Dynamic<?> dynamic,
        String nbtKey,
        String componentId,
        boolean hideInTooltip,
        CallbackInfo ci,
        @Local LocalRef<List<Pair<String, Integer>>> Enchantments
    ) {
        Enchantments.set(PaperDataFixer.FixEnchantments(Enchantments.get()));
    }
}
