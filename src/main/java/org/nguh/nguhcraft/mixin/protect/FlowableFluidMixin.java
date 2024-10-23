package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowableFluid.class)
public abstract class FlowableFluidMixin {
    /** Prevent fluids from flowing in(to) protected areas. */
    @Inject(
        method = "canFill(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/Fluid;)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$canFill(
        BlockView BV,
        BlockPos Pos,
        BlockState St,
        Fluid Fl,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        // Ignore region check during world generation.
        if (!(BV instanceof World W) || Pos == null) return;
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CIR.setReturnValue(false);
    }
}
