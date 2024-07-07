package org.nguh.nguhcraft.mixin.protect.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.effect.entity.ReplaceDiskEnchantmentEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ReplaceDiskEnchantmentEffect.class)
public abstract class ReplaceDiskEnchantmentEffectMixin {
    /** Disable replacement effects such as frost walker in protected regions. */
    @WrapOperation(
        method = "apply",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
        )
    )
    private boolean inject$apply(
        ServerWorld Instance,
        BlockPos Pos,
        BlockState St,
        Operation<Boolean> SetBlockState
    ) {
        if (ProtectionManager.IsProtectedBlock(Instance, Pos)) return false;
        return SetBlockState.call(Instance, Pos, St);
    }
}
