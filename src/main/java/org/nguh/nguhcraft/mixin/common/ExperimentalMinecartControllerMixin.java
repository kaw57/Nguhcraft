package org.nguh.nguhcraft.mixin.common;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.entity.vehicle.MinecartController;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.nguh.nguhcraft.entity.MinecartUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExperimentalMinecartController.class)
public abstract class ExperimentalMinecartControllerMixin extends MinecartController {
    ExperimentalMinecartControllerMixin(AbstractMinecartEntity minecart) { super(minecart); }

    @Unique private static final double POWERED_RAIL_BOOST = 0.2;

    /** Increase powered rail acceleration. */
    @ModifyConstant(method = "accelerateFromPoweredRail", constant = @Constant(doubleValue = 0.06))
    private double inject$accelerateFromPoweredRail(double value) {
        return POWERED_RAIL_BOOST;
    }

    /** Increase initial velocity. */
    @Redirect(
        method = "applyInitialVelocity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/Vec3d;multiply(D)Lnet/minecraft/util/math/Vec3d;"
        )
    )
    private Vec3d inject$applyInitialVelocity(Vec3d Vec, double value) {
        return Vec.multiply(.01);
    }

    /** Disable underwater slowdown on slopes. */
    @Redirect(
        method = "applySlopeVelocity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;isTouchingWater()Z"
        )
    )
    private boolean inject$applySlopeVelocity(AbstractMinecartEntity M) { return false; }

    /** Accelerate on every step. */
    @Redirect(
        method = "calcNewHorizontalVelocity",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/vehicle/ExperimentalMinecartController$MoveIteration;accelerated:Z",
            ordinal = 0
        )
    )
    private boolean inject$calcNewHorizontalVelocity(ExperimentalMinecartController.MoveIteration instance) { return false; }

    /**
    * Stop immediately when we hit an unpowered rail.
    *
    * @reason Complete replacement.
    * @author Sirraide
    */
    @Overwrite
    private Vec3d decelerateFromPoweredRail(Vec3d V, BlockState S) {
        return S.isOf(Blocks.POWERED_RAIL) && !S.get(PoweredRailBlock.POWERED)
            ? Vec3d.ZERO
            : V;
    }

    /** Disable max speed reduction underwater. */
    @Redirect(
        method = "getMaxSpeed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/vehicle/AbstractMinecartEntity;isTouchingWater()Z"
        )
    )
    private boolean inject$getMaxSpeed(AbstractMinecartEntity M) { return false; }

    /**
     * Implement Minecart collisions.
     */
    @Inject(
        method = "handleCollision",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$handleCollision(CallbackInfoReturnable<Boolean> CIR) {
        // Return 'false' in here to prevent the minecart from inverting
        // its movement direction.
        if (MinecartUtils.HandleCollisions(this.minecart))
            CIR.setReturnValue(false);
    }
}
