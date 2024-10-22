package org.nguh.nguhcraft.mixin.common;

import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import org.nguh.nguhcraft.SyncedGameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderEyeItem.class)
public class EnderEyeItemMixin {
    @Unique static private final Text END_DISABLED_MESSAGE
        = Text.literal("You’re not allowed to place this here.").formatted(Formatting.RED);

    /** Disallow placing an eye of ender on an end portal frame. */
    @Inject(
        method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$useOnBlock(ItemUsageContext C, CallbackInfoReturnable<ActionResult> CI) {
        var W = C.getWorld();
        if (!SyncedGameRule.END_ENABLED.IsSet()) {
            if (W.isClient) C.getPlayer().sendMessage(END_DISABLED_MESSAGE, true);
            CI.setReturnValue(ActionResult.FAIL);
        }
    }
}
