package org.nguh.nguhcraft.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor;
import org.nguh.nguhcraft.NguhcraftPersistentProjectileEntityAccessor;
import org.nguh.nguhcraft.server.HypershotContext;
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static org.nguh.nguhcraft.Utils.EnchantLvl;

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
        if (W.isClient) return;

        // Apply homing.
        if (EnchantLvl(Weapon, NguhcraftEnchantments.HOMING) != 0) {
            W.getProfiler().push("homingArrows");
            HomingTarget.set(ServerUtils.MaybeMakeHomingArrow(W, Shooter));
            W.getProfiler().pop();
        }

        // If we’re not already in a hypershot context, apply hypershot.
        ServerUtils.MaybeEnterHypershotContext(Shooter, Hand, Weapon, Projectiles, Speed, Div, Crit);
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
