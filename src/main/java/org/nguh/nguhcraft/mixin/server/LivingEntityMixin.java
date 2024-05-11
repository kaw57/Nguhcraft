package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.server.HypershotContext;
import org.nguh.nguhcraft.server.ServerUtils;
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements LivingEntityAccessor {
    @Shadow @Final private static Logger LOGGER;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    /** The current hypershot context, if any. */
    @Unique @Nullable private HypershotContext HSContext = null;

    /** Accessors for the hypershot context. */
    @Override public void setHypershotContext(HypershotContext context) { HSContext = context; }
    @Override @Nullable public HypershotContext getHypershotContext() { return HSContext; }

    @Unique private LivingEntity This() { return (LivingEntity) (Object) this; }

    /** Checks that need to run when an entity is ticked. */
    @Inject(method = "baseTick()V", at = @At("HEAD"))
    private void inject$baseTick(CallbackInfo CI) {
        var Self = This();
        if (Self.getWorld().isClient) return;
        ServerUtils.ActOnLivingEntityBaseTick(Self);
    }

    /** Tick Hypershot. */
    @Inject(method = "tickActiveItemStack()V", at = @At("HEAD"), cancellable = true)
    private void inject$tickActiveItemStack(CallbackInfo CI) {
        if (
            HSContext != null &&
            HSContext.Tick(getWorld(), This()) != HypershotContext.EXPIRED
        ) CI.cancel();
    }
}
