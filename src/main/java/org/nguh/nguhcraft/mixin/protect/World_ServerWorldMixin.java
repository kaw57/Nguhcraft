package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {World.class, ServerWorld.class})
public abstract class World_ServerWorldMixin {
    @Inject(method = "canPlayerModifyAt", at = @At("HEAD"), cancellable = true)
    private void inject$canPlayerModifyAt(
        PlayerEntity PE,
        BlockPos Pos,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        if (!ProtectionManager.AllowBlockModify(PE, (World) (Object) this, Pos))
            CIR.setReturnValue(false);
    }
}
