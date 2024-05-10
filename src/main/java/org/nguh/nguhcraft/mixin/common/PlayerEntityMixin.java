package org.nguh.nguhcraft.mixin.common;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    /** Prevent interactions within a region. */
    @SuppressWarnings("UnreachableCode")
    @Inject(method = "canInteractWithBlockAt", at = @At("HEAD"), cancellable = true)
    private void inject$canInteractWithBlockAt(
        BlockPos Pos,
        double Range,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        if (!ProtectionManager.AllowBlockInteract((PlayerEntity) (Object) this, getWorld(), Pos))
            CIR.setReturnValue(false);
    }
}
