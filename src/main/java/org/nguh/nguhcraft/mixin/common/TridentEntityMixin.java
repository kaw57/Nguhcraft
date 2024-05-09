package org.nguh.nguhcraft.mixin.common;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.nguh.nguhcraft.TridentEntityAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nguh.nguhcraft.Utils.Debug;
import static org.nguh.nguhcraft.Utils.EnchantLvl;

@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends PersistentProjectileEntity implements TridentEntityAccessor {
    @Shadow @Final private static TrackedData<Byte> LOYALTY;

    protected TridentEntityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Unique boolean Copy = false;

    /** Mark this as a copy. */
    @Override
    public void SetCopy() {
        Copy = true;
        pickupType = PickupPermission.CREATIVE_ONLY;
        dataTracker.set(LOYALTY, (byte) 0);
        setOwner(null);
    }

    /** Unconditionally strike lightning. */
    @Environment(EnvType.SERVER)
    @Unique void StrikeLighting(ServerWorld W, BlockPos Where) {
        var Lightning = EntityType.LIGHTNING_BOLT.create(W);
        if (Lightning != null) {
            Lightning.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(Where));
            Lightning.setChanneler(getOwner() instanceof ServerPlayerEntity SP ? SP : null);
            W.spawnEntity(Lightning);
        }
    }

    /** Disable pickup in the constructor. */
    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;)V", at = @At("TAIL"))
    private void inject$init$0(World W, LivingEntity O, ItemStack S, CallbackInfo CI) {
        this.pickupType = PickupPermission.CREATIVE_ONLY;
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
            EHR.getEntity().timeUntilRegen = 0;
            BlockPos Where = EHR.getEntity().getBlockPos();
            if (Lvl >= 2 || W.isSkyVisible(Where)) {
                StrikeLighting(W, Where);
                SE = SoundEvents.ITEM_TRIDENT_THUNDER;
                Volume = 5.0F;
            }
        }

        this.playSound(SE, Volume, 1.0F);
        CI.cancel();
    }

    /** Discard copied tridents after 5 seconds. */
    @Inject(
        method = "tick()V",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/entity/projectile/TridentEntity.getOwner ()Lnet/minecraft/entity/Entity;",
            ordinal = 0
        )
    )
    private void inject$tick(CallbackInfo CI) {
        if (Copy) {
            if (getWorld() instanceof ServerWorld && inGroundTime > 100) discard();
            super.tick();
            CI.cancel();
        }
    }

    /** Implement Channeling II. */
    @Override
    protected void onBlockHit(BlockHitResult BHR) {
        var Lvl = EnchantLvl(getItemStack(), Enchantments.CHANNELING);
        if (getWorld() instanceof ServerWorld W && Lvl >= 2) {
            StrikeLighting(W, BHR.getBlockPos());
            this.playSound(SoundEvents.ITEM_TRIDENT_THUNDER, 5.F, 1.0F);
        }
    }
}
