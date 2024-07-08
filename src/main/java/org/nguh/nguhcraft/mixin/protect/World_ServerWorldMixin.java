package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {World.class, ServerWorld.class})
public abstract class World_ServerWorldMixin {
    /** Blanket fix for a bunch of random stuff (e.g. picking up water w/ a bucket etc.). */
    @Inject(method = "canPlayerModifyAt", at = @At("HEAD"), cancellable = true)
    private void inject$canPlayerModifyAt(
        PlayerEntity PE,
        BlockPos Pos,
        CallbackInfoReturnable<Boolean> CIR
    ) {
        var Res = ProtectionManager.HandleBlockInteract(PE, (World) (Object) this, Pos, PE.getMainHandStack());
        if (Res != ActionResult.SUCCESS)
            CIR.setReturnValue(false);
    }
}
