package org.nguh.nguhcraft.mixin.common;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique private PlayerEntity This() { return (PlayerEntity) (Object) this; }

    /** Prevent players from attacking certain entities. */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void inject$attack$0(Entity Target, CallbackInfo CI) {
        if (ProtectionManager.IsProtectedEntity(This(), Target))
            CI.cancel();
    }

    /** Fix bug in Sweeping Edge damage calculation. */
    @Redirect(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/enchantment/EnchantmentHelper.getSweepingMultiplier (Lnet/minecraft/entity/LivingEntity;)F",
            ordinal = 0
        )
    )
    private float inject$attack$1(LivingEntity LE) {
        // This may end up multiplied with infinity, so avoid creating a NaN here.
        var Base = EnchantmentHelper.getSweepingMultiplier(LE);
        return Base == 0 ? Float.MIN_VALUE : Base;
    }

    /** Prevent interactions within a region. */
    @Inject(method = "canInteractWithBlockAt", at = @At("HEAD"), cancellable = true)
    private void inject$canInteractWithBlockAt(
        BlockPos Pos,
        double Range,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        if (!ProtectionManager.AllowBlockInteract(This(), getWorld(), Pos))
            CIR.setReturnValue(false);
    }
}
