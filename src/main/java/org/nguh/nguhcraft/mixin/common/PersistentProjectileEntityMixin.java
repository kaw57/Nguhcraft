package org.nguh.nguhcraft.mixin.common;

import com.mojang.logging.LogUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.NguhcraftPersistentProjectileEntityAccessor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE;
import static org.nguh.nguhcraft.Utils.Debug;

@Mixin(PersistentProjectileEntity.class)
public abstract class PersistentProjectileEntityMixin extends ProjectileEntity implements NguhcraftPersistentProjectileEntityAccessor {
    public PersistentProjectileEntityMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    /** Maximum ticks before we give up. */
    @Unique static private final int MAX_HOMING_TICKS = 60 * 20;

    /** Vertical offset added to the movement vector to avoid collisions. */
    @Unique static private final Vec3d COLLISION_OFFSET = new Vec3d(0, 1.5, 0);

    /** The entity we’re homing in on. */
    @Unique @Nullable public Entity Target;

    /** How long we’ve been following the entity. */
    @Unique private int HomingTicks;

    /** Set the homing target. */
    @Override public void SetHomingTarget(LivingEntity Target) {
        Debug("Targeting {}", Target);
        this.Target = Target;
        this.HomingTicks = 0;
    }

    /** Implement the homing enchantment. */
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void inject$tick(CallbackInfo CI) {
        // Not a homing projectile.
        if (Target == null) return;

        // Stop if we’ve been going for too long or the target is gone.
        if (
            HomingTicks++ >= MAX_HOMING_TICKS ||
            isRemoved() ||
            !Target.isAlive() ||
            Target.isRemoved()
        ) {
            Target = null;
            return;
        }

        // Calculate the distance vector to the target.
        var TPos = Target.getPos();
        var PPos = getPos();
        var DVec = TPos.subtract(PPos).add(0, Target.getHeight() / 2., 0);
        var Dist = DVec.length();
        if (Dist > MAX_HOMING_DISTANCE) {
            Target = null;
            return;
        }

        // If we would collide with an object, jump up.
        if (getWorld().raycast(new RaycastContext(
            PPos,
            TPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            this
        )).isInsideBlock()) DVec.add(COLLISION_OFFSET);

        // Adjust movement vector towards the target.
        setVelocity(DVec.normalize());
    }
}
