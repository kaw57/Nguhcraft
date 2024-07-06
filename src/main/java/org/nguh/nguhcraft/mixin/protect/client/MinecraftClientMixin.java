package org.nguh.nguhcraft.mixin.protect.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.client.NguhcraftClient;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientWorld world;

    /** Prevent breaking blocks in a protected region. */
    @Inject(
        method = "doAttack()Z",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/network/ClientPlayerInteractionManager.attackBlock (Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z"
        )
    )
    private void inject$doAttack(CallbackInfoReturnable<Boolean> CI, @Local BlockPos Pos) {
        if (!ProtectionManager.AllowBlockModify(player, world, Pos)) {
            // Returning true will make the client hallucinate that we did actually
            // break something and stop further processing of this event, without
            // in fact breaking anything.
            player.swingHand(Hand.MAIN_HAND);
            CI.setReturnValue(true);
        }
    }

    /**
     * Prevent interactions with blocks within regions.
     * <p>
     * Rewrite them to item uses instead.
     */
    @Redirect(
        method = "doItemUse()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(Lnet/minecraft/client/network/ClientPlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"
        )
    )
    private ActionResult inject$doItemUse(
        ClientPlayerInteractionManager Mgr,
        ClientPlayerEntity CPE,
        Hand H,
        BlockHitResult BHR
    ) {
        // Horrible hack: remember this as the last position we interacted
        // with so we can subsequently disable the ‘Take Book’ button when
        // opening a lectern screen.
        //
        // There doesn’t seem to be a good way of doing this properly since
        // the block position of the lectern is never actually sent to the
        // client...
        //
        // This means this value will contain garbage half of the time, but
        // the only thing that matters is that it doesn’t when we actually
        // manage to open a lectern...
        NguhcraftClient.LastInteractedLecternPos = BHR.getBlockPos();
        var Res = ProtectionManager.HandleBlockInteract(
                CPE,
                CPE.clientWorld,
                BHR.getBlockPos(),
                CPE.getStackInHand(H)
        );

        if (!Res.isAccepted()) return Res;
        return Mgr.interactBlock(CPE, H, BHR);
    }
}

