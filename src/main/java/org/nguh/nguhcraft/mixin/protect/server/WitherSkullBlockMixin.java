package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.block.WitherSkullBlock;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkullBlock.class)
public abstract class WitherSkullBlockMixin {
    /** Prevent spawning withers in protected areas. */
    @Inject(
        method = "onPlaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/SkullBlockEntity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$onPlaced(World W, BlockPos Pos, SkullBlockEntity BE, CallbackInfo CI) {
        if (ProtectionManager.IsProtectedBlock(W, Pos))
            CI.cancel();
    }
}
