package org.nguh.nguhcraft.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void inject$interactItem(PlayerEntity Player, Hand Hand, CallbackInfoReturnable<ActionResult> CIR) {
        if (NguhcraftClient.InHypershotContext) CIR.setReturnValue(ActionResult.PASS);
    }

    /** Prevent block breaking in a region to avoid desync issues. */
    @Inject(method ="updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true)
    private void inject$updateBlockBreakingProgress(BlockPos Pos, Direction Dir, CallbackInfoReturnable<Boolean> CIR) {
        if (!ProtectionManager.AllowBlockModify(client.player, client.world, Pos))
            CIR.setReturnValue(false);
    }
}
