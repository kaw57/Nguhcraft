package org.nguh.nguhcraft.mixin.server;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.world.World;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TridentItem.class)
public abstract class TridentItemMixin {
    /** Implement multishot for tridents. */
    @Inject(
        method = "onStoppedUsing",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/world/World.playSoundFromEntity (Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void inject$onStoppedUsing$0(
        ItemStack Stack,
        World World,
        LivingEntity User,
        int Ticks,
        CallbackInfo CI
    ) {
        ServerUtils.ActOnTridentThrown(
            World,
            (PlayerEntity) User,
            Stack,
            0
        );
    }
}
