package org.nguh.nguhcraft.mixin.common;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.MinecartMover;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartMixin extends VehicleEntity {
    public AbstractMinecartMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Unique private AbstractMinecartEntity This() { return  (AbstractMinecartEntity) (Object) this; }

    @Inject(method = "moveOnRail", at = @At("HEAD"), cancellable = true)
    private void inject$moveOnRail(ServerWorld SL, CallbackInfo CI) {
        if (getFirstPassenger() instanceof ServerPlayerEntity) {
            MinecartMover.Move(This(), getBlockPos(), getBlockStateAtPos());
            CI.cancel();
        }
    }
}
