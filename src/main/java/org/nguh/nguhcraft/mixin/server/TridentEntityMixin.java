package org.nguh.nguhcraft.mixin.server;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nguh.nguhcraft.Utils.EnchantLvl;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends PersistentProjectileEntity {
    protected TridentEntityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    /** Implement Channeling II. */
    @Inject(
        method = "onEntityHit(Lnet/minecraft/util/hit/EntityHitResult;)V",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/entity/projectile/TridentEntity.setVelocity (Lnet/minecraft/util/math/Vec3d;)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void inject$onEntityHit(EntityHitResult EHR, CallbackInfo CI) {
        SoundEvent SE = SoundEvents.ITEM_TRIDENT_HIT;
        float Volume = 1.0F;

        // Check if it’s thundering or if we have Channeling II.
        var Thunder = getWorld().isThundering();
        var Lvl = EnchantLvl(getItemStack(), Enchantments.CHANNELING);
        if (getWorld() instanceof ServerWorld W && Lvl > 0 && (Thunder || Lvl >= 2)) {
            BlockPos Where = EHR.getEntity().getBlockPos();
            if (Lvl >= 2 || W.isSkyVisible(Where)) {
                var Lightning = EntityType.LIGHTNING_BOLT.create(W);
                if (Lightning != null) {
                    Lightning.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(Where));
                    Lightning.setChanneler(getOwner() instanceof ServerPlayerEntity SP ? SP : null);
                    W.spawnEntity(Lightning);
                    SE = SoundEvents.ITEM_TRIDENT_THUNDER;
                    Volume = 5.0F;
                }
            }
        }

        this.playSound(SE, Volume, 1.0F);
        CI.cancel();
    }
}
