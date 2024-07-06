package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin {
    /** Prevent pistons from moving protected blocks. */
    @Inject(method = "isMovable", at = @At("HEAD"), cancellable = true)
    private static void inject$isMovable(
        BlockState St,
        World W,
        BlockPos Pos,
        Direction D,
        boolean CanBreak,
        Direction PistonDir,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CIR.setReturnValue(false);
    }
}
