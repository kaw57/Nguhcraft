package org.nguh.nguhcraft.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractPressurePlateBlock.class)
public abstract class AbstractPressurePlateBlockMixin {
    /** Return a redstone signal of 0 if this pressure plate is disabled. */
    @WrapOperation(
        method = "updatePlateState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/AbstractPressurePlateBlock;getRedstoneOutput(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)I"
        )
    )
    private int inject$updatePlateState(
        AbstractPressurePlateBlock Instance,
        World W,
        BlockPos Pos,
        Operation<Integer> Op
    ) {
        return ProtectionManager.IsPressurePlateEnabled(W, Pos)
            ? Op.call(Instance, W, Pos)
            : 0;
    }
}
