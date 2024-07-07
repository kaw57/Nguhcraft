package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.block.BlockState;
import net.minecraft.block.IceBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin {
    /** Prevent protected ice blocks from melting. */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void inject$randomTick(
        BlockState St,
        ServerWorld SW,
        BlockPos Pos,
        Random Rng,
        CallbackInfo CI
    ) {
        if (ProtectionManager.IsProtectedBlock(SW, Pos))
            CI.cancel();
    }
}
