package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowBlock.class)
public abstract class SnowBlockMixin {
    /** Prevent snow from falling on protected blocks. */
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    private void inject$canPlaceAt(
        BlockState St,
        WorldView WV,
        BlockPos Pos,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        if (WV instanceof World W && ProtectionManager.IsProtectedBlock(W, Pos))
            CIR.setReturnValue(false);
    }

    /** Prevent protected snow from melting. */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void inject$randomTick(
        BlockState St,
        ServerWorld W,
        BlockPos Pos,
        Random Rng,
        CallbackInfo CI
    ) {
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CI.cancel();
    }
}
