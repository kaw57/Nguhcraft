package org.nguh.nguhcraft.mixin.server;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    /**
    * Disable fire tick in regions.
    * <p>
    * We accomplish this by returning false from the game rule check.
    */
    @WrapOperation(
        method = "scheduledTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$Key;)Z"
        )
    )
    private boolean inject$scheduledTick(
        GameRules GR,
        GameRules.Key<GameRules.BooleanRule> R,
        Operation<Boolean> Op,
        BlockState St,
        ServerWorld SW,
        BlockPos Pos
    ) {
        if (ProtectionManager.IsProtectedBlock(SW, Pos)) return false;
        return Op.call(GR, R);
    }
}
