package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.block.CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlock {
    /** Prevent the creation of snow and iron golems in protected areas. */
    @Inject(method = "trySpawnEntity", at = @At("HEAD"), cancellable = true)
    private void inject$trySpawnEntity(World W, BlockPos Pos, CallbackInfo CI) {
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CI.cancel();
    }
}
