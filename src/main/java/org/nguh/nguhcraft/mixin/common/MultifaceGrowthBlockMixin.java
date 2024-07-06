package org.nguh.nguhcraft.mixin.common;

import net.minecraft.block.BlockState;
import net.minecraft.block.MultifaceGrowthBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultifaceGrowthBlock.class)
public abstract class MultifaceGrowthBlockMixin {
    /** Prevent vine growth in regions. */
    @Inject(method = "canGrowOn", at = @At("HEAD"), cancellable = true)
    private static void inject$canGrowOn(
        BlockView BV,
        Direction Dir,
        BlockPos Pos,
        BlockState St,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        // Ignore regions during world generation.
        if (!(BV instanceof World W)) return;
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CIR.setReturnValue(false);
    }
}
