package org.nguh.nguhcraft.mixin.datafixer;

import net.minecraft.datafixer.fix.LockComponentPredicateFix;
import org.nguh.nguhcraft.item.KeyItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LockComponentPredicateFix.class)
public abstract class LockComponentPredicateFixMixin {
    /**
     * Instead of converting the 'lock' string to a predicate that tests
     * for the custom name of an item, rewrite it to instead check for our
     * custom key component.
     */
    @ModifyArg(
        method = "fixLock",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/serialization/Dynamic;set(Ljava/lang/String;Lcom/mojang/serialization/Dynamic;)Lcom/mojang/serialization/Dynamic;",
            remap = false, // Mojang’s serialisation library is separate and not obfuscated.
            ordinal = 0
        ),
        index = 0
    )
    private static String fixLock(String key) {
        return KeyItem.COMPONENT_ID.toString();
    }
}
