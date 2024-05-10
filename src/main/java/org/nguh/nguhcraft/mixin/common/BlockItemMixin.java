package org.nguh.nguhcraft.mixin.common;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    /** Prevent placing a block inside a region. */
    @Inject(
        method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$place(ItemPlacementContext Context, CallbackInfoReturnable<ActionResult> CIR) {
        var Player = Context.getPlayer();
        if (Player == null) return;
        if (!ProtectionManager.AllowBlockModify(Context.getPlayer(), Context.getWorld(), Context.getBlockPos()))
            CIR.setReturnValue(ActionResult.FAIL);
    }
}
