package org.nguh.nguhcraft.mixin.protect.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EggEntity.class)
public abstract class EggEntityMixin extends ThrownItemEntity {
    public EggEntityMixin(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    /** Prevent thrown eggs from spawning chickens (or someone might decide to be funny). */
    @ModifyExpressionValue(
        method = "onCollision", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I",
            ordinal = 0
        )
    )
    private int inject$onCollision(int original) {
        // Zero here means that we spawn the chicken, so return 1 instead.
        if (original == 0 && ProtectionManager.IsProtectedBlock(getWorld(), getBlockPos())) return 1;
        return original;
    }
}
