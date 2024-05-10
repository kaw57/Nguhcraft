package org.nguh.nguhcraft.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.BlockPos;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final MinecraftClient client;

    /** Don’t highlight blocks if we can’t modify them anyway. */
    @Inject(
        method = "shouldRenderBlockOutline()Z",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/world/ClientWorld.getBlockState (Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;",
            ordinal = 0
        )
    )
    private void inject$shouldRenderBlockOutline(CallbackInfoReturnable<Boolean> CI, @Local BlockPos Pos) {
        // FIXME: This should work, but it doesn’t...
        if (!ProtectionManager.AllowBlockBreak(client.player, client.world, Pos))
            CI.setReturnValue(false);
    }
}
