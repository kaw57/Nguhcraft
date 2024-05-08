package org.nguh.nguhcraft.mixin.common;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.NguhcraftPersistentProjectileEntityAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE;
import static org.nguh.nguhcraft.Utils.Debug;

@Mixin(RangedWeaponItem.class)
public abstract class RangedWeaponItemMixin {
    /** At the start of the function, find a target to home in on. */
    @Inject(method = "shootAll", at = @At("HEAD"))
    private void inject$shootAll$0(
        World W,
        LivingEntity Shooter,
        Hand Hand,
        ItemStack Weapon,
        List<ItemStack> Projectiles,
        float Speed,
        float Div,
        boolean Crit,
        @Nullable LivingEntity Tgt,
        CallbackInfo CI,
        @Share("HomingTarget") LocalRef<LivingEntity> HomingTarget
    ) {
        HomingTarget.set(null);

        // Perform a ray cast up to the max distance, starting at the shooter’s
        // position. Passing a 1 for the tick delta yields the actual camera pos
        // etc.
        var VCam = Shooter.getCameraPosVec(1.0F);
        var VRot = Shooter.getRotationVec(1.0F);
        var VEnd = VCam.add(VRot.x * MAX_HOMING_DISTANCE, VRot.y * MAX_HOMING_DISTANCE, VRot.z * MAX_HOMING_DISTANCE);
        var Ray = W.raycast(new RaycastContext(VCam, VEnd, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, Shooter));

        // If we hit something, don’t go further.
        if (Ray.getType() != HitResult.Type.MISS) VEnd = Ray.getPos();

        // Search for an entity to target. Extend the arrow’s bounding box to
        // the block that we’ve hit, or to the max distance if we missed and
        // check for entity collisions.
        var BB = Box.from(VCam).stretch(VEnd.subtract(VCam)).expand(1.0);
        var EHR = ProjectileUtil.raycast(
            Shooter,
            VCam,
            VEnd,
            BB,
            E -> !E.isSpectator() && E.canHit(),
            MathHelper.square(MAX_HOMING_DISTANCE)
        );

        // If we’re aiming at an entity, use it as the target.
        Debug("Raycasting {} -> {}", VCam, VEnd);
        if (EHR != null) {
            Debug("Targeting {}", EHR.getEntity());
            if (EHR.getEntity() instanceof LivingEntity L) HomingTarget.set(L);
        }
    }

    /** Then, when we shoot an arrow, set the target appropriately. */
    @Inject(
        method = "shootAll",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/item/RangedWeaponItem.shoot (Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/projectile/ProjectileEntity;IFFFLnet/minecraft/entity/LivingEntity;)V",
            ordinal = 0
        )
    )
    private void inject$shootAll$1(
            World W,
            LivingEntity Shooter,
            Hand Hand,
            ItemStack Weapon,
            List<ItemStack> Projectiles,
            float Speed,
            float Div,
            boolean Crit,
            @Nullable LivingEntity Tgt,
            CallbackInfo CI,
            @Local ProjectileEntity Proj,
            @Share("HomingTarget") LocalRef<LivingEntity> HomingTarget
    ) { ((NguhcraftPersistentProjectileEntityAccessor)Proj).SetHomingTarget(HomingTarget.get()); }

}
